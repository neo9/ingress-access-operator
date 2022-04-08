package io.neo9.ingress.access.services;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.Namespace;
import io.neo9.ingress.access.config.AdditionalWatchersConfig;
import io.neo9.ingress.access.customresources.external.istio.Sidecar;
import io.neo9.ingress.access.customresources.external.istio.spec.EgressSpec;
import io.neo9.ingress.access.customresources.external.istio.spec.SidecarSpec;
import io.neo9.ingress.access.exceptions.ResourceNotManagedByOperatorException;
import io.neo9.ingress.access.repositories.NamespaceRepository;
import io.neo9.ingress.access.repositories.SidecarRepository;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import static io.neo9.ingress.access.config.MutationLabels.ISTIO_WATCH_NAMESPACE_LABEL_KEY;
import static io.neo9.ingress.access.config.MutationLabels.ISTIO_WATCH_NAMESPACE_LABEL_VALUE;
import static io.neo9.ingress.access.config.MutationLabels.MANAGED_BY_OPERATOR_KEY;
import static io.neo9.ingress.access.config.MutationLabels.MANAGED_BY_OPERATOR_VALUE;
import static io.neo9.ingress.access.utils.common.KubernetesUtils.getResourceNamespaceAndName;
import static io.neo9.ingress.access.utils.common.KubernetesUtils.isManagedByOperator;
import static java.util.Objects.nonNull;

@Service
@Slf4j
public class IstioSidecarReconciler {

	private static final String SIDECAR_NAME = "ingress";

	private final AdditionalWatchersConfig additionalWatchersConfig;

	private final NamespaceRepository namespaceRepository;

	private final SidecarRepository sidecarRepository;

	public IstioSidecarReconciler(NamespaceRepository namespaceRepository,
			AdditionalWatchersConfig additionalWatchersConfig, SidecarRepository sidecarRepository) {
		this.namespaceRepository = namespaceRepository;
		this.additionalWatchersConfig = additionalWatchersConfig;
		this.sidecarRepository = sidecarRepository;
	}

	public void reconcile() {
		Sidecar oldSidecar = sidecarRepository
				.getSidecar(additionalWatchersConfig.updateIstioIngressSidecar().getIngressNamespace(), SIDECAR_NAME);
		if (nonNull(oldSidecar) && !isManagedByOperator(oldSidecar)) {
			throw new ResourceNotManagedByOperatorException(getResourceNamespaceAndName(oldSidecar));
		}

		List<Namespace> namespacesWatchedByIstio = namespaceRepository
				.listNamespacesWithLabel(ISTIO_WATCH_NAMESPACE_LABEL_KEY, ISTIO_WATCH_NAMESPACE_LABEL_VALUE);
		List<String> namespaceForSidecar = Stream
				.concat(additionalWatchersConfig.updateIstioIngressSidecar().getAdditionalEgressRulesEntries().stream(),
						namespacesWatchedByIstio.stream().map(namespace -> namespace.getMetadata().getName()))
				.map(s -> String.format("%s/*", s)).distinct().collect(Collectors.toList());

		log.trace("computed namespace list : {}", namespaceForSidecar);

		Sidecar sidecar = new Sidecar();
		sidecar.getMetadata().setNamespace(additionalWatchersConfig.updateIstioIngressSidecar().getIngressNamespace());
		sidecar.getMetadata().setName(SIDECAR_NAME);
		sidecar.getMetadata().setLabels(Map.of(MANAGED_BY_OPERATOR_KEY, MANAGED_BY_OPERATOR_VALUE));
		EgressSpec egressSpec = new EgressSpec(namespaceForSidecar);
		SidecarSpec sidecarSpec = new SidecarSpec();
		sidecarSpec.setEgress(List.of(egressSpec));
		sidecar.setSpec(sidecarSpec);

		sidecarRepository.createOrReplace(sidecar);
	}

}
