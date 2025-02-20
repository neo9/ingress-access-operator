package io.neo9.ingress.access.services;

import static io.neo9.ingress.access.config.MutationAnnotations.*;
import static io.neo9.ingress.access.config.MutationLabels.MANAGED_BY_OPERATOR_VALUE;
import static io.neo9.ingress.access.config.MutationLabels.MUTABLE_LABEL_KEY;
import static io.neo9.ingress.access.config.MutationLabels.MUTABLE_LABEL_VALUE;
import static io.neo9.ingress.access.config.MutationLabels.VISITOR_GROUP_LABEL_CATEGORY;
import static io.neo9.ingress.access.utils.common.KubernetesUtils.getAnnotationValue;
import static io.neo9.ingress.access.utils.common.KubernetesUtils.getResourceNamespaceAndName;
import static io.neo9.ingress.access.utils.common.KubernetesUtils.hasAnnotation;
import static io.neo9.ingress.access.utils.common.KubernetesUtils.hasLabel;
import static io.neo9.ingress.access.utils.common.StringUtils.COMMA;
import static io.neo9.ingress.access.utils.common.StringUtils.EMPTY;
import static java.util.Arrays.stream;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.neo9.ingress.access.config.AdditionalWatchersConfig;
import io.neo9.ingress.access.customresources.VisitorGroup;
import io.neo9.ingress.access.customresources.spec.V1VisitorGroupSpecSources;
import io.neo9.ingress.access.exceptions.NotHandledIngressClass;
import io.neo9.ingress.access.exceptions.NotHandledWorkloadException;
import io.neo9.ingress.access.exceptions.VisitorGroupNotFoundException;
import io.neo9.ingress.access.repositories.IngressRepository;
import io.neo9.ingress.access.repositories.ServiceRepository;
import io.neo9.ingress.access.repositories.VisitorGroupRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.acm.AcmClient;
import software.amazon.awssdk.services.acm.model.CertificateStatus;
import software.amazon.awssdk.services.acm.model.CertificateSummary;
import software.amazon.awssdk.services.acm.model.ListCertificatesRequest;
import software.amazon.awssdk.services.acm.model.ListCertificatesResponse;

@Service
@Slf4j
public class VisitorGroupIngressReconciler {

	private final static String ALL_CIDR = "0.0.0.0/0";

	private final VisitorGroupRepository visitorGroupRepository;

	private final IngressRepository ingressRepository;

	private final ServiceRepository serviceRepository;

	private final AdditionalWatchersConfig additionalWatchersConfig;

	public VisitorGroupIngressReconciler(VisitorGroupRepository visitorGroupRepository,
			IngressRepository ingressRepository, ServiceRepository serviceRepository,
			AdditionalWatchersConfig additionalWatchersConfig) {
		this.visitorGroupRepository = visitorGroupRepository;
		this.ingressRepository = ingressRepository;
		this.serviceRepository = serviceRepository;
		this.additionalWatchersConfig = additionalWatchersConfig;
	}

	private void reconcileIfLinkedToVisitorGroup(HasMetadata hasMetadata, String visitorGroupName) {
		if (isLinkedToVisitorGroupName(hasMetadata, visitorGroupName)) {
			String resourceNamespaceAndName = getResourceNamespaceAndName(hasMetadata);
			log.info("{} have to be marked for update check", resourceNamespaceAndName);
			try {
				if (hasMetadata.getKind().equals(new Ingress().getKind())) {
					reconcile((Ingress) hasMetadata);
				}
				else if (hasMetadata.getKind().equals(new io.fabric8.kubernetes.api.model.Service().getKind())) {
					reconcile((io.fabric8.kubernetes.api.model.Service) hasMetadata);
				}
				else {
					throw new NotHandledWorkloadException(
							String.format("%s cannot be whitelisted !", hasMetadata.getKind()));
				}
			}
			catch (VisitorGroupNotFoundException e) {
				log.error("panic: could not resolve visitorGroup {} for ingress {}", visitorGroupName,
						resourceNamespaceAndName);
			}
		}
	}

	public void reconcile(VisitorGroup visitorGroup) {
		String visitorGroupName = visitorGroup.getMetadata().getName();
		log.info("starting reconcile for visitor group {}", visitorGroupName);

		// reconcile ingresses
		if (additionalWatchersConfig.defaultFiltering().isEnabled()) {
			ingressRepository.listAllIngress().forEach(ingress -> {
				String ingressNamespaceAndName = getResourceNamespaceAndName(ingress);
				log.info("ingress {} have to be marked for update check", ingressNamespaceAndName);
				try {
					reconcile(ingress);
				}
				catch (VisitorGroupNotFoundException e) {
					log.error("panic: could not resolve visitorGroup {} for ingress {}", visitorGroupName,
							ingressNamespaceAndName);
				}
			});
		}
		else {
			ingressRepository.listIngressWithLabel(MUTABLE_LABEL_KEY, MUTABLE_LABEL_VALUE)
				.forEach(ingress -> reconcileIfLinkedToVisitorGroup(ingress, visitorGroupName));
		}

		// reconcile services
		serviceRepository.listWithLabel(MUTABLE_LABEL_KEY, MUTABLE_LABEL_VALUE)
			.forEach(service -> reconcileIfLinkedToVisitorGroup(service, visitorGroupName));
	}

	public void reconcile(Ingress ingress) {
		String resourceNamespaceAndName = getResourceNamespaceAndName(ingress);
		log.trace("start patching of {}", resourceNamespaceAndName);

		// 1. reconcile ssl certificate
		if (hasAnnotation(ingress, OPERATOR_AWS_ACM_CERT_DOMAIN)) {
			CertificateSummary certificateSummary = getAcmCertificate(ingress);
			String currentValue = getAnnotationValue(ingress, OPERATOR_AWS_ALB_CERT_ARN);
			boolean valueChanged = (certificateSummary != null)
					&& (!certificateSummary.certificateArn().equals(currentValue));
			if (valueChanged) {
				log.info("updating ingress {} with acm certificate ({}) because value changed",
						resourceNamespaceAndName, certificateSummary.certificateArn());
				ingressRepository.patchWithAnnotations(ingress,
						Map.of(OPERATOR_AWS_ALB_CERT_ARN, certificateSummary.certificateArn()));
			}
		}

		// 2. Reconcile WAF

		if (additionalWatchersConfig.awsIngress().isEnabled() && hasAnnotation(ingress, OPERATOR_AWS_WAF_ENABLED)) {
			if (isEmpty(additionalWatchersConfig.awsIngress().getWafArn())) {
				log.warn("wants to enable waf on ingress {}, but no waf ARN were configured. Cannot continue",
						resourceNamespaceAndName);
			}
			else {
				String annotationValue = getAnnotationValue(ingress, OPERATOR_AWS_WAF_ENABLED);
				if (!(annotationValue.equals("true"))) {
					log.warn("disabling waf on ingress {}", resourceNamespaceAndName);
					ingressRepository.removeAnnotation(ingress, OPERATOR_AWS_ALB_WAF_ARN);
				}
				else {
					boolean valueChanged = !additionalWatchersConfig.awsIngress()
						.getWafArn()
						.equals(getAnnotationValue(ingress, OPERATOR_AWS_ALB_WAF_ARN));
					if (valueChanged) {
						log.info("updating ingress {} with waf ({}) because value changed", resourceNamespaceAndName,
								additionalWatchersConfig.awsIngress().getWafArn());
						ingressRepository.patchWithAnnotations(ingress,
								Map.of(OPERATOR_AWS_ALB_WAF_ARN, additionalWatchersConfig.awsIngress().getWafArn()));
					}
				}
			}
		}

		// 3. reconcile whitelist
		if (!whitelistCanBeUpdated(ingress)) {
			return;
		}

		String cidrListAsString = getCidrListAsString(ingress);
		if (!cidrListAsString.equals(getAnnotationValue(ingress, getIngressWhitelistAnnotation(ingress)))) {
			log.info("updating ingress {} because the access whitelist value changed", resourceNamespaceAndName);
			Map<String, String> annotationsToApply = new HashMap<>();
			annotationsToApply.put(getIngressWhitelistAnnotation(ingress), cidrListAsString);
			if (!hasLabel(ingress, MUTABLE_LABEL_KEY, MUTABLE_LABEL_VALUE)) {
				annotationsToApply.put(FILTERING_MANAGED_BY_OPERATOR_KEY, MANAGED_BY_OPERATOR_VALUE);
			}
			if (hasAnnotation(ingress, FORECASTLE_EXPOSE)) {
				annotationsToApply.put(FORECASTLE_NETWORK_RESTRICTED,
						Boolean.toString(!ALL_CIDR.equals(cidrListAsString)));
			}
			ingressRepository.patchWithAnnotations(ingress, annotationsToApply);
		}

		log.trace("end of patching of {}", resourceNamespaceAndName);
	}

	private CertificateSummary getAcmCertificate(Ingress ingress) {
		String resourceNamespaceAndName = getResourceNamespaceAndName(ingress);
		String domainName = getAnnotationValue(ingress, OPERATOR_AWS_ACM_CERT_DOMAIN);

		AcmClient client = AcmClient.create();

		ListCertificatesRequest req = ListCertificatesRequest.builder()
			.certificateStatuses(CertificateStatus.ISSUED)
			.maxItems(1000)
			.build();

		// Retrieve the list of certificates.
		ListCertificatesResponse result = null;
		try {
			result = client.listCertificates(req);
		}
		catch (Exception e) {
			log.error("could not list acm certificates, wont make any change", e);
			return null;
		}
		Optional<CertificateSummary> certificateSummaryOpt = result.certificateSummaryList()
			.stream()
			.filter(c -> c.domainName().equals(domainName))
			.findFirst();

		if (certificateSummaryOpt.isEmpty()) {
			log.warn("did not find acm certificate for ingress {}, domain = {}", resourceNamespaceAndName, domainName);
			return null;
		}

		return certificateSummaryOpt.get();
	}

	private boolean whitelistCanBeUpdated(Ingress ingress) {
		if (hasLabel(ingress, MUTABLE_LABEL_KEY, MUTABLE_LABEL_VALUE)) {
			return true;
		}
		if (hasAnnotation(ingress, FILTERING_MANAGED_BY_OPERATOR_KEY, MANAGED_BY_OPERATOR_VALUE)) {
			return true;
		}
		return isEmpty(getAnnotationValue(ingress, getIngressWhitelistAnnotation(ingress), EMPTY));
	}

	public String getIngressWhitelistAnnotation(Ingress ingress) {
		String ingressClassName = null;
		if (ingress.getSpec() != null) {
			ingressClassName = ingress.getSpec().getIngressClassName();
		}
		if (ingressClassName == null) {
			ingressClassName = ingress.getMetadata().getAnnotations().get("kubernetes.io/ingress.class");
		}
		if (ingressClassName.contains("nginx")) {
			return NGINX_INGRESS_WHITELIST_ANNOTATION_KEY;
		}
		else if (ingressClassName.contains("alb")) {
			return ALB_INGRESS_WHITELIST_ANNOTATION_KEY;
		}
		throw new NotHandledIngressClass(String.format("Ingress class %s is not handled", ingressClassName));
	}

	public void reconcile(io.fabric8.kubernetes.api.model.Service service) {
		String resourceNamespaceAndName = getResourceNamespaceAndName(service);
		log.trace("start patching of {}", resourceNamespaceAndName);

		List<String> cidrList = getCidrList(service);
		if (!cidrList.equals(service.getSpec().getLoadBalancerSourceRanges())) {
			log.info("updating service {} because the access whitelist value changed", resourceNamespaceAndName);
			serviceRepository.patchLoadBalancerSourceRanges(service, cidrList);
		}

		log.trace("end of patching of {}", resourceNamespaceAndName);
	}

	public List<String> getCidrList(HasMetadata hasMetadata) {
		return stream(getVisitorGroupsFromCategoryLabelOrVisitorGroupAnnotation(hasMetadata).split(COMMA))
			.map(String::trim)
			.filter(StringUtils::isNotBlank)
			.map(visitorGroupRepository::getVisitorGroupByName)
			.map(VisitorGroup::extractSpecSources)
			.flatMap(Collection::stream)
			.map(V1VisitorGroupSpecSources::getCidr)
			.distinct()
			.collect(Collectors.toList());
	}

	public String getCidrListAsString(HasMetadata hasMetadata) {
		String cidrListAsString = getCidrList(hasMetadata).stream().collect(Collectors.joining(COMMA));

		// by default, open access
		if (isEmpty(cidrListAsString)) {
			cidrListAsString = ALL_CIDR;
		}

		log.trace("computed cidr list : {}", cidrListAsString);
		return cidrListAsString;
	}

	private boolean isLinkedToVisitorGroupName(HasMetadata hasMetadata, String visitorGroupName) {
		log.debug("checking if {} is concerned by visitorGroupName {}", getResourceNamespaceAndName(hasMetadata),
				visitorGroupName);
		return stream(getVisitorGroupsFromCategoryLabelOrVisitorGroupAnnotation(hasMetadata).split(COMMA))
			.anyMatch(s -> s.trim().equalsIgnoreCase(visitorGroupName));
	}

	private String getVisitorGroupsFromCategoryLabelOrVisitorGroupAnnotation(HasMetadata hasMetadata) {
		// priority to visitor groups
		if (hasAnnotation(hasMetadata, MUTABLE_INGRESS_VISITOR_GROUP_KEY)) {
			return getAnnotationValue(hasMetadata, MUTABLE_INGRESS_VISITOR_GROUP_KEY);
		}

		// then default groups
		List<VisitorGroup> defaultCategories = new ArrayList<>();
		for (String category : additionalWatchersConfig.defaultFiltering().getCategories()) {
			defaultCategories.addAll(visitorGroupRepository.getByLabel(VISITOR_GROUP_LABEL_CATEGORY, category));
		}
		if (!defaultCategories.isEmpty()) {
			return defaultCategories.stream().map(vg -> vg.getMetadata().getName()).collect(Collectors.joining(COMMA));
		}

		// by default, nothing
		return EMPTY;
	}

}
