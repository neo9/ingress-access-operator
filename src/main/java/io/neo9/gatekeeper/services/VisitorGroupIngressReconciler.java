package io.neo9.gatekeeper.services;

import java.util.Collection;

import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;
import io.neo9.gatekeeper.customresources.VisitorGroup;
import io.neo9.gatekeeper.exceptions.SafeTaskInterruptionOnErrorException;
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
import static org.apache.commons.lang3.ObjectUtils.allNotNull;

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
						log.info("ingress {} have to be marked for update", ingress.getMetadata().getName());
						try {
							reconcile(ingress);
						}
						catch (SafeTaskInterruptionOnErrorException e) {
							log.error(e.getMessage());
						}
					}
				}
		);
	}

	public void reconcile(Ingress ingress) throws SafeTaskInterruptionOnErrorException {
		String ingressName = ingress.getMetadata().getName();
		log.trace("start patching ingress {}", ingressName);

		String cidrListAsString = stream(getAnnotationValue(MUTABLE_INGRESS_VISITOR_GROUP_KEY, ingress, EMPTY_STRING).split(","))
				.map(visitorGroupName -> visitorGroupName.trim())
				.filter(visitorGroupName -> !visitorGroupName.isEmpty())
				.map(visitorGroupName -> getExistingVisitorGroup(visitorGroupName, ingressName))
				.filter(visitorGroup -> visitorGroupHasSources(visitorGroup))
				.map(visitorGroup -> visitorGroup.getSpec().getSources())
				.flatMap(Collection::stream)
				.map(visitorGroupSpecSources -> visitorGroupSpecSources.getCidr())
				.distinct()
				.collect(joining(","));

		// by default, open access
		if (StringUtils.isEmpty(cidrListAsString)) {
			cidrListAsString = "0.0.0.0/0";
		}

		log.trace("computed cidr list : {}", cidrListAsString);
		if (!cidrListAsString.equals(getAnnotationValue(NGINX_INGRESS_WHITELIST_ANNOTATION_KEY, ingress))) {
			ingressRepository.patchIngressWithAnnotation(ingress, NGINX_INGRESS_WHITELIST_ANNOTATION_KEY, cidrListAsString);
		}

		log.trace("end of patching ingress {}", ingressName);
	}

	private boolean ingressIsLinkedToVisitorGroupName(Ingress ingress, String visitorGroupName) {
		return stream(getAnnotationValue(MUTABLE_INGRESS_VISITOR_GROUP_KEY, ingress, EMPTY_STRING).split(","))
				.anyMatch(s -> s.trim().equalsIgnoreCase(visitorGroupName));
	}

	private VisitorGroup getExistingVisitorGroup(String visitorGroupName, String ingressName) {
		try {
			return visitorGroupRepository.getVisitorGroupByName(visitorGroupName);
		}
		catch (VisitorGroupNotFoundException e) {
			log.error("panic: could not resolve visitorGroup {}, wont do anything in ingress {}", visitorGroupName, ingressName);
			throw new SafeTaskInterruptionOnErrorException(String.format("interrupted patch of ingress %s", ingressName), e);
		}
	}

	private boolean visitorGroupHasSources(VisitorGroup visitorGroup) {
		return allNotNull(visitorGroup, visitorGroup.getSpec(), visitorGroup.getSpec().getSources());
	}
}
