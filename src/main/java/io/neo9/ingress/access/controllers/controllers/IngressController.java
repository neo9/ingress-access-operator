package io.neo9.ingress.access.controllers.controllers;

import java.util.function.BiFunction;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.neo9.ingress.access.config.AdditionalWatchers;
import io.neo9.ingress.access.exceptions.VisitorGroupNotFoundException;
import io.neo9.ingress.access.services.VisitorGroupIngressReconciler;
import io.neo9.ingress.access.utils.RetryContext;
import io.neo9.ingress.access.utils.RetryableWatcher;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import static io.neo9.ingress.access.config.MutationLabels.MUTABLE_LABEL_KEY;
import static io.neo9.ingress.access.config.MutationLabels.MUTABLE_LABEL_VALUE;
import static io.neo9.ingress.access.utils.KubernetesUtils.getAnnotationValue;
import static io.neo9.ingress.access.utils.KubernetesUtils.getResourceNamespaceAndName;
import static java.util.Objects.nonNull;

@Component
@Slf4j
public class IngressController {

	private final KubernetesClient kubernetesClient;

	private final AdditionalWatchers additionalWatchers;

	private final RetryContext retryContext = new RetryContext();

	private final BiFunction<Action, Ingress, Void> onEventReceived;

	private Watch ingressWatchOnLabel;

	private Watch ingressWatchOnAnnotations;

	public IngressController(KubernetesClient kubernetesClient, VisitorGroupIngressReconciler visitorGroupIngressReconciler, AdditionalWatchers additionalWatchers) {
		this.kubernetesClient = kubernetesClient;
		this.additionalWatchers = additionalWatchers;
		this.onEventReceived = (action, ingress) -> {
			String ingressNamespaceAndName = getResourceNamespaceAndName(ingress);
			switch (action) {
				case ADDED:
				case MODIFIED:
					log.info("update event detected for ingress : {}", ingressNamespaceAndName);
					try {
						visitorGroupIngressReconciler.reconcile(ingress);
					}
					catch (VisitorGroupNotFoundException e) {
						log.error("panic: could not resolve visitorGroup {} for ingress {}", e.getVisitorGroupName(), ingressNamespaceAndName, e);
					}
					break;
				default:
					// do nothing on ingress deletion
			}
			return null;
		};
	}

	@PostConstruct
	public void startWatch() {
		log.info("starting watch loop on ingress (by label)");
		watchIngressesOnLabel();
		if (additionalWatchers.watchIngressAnnotations()) {
			log.info("starting watch loop on ingress (by name prefix)");
			watchIngressesOnNamePrefix();
		}
	}

	@PreDestroy
	public void stopWatch() {
		log.info("closing watch loop on ingress");
		if (nonNull(ingressWatchOnLabel)) {
			ingressWatchOnLabel.close();
		}
		if (nonNull(ingressWatchOnAnnotations)) {
			ingressWatchOnAnnotations.close();
		}
		retryContext.shutdown();
	}

	private void watchIngressesOnLabel() {
		ingressWatchOnLabel = kubernetesClient.network().ingresses()
				.inAnyNamespace()
				.withLabel(MUTABLE_LABEL_KEY, MUTABLE_LABEL_VALUE)
				.watch(new RetryableWatcher<>(
						retryContext,
						String.format("%s-onLabel", Ingress.class.getSimpleName()),
						this::watchIngressesOnLabel,
						ingress -> true,
						onEventReceived
				));
	}

	private void watchIngressesOnNamePrefix() {
		ingressWatchOnAnnotations = kubernetesClient.network().ingresses()
				.inAnyNamespace()
				.withoutLabel(MUTABLE_LABEL_KEY, MUTABLE_LABEL_VALUE) // exclude because already retrieved by previous watcher
				.watch(new RetryableWatcher<>(
						retryContext,
						String.format("%s-onNamePrefix", Ingress.class.getSimpleName()),
						this::watchIngressesOnNamePrefix,
						ingress -> getAnnotationValue(MUTABLE_LABEL_KEY, ingress, "").equalsIgnoreCase(MUTABLE_LABEL_VALUE),
						onEventReceived
				));
	}
}
