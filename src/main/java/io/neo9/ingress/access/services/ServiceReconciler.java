package io.neo9.ingress.access.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.networking.v1beta1.HTTPIngressPathBuilder;
import io.fabric8.kubernetes.api.model.networking.v1beta1.HTTPIngressRuleValueBuilder;
import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressBackendBuilder;
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressBuilder;
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressFluent.SpecNested;
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressRuleBuilder;
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressTLSBuilder;
import io.neo9.ingress.access.config.AdditionalWatchersConfig;
import io.neo9.ingress.access.exceptions.ResourceNotManagedByOperatorException;
import io.neo9.ingress.access.repositories.IngressRepository;
import lombok.extern.slf4j.Slf4j;

import static io.neo9.ingress.access.config.MutationAnnotations.EXPOSE_INGRESS_ADDITIONAL_ANNOTATIONS;
import static io.neo9.ingress.access.config.MutationAnnotations.EXPOSE_INGRESS_ADDITIONAL_LABELS;
import static io.neo9.ingress.access.config.MutationAnnotations.EXPOSE_INGRESS_HOSTNAME;
import static io.neo9.ingress.access.config.MutationLabels.MANAGED_BY_OPERATOR_KEY;
import static io.neo9.ingress.access.config.MutationLabels.MANAGED_BY_OPERATOR_VALUE;
import static io.neo9.ingress.access.utils.KubernetesUtils.getAnnotationValue;
import static io.neo9.ingress.access.utils.KubernetesUtils.getResourceNamespaceAndName;
import static io.neo9.ingress.access.utils.KubernetesUtils.isManagedByOperator;
import static io.neo9.ingress.access.utils.StringUtils.rawBlockToMap;
import static java.util.Objects.nonNull;

@org.springframework.stereotype.Service
@Slf4j
public class ServiceReconciler {


	private final IngressRepository ingressRepository;

	private final AdditionalWatchersConfig additionalWatchersConfig;

	public ServiceReconciler(IngressRepository ingressRepository, AdditionalWatchersConfig additionalWatchersConfig) {
		this.ingressRepository = ingressRepository;
		this.additionalWatchersConfig = additionalWatchersConfig;
	}

	public void reconcile(Service service) {
		String serviceNamespaceAndName = getResourceNamespaceAndName(service);
		log.trace("start working with service {}", serviceNamespaceAndName);

		Ingress oldIngress = ingressRepository.get(service.getMetadata().getNamespace(), service.getMetadata().getName());
		if (nonNull(oldIngress) && !isManagedByOperator(oldIngress)) {
			throw new ResourceNotManagedByOperatorException(getResourceNamespaceAndName(oldIngress));
		}

		String hostname = generateHostname(service);
		log.debug("generating ingress for hostname : {}", hostname);

		Map<String, String> ingressAnnotations = rawBlockToMap(getAnnotationValue(EXPOSE_INGRESS_ADDITIONAL_ANNOTATIONS, service, ""));
		Map<String, String> ingressLabels = rawBlockToMap(getAnnotationValue(EXPOSE_INGRESS_ADDITIONAL_LABELS, service, ""));

		SpecNested<IngressBuilder> ingressBuilderSpecNested = new IngressBuilder()
				.withNewMetadata()
				.withNamespace(service.getMetadata().getNamespace())
				.withName(service.getMetadata().getName())
				.addToAnnotations(ingressAnnotations)
				.addToLabels(ingressLabels)
				.addToLabels(MANAGED_BY_OPERATOR_KEY, MANAGED_BY_OPERATOR_VALUE)
				.endMetadata()
				.withNewSpec()
				.withRules(
						new IngressRuleBuilder()
								.withHost(hostname)
								.withHttp(new HTTPIngressRuleValueBuilder()
										.withPaths(
												new HTTPIngressPathBuilder()
														.withPath("/")
														.withBackend(
																new IngressBackendBuilder()
																		.withServiceName(service.getMetadata().getName())
																		.withServicePort(new IntOrString(service.getSpec().getPorts().get(0).getPort()))
																		.build()
														).build()
										).build()
								).build()
				);

		if (shouldEnableTls(ingressAnnotations)) {
			ingressBuilderSpecNested = ingressBuilderSpecNested
					.withTls(new IngressTLSBuilder()
							.addToHosts(hostname)
							.withSecretName(String.format("%s-tls", service.getMetadata().getName()))
							.build());
		}

		Ingress ingress = ingressBuilderSpecNested.endSpec().build();

		ingressRepository.createOrReplace(ingress);
		log.trace("end of working with service {}", serviceNamespaceAndName);
	}

	public void reconcileOnDelete(Service service) {
		String serviceNamespaceAndName = getResourceNamespaceAndName(service);
		log.trace("start working with deleted service {}", serviceNamespaceAndName);
		Ingress oldIngress = ingressRepository.get(service.getMetadata().getNamespace(), service.getMetadata().getName());
		if (nonNull(oldIngress) && !isManagedByOperator(oldIngress)) {
			throw new ResourceNotManagedByOperatorException(getResourceNamespaceAndName(oldIngress));
		}
		if (nonNull(oldIngress)) {
			log.info("Deleting ingress {} due to service deletion {}", getResourceNamespaceAndName(oldIngress), serviceNamespaceAndName);
			ingressRepository.delete(oldIngress.getMetadata().getNamespace(), oldIngress.getMetadata().getName());
		}
		log.trace("end of working with deleted service {}", serviceNamespaceAndName);
	}

	public String generateHostname(HasMetadata service) {
		List<UnaryOperator<String>> hostnameReplacements = new ArrayList<>();
		hostnameReplacements.add(s -> s.replaceAll(Pattern.quote("{{name}}"), service.getMetadata().getName()));
		hostnameReplacements.add(s -> s.replaceAll(Pattern.quote("{{namespace}}"), service.getMetadata().getNamespace()));
		hostnameReplacements.add(s -> s.replaceAll(Pattern.quote("{{domain}}"), additionalWatchersConfig.exposer().getDomain()));

		String hostnameTemplate = getAnnotationValue(EXPOSE_INGRESS_HOSTNAME, service, additionalWatchersConfig.exposer().getHostnameTemplate());
		for (UnaryOperator<String> fn : hostnameReplacements) {
			hostnameTemplate = fn.apply(hostnameTemplate);
		}
		return hostnameTemplate;
	}

	public boolean shouldEnableTls(Map<String, String> ingressAnnotations) {
		for (String annotationKey : ingressAnnotations.keySet()) {
			if (additionalWatchersConfig.exposer().getTlsEnabledDetectionAnnotation().contains(annotationKey)) {
				return true;
			}
		}
		return false;
	}
}
