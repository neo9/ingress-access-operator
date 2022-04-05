package io.neo9.ingress.access.services;

import java.util.Collection;
import java.util.List;
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

import static io.neo9.ingress.access.config.MutationAnnotations.MUTABLE_INGRESS_VISITOR_GROUP_KEY;
import static io.neo9.ingress.access.config.MutationAnnotations.NGINX_INGRESS_WHITELIST_ANNOTATION_KEY;
import static io.neo9.ingress.access.config.MutationLabels.*;
import static io.neo9.ingress.access.utils.common.KubernetesUtils.getAnnotationValue;
import static io.neo9.ingress.access.utils.common.KubernetesUtils.getResourceNamespaceAndName;
import static io.neo9.ingress.access.utils.common.StringUtils.COMMA;
import static io.neo9.ingress.access.utils.common.StringUtils.EMPTY;
import static java.util.Arrays.stream;

@Service
@Slf4j
public class VisitorGroupIngressReconciler {

	private final VisitorGroupRepository visitorGroupRepository;

	private final IngressRepository ingressRepository;

	private final ServiceRepository serviceRepository;

	private final AdditionalWatchersConfig additionalWatchersConfig;

	public VisitorGroupIngressReconciler(VisitorGroupRepository visitorGroupRepository, IngressRepository ingressRepository, ServiceRepository serviceRepository, AdditionalWatchersConfig additionalWatchersConfig) {
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
					throw new NotHandledWorkloadException(String.format("%s cannot be whitelisted !", hasMetadata.getKind()));
				}
			}
			catch (VisitorGroupNotFoundException e) {
				log.error("panic: could not resolve visitorGroup {} for ingress {}", visitorGroupName, resourceNamespaceAndName);
			}
		}
	}

	public void reconcile(VisitorGroup visitorGroup) {
		String visitorGroupName = visitorGroup.getMetadata().getName();
		log.info("starting reconcile for visitor group {}", visitorGroupName);

		for (String filteringLabel : MUTABLE_FILTERING_LABELS) {
			ingressRepository.listIngressWithLabel(filteringLabel, MUTABLE_LABEL_VALUE)
					.forEach(ingress -> reconcileIfLinkedToVisitorGroup(ingress, visitorGroupName));
			serviceRepository.listWithLabel(filteringLabel, MUTABLE_LABEL_VALUE)
					.forEach(service -> reconcileIfLinkedToVisitorGroup(service, visitorGroupName));
		}

		if (additionalWatchersConfig.watchIngressAnnotations().isEnabled()) {
			for (String filteringLabel : MUTABLE_FILTERING_LABELS) {
				ingressRepository.listIngressWithoutLabel(filteringLabel, MUTABLE_LABEL_VALUE).forEach( // exclude because already retrieved by previous watcher
						ingress -> {
							if (getAnnotationValue(filteringLabel, ingress, "").equalsIgnoreCase(MUTABLE_LABEL_VALUE)) {
								String ingressNamespaceAndName = getResourceNamespaceAndName(ingress);
								log.info("ingress {} have to be marked for update check", ingressNamespaceAndName);
								try {
									reconcile(ingress);
								} catch (VisitorGroupNotFoundException e) {
									log.error("panic: could not resolve visitorGroup {} for ingress {}", visitorGroupName, ingressNamespaceAndName);
								}
							}
						}
				);
			}
		}
	}

	public void reconcile(Ingress ingress) {
		String resourceNamespaceAndName = getResourceNamespaceAndName(ingress);
		log.trace("start patching of {}", resourceNamespaceAndName);

		String cidrListAsString = getCidrListAsString(ingress);
		if (!cidrListAsString.equals(getAnnotationValue(NGINX_INGRESS_WHITELIST_ANNOTATION_KEY, ingress))) {
			log.info("updating ingress {} because the targeted value changed", resourceNamespaceAndName);
			ingressRepository.patchWithAnnotation(ingress, NGINX_INGRESS_WHITELIST_ANNOTATION_KEY, cidrListAsString);
		}

		log.trace("end of patching of {}", resourceNamespaceAndName);
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
		return stream(getAnnotationValue(MUTABLE_INGRESS_VISITOR_GROUP_KEY, hasMetadata, EMPTY).split(COMMA))
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
		if (StringUtils.isEmpty(cidrListAsString)) {
			cidrListAsString = "0.0.0.0/0";
		}

		log.trace("computed cidr list : {}", cidrListAsString);
		return cidrListAsString;
	}

	private boolean isLinkedToVisitorGroupName(HasMetadata hasMetadata, String visitorGroupName) {
		log.debug("checking if {} is concerned by visitorGroupName {}", getResourceNamespaceAndName(hasMetadata), visitorGroupName);
		return stream(getAnnotationValue(MUTABLE_INGRESS_VISITOR_GROUP_KEY, hasMetadata, EMPTY).split(COMMA))
				.anyMatch(s -> s.trim().equalsIgnoreCase(visitorGroupName));
	}

}
