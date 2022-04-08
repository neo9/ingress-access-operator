package io.neo9.ingress.access.services;

import java.util.*;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.neo9.ingress.access.config.AdditionalWatchersConfig;
import io.neo9.ingress.access.customresources.VisitorGroup;
import io.neo9.ingress.access.customresources.spec.V1VisitorGroupSpecSources;
import io.neo9.ingress.access.exceptions.NotHandledWorkloadException;
import io.neo9.ingress.access.exceptions.VisitorGroupNotFoundException;
import io.neo9.ingress.access.repositories.IngressRepository;
import io.neo9.ingress.access.repositories.ServiceRepository;
import io.neo9.ingress.access.repositories.VisitorGroupRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import org.springframework.stereotype.Service;

import static io.neo9.ingress.access.config.MutationAnnotations.*;
import static io.neo9.ingress.access.config.MutationLabels.*;
import static io.neo9.ingress.access.utils.common.KubernetesUtils.*;
import static io.neo9.ingress.access.utils.common.StringUtils.COMMA;
import static io.neo9.ingress.access.utils.common.StringUtils.EMPTY;
import static java.util.Arrays.stream;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;

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
			MUTABLE_FILTERING_LABELS.forEach(
					filteringLabelKey -> ingressRepository.listIngressWithLabel(filteringLabelKey, MUTABLE_LABEL_VALUE)
							.forEach(ingress -> reconcileIfLinkedToVisitorGroup(ingress, visitorGroupName)));
		}

		// reconcile services
		MUTABLE_FILTERING_LABELS
				.forEach(filteringLabelKey -> serviceRepository.listWithLabel(filteringLabelKey, MUTABLE_LABEL_VALUE)
						.forEach(service -> reconcileIfLinkedToVisitorGroup(service, visitorGroupName)));
	}

	public void reconcile(Ingress ingress) {
		String resourceNamespaceAndName = getResourceNamespaceAndName(ingress);
		log.trace("start patching of {}", resourceNamespaceAndName);

		if (!whitelistCanBeUpdated(ingress)) {
			return;
		}

		String cidrListAsString = getCidrListAsString(ingress);
		if (!cidrListAsString.equals(getAnnotationValue(ingress, NGINX_INGRESS_WHITELIST_ANNOTATION_KEY))) {
			log.info("updating ingress {} because the targeted value changed", resourceNamespaceAndName);
			Map<String, String> annotationsToApply = new HashMap<>();
			annotationsToApply.put(NGINX_INGRESS_WHITELIST_ANNOTATION_KEY, cidrListAsString);
			if (!hasLabel(ingress, MUTABLE_LABEL_KEY, MUTABLE_LABEL_VALUE)
					&& !hasLabel(ingress, LEGACY_MUTABLE_LABEL_KEY, MUTABLE_LABEL_VALUE)) {
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

	private boolean whitelistCanBeUpdated(Ingress ingress) {
		if (hasLabel(ingress, MUTABLE_LABEL_KEY, MUTABLE_LABEL_VALUE)
				|| hasLabel(ingress, LEGACY_MUTABLE_LABEL_KEY, MUTABLE_LABEL_VALUE)) {
			return true;
		}
		if (hasAnnotation(ingress, FILTERING_MANAGED_BY_OPERATOR_KEY, MANAGED_BY_OPERATOR_VALUE)) {
			return true;
		}
		return isEmpty(getAnnotationValue(ingress, NGINX_INGRESS_WHITELIST_ANNOTATION_KEY, EMPTY));
	}

	public void reconcile(io.fabric8.kubernetes.api.model.Service service) {
		String resourceNamespaceAndName = getResourceNamespaceAndName(service);
		log.trace("start patching of {}", resourceNamespaceAndName);

		List<String> cidrList = getCidrList(service);
		if (!cidrList.equals(service.getSpec().getLoadBalancerSourceRanges())) {
			log.info("updating service {} because the targeted value changed", resourceNamespaceAndName);
			serviceRepository.patchLoadBalancerSourceRanges(service, cidrList);
		}

		log.trace("end of patching of {}", resourceNamespaceAndName);
	}

	public List<String> getCidrList(HasMetadata hasMetadata) {
		return stream(getVisitorGroupsFromCategoryLabelOrVisitorGroupAnnotation(hasMetadata).split(COMMA))
				.map(String::trim).filter(StringUtils::isNotBlank).map(visitorGroupRepository::getVisitorGroupByName)
				.map(VisitorGroup::extractSpecSources).flatMap(Collection::stream)
				.map(V1VisitorGroupSpecSources::getCidr).distinct().collect(Collectors.toList());
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
