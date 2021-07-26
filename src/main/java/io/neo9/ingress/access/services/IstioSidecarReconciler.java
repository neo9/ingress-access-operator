package io.neo9.ingress.access.services;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.Namespace;
import io.neo9.ingress.access.config.AdditionalWatchersConfig;
import io.neo9.ingress.access.exceptions.ResourceNotManagedByOperatorException;
import io.neo9.ingress.access.repositories.NamespaceRepository;
import io.neo9.ingress.access.repositories.SidecarRepository;
import lombok.extern.slf4j.Slf4j;
import me.snowdrop.istio.api.networking.v1beta1.IstioEgressListenerBuilder;
import me.snowdrop.istio.api.networking.v1beta1.Sidecar;
import me.snowdrop.istio.api.networking.v1beta1.SidecarBuilder;

import org.springframework.stereotype.Service;

import static io.neo9.ingress.access.config.MutationLabels.ISTIO_WATCH_NAMESPACE_LABEL_KEY;
import static io.neo9.ingress.access.config.MutationLabels.ISTIO_WATCH_NAMESPACE_LABEL_VALUE;
import static io.neo9.ingress.access.config.MutationLabels.MANAGED_BY_OPERATOR_KEY;
import static io.neo9.ingress.access.config.MutationLabels.MANAGED_BY_OPERATOR_VALUE;
import static io.neo9.ingress.access.utils.KubernetesUtils.getLabelValue;
import static io.neo9.ingress.access.utils.KubernetesUtils.getResourceNamespaceAndName;
import static java.util.Objects.nonNull;

@Service
@Slf4j
public class IstioSidecarReconciler {

	private final String SIDECAR_NAME = "ingress";

	private final AdditionalWatchersConfig additionalWatchersConfig;

	private final NamespaceRepository namespaceRepository;

	private final SidecarRepository sidecarRepository;

	public IstioSidecarReconciler(NamespaceRepository namespaceRepository, AdditionalWatchersConfig additionalWatchersConfig, SidecarRepository sidecarRepository) {
		this.namespaceRepository = namespaceRepository;
		this.additionalWatchersConfig = additionalWatchersConfig;
		this.sidecarRepository = sidecarRepository;
	}

	public void reconcile(Namespace updatedOrDeletedNamespace) {
		Sidecar oldSidecar = sidecarRepository.getSidecar(additionalWatchersConfig.updateIstioIngressSidecar().getIngressNamespace(), SIDECAR_NAME);
		if (nonNull(oldSidecar) && !getLabelValue(MANAGED_BY_OPERATOR_KEY, oldSidecar).equals(MANAGED_BY_OPERATOR_VALUE)) {
			throw new ResourceNotManagedByOperatorException(getResourceNamespaceAndName(oldSidecar));
		}

		List<Namespace> namespacesWatchedByIstio = namespaceRepository.listNamespacesWithLabel(ISTIO_WATCH_NAMESPACE_LABEL_KEY, ISTIO_WATCH_NAMESPACE_LABEL_VALUE);
		List<String> namespaceForSidecar = Stream.concat(
				additionalWatchersConfig.updateIstioIngressSidecar().getAdditionalEgressRulesEntries().stream(),
				namespacesWatchedByIstio.stream().map(namespace -> namespace.getMetadata().getName())
		)
				.map(s -> String.format("%s/*", s))
				.distinct()
				.collect(Collectors.toList());

		log.trace("computed namespace list : {}", namespaceForSidecar);

		Sidecar sidecar = new SidecarBuilder()
				.withNewMetadata()
				.withNamespace(additionalWatchersConfig.updateIstioIngressSidecar().getIngressNamespace())
				.withName(SIDECAR_NAME)
				.addToLabels(MANAGED_BY_OPERATOR_KEY, MANAGED_BY_OPERATOR_VALUE)
				.endMetadata()
				.withNewSpec()
				.addToEgress(new IstioEgressListenerBuilder().addAllToHosts(namespaceForSidecar).build())
				.endSpec().build();
		sidecarRepository.createOrReplace(sidecar);
	}

}
