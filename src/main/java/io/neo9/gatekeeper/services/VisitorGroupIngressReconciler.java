package io.neo9.gatekeeper.services;

import java.util.Collection;

import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;
import io.neo9.gatekeeper.customresources.VisitorGroup;
import io.neo9.gatekeeper.customresources.spec.V1VisitorGroupSpecSources;
import io.neo9.gatekeeper.exceptions.VisitorGroupNotFoundException;
import io.neo9.gatekeeper.repositories.IngressRepository;
import io.neo9.gatekeeper.repositories.VisitorGroupRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import org.springframework.stereotype.Service;

import static io.neo9.gatekeeper.config.MutationAnnotations.MUTABLE_INGRESS_VISITOR_GROUP_KEY;
import static io.neo9.gatekeeper.config.MutationAnnotations.NGINX_INGRESS_WHITELIST_ANNOTATION_KEY;
import static io.neo9.gatekeeper.config.MutationLabels.MUTABLE_LABEL_KEY;
import static io.neo9.gatekeeper.config.MutationLabels.MUTABLE_LABEL_VALUE;
import static io.neo9.gatekeeper.utils.KubernetesUtils.getAnnotationValue;
import static io.netty.util.internal.StringUtil.EMPTY_STRING;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

@Service
@Slf4j
public class VisitorGroupIngressReconciler {

	private final VisitorGroupRepository visitorGroupRepository;

	private final IngressRepository ingressRepository;

	public VisitorGroupIngressReconciler(VisitorGroupRepository visitorGroupRepository, IngressRepository ingressRepository) {
		this.visitorGroupRepository = visitorGroupRepository;
		this.ingressRepository = ingressRepository;
	}

	public void reconcile(VisitorGroup visitorGroup) {
		String visitorGroupName = visitorGroup.getMetadata().getName();
		log.info("starting reconcile for visitor group {}", visitorGroupName);
		ingressRepository.listIngressWithLabel(MUTABLE_LABEL_KEY, MUTABLE_LABEL_VALUE).forEach(
				ingress -> {
					if (ingressIsLinkedToVisitorGroupName(ingress, visitorGroupName)) {
						String ingressName = ingress.getMetadata().getName();
						log.info("ingress {} have to be marked for update check", ingressName);
						try {
							reconcile(ingress);
						}
						catch (VisitorGroupNotFoundException e) {
							log.error("panic: could not resolve visitorGroup {} for ingress {}", visitorGroupName, ingressName);
						}
					}
				}
		);
	}

	public void reconcile(Ingress ingress) {
		String ingressName = ingress.getMetadata().getName();
		log.trace("start patching ingress {}", ingressName);

		String cidrListAsString = getCidrListAsString(ingress);
		if (!cidrListAsString.equals(getAnnotationValue(NGINX_INGRESS_WHITELIST_ANNOTATION_KEY, ingress))) {
			log.info("updating ingress {} because the targeted value changed", ingress.getMetadata().getName());
			ingressRepository.patchIngressWithAnnotation(ingress, NGINX_INGRESS_WHITELIST_ANNOTATION_KEY, cidrListAsString);
		}

		log.trace("end of patching ingress {}", ingressName);
	}

	public String getCidrListAsString(Ingress ingress) {
		String cidrListAsString = stream(getAnnotationValue(MUTABLE_INGRESS_VISITOR_GROUP_KEY, ingress, EMPTY_STRING).split(","))
				.map(String::trim)
				.filter(StringUtils::isNotBlank)
				.map(visitorGroupRepository::getVisitorGroupByName)
				.map(VisitorGroup::extractSpecSources)
				.flatMap(Collection::stream)
				.map(V1VisitorGroupSpecSources::getCidr)
				.distinct()
				.collect(joining(","));

		// by default, open access
		if (StringUtils.isEmpty(cidrListAsString)) {
			cidrListAsString = "0.0.0.0/0";
		}

		log.trace("computed cidr list : {}", cidrListAsString);
		return cidrListAsString;
	}

	private boolean ingressIsLinkedToVisitorGroupName(Ingress ingress, String visitorGroupName) {
		log.debug("checking if ingress {} is concerned by visitorGroupName {}", ingress.getMetadata().getName(), visitorGroupName);
		return stream(getAnnotationValue(MUTABLE_INGRESS_VISITOR_GROUP_KEY, ingress, EMPTY_STRING).split(","))
				.anyMatch(s -> s.trim().equalsIgnoreCase(visitorGroupName));
	}
}
