package io.neo9.ingress.access.controllers.kubernetes;

import java.util.function.BiFunction;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.neo9.ingress.access.config.AdditionalWatchersConfig;
import io.neo9.ingress.access.exceptions.VisitorGroupNotFoundException;
import io.neo9.ingress.access.services.VisitorGroupIngressReconciler;
import io.neo9.ingress.access.utils.Debouncer;
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

	private final AdditionalWatchersConfig additionalWatchersConfig;

	private final RetryContext retryContext = new RetryContext();

	private final BiFunction<Action, Ingress, Void> onEventReceived;

	private final Debouncer debouncer = new Debouncer();

	private Watch ingressWatchOnLabel;

	private Watch ingressWatchOnAnnotations;

	public IngressController(KubernetesClient kubernetesClient, VisitorGroupIngressReconciler visitorGroupIngressReconciler, AdditionalWatchersConfig additionalWatchersConfig) {
		this.kubernetesClient = kubernetesClient;
		this.additionalWatchersConfig = additionalWatchersConfig;
		this.onEventReceived = (action, ingress) -> {
			String ingressNamespaceAndName = getResourceNamespaceAndName(ingress);
			switch (action) {
				case ADDED:
				case MODIFIED:
					log.info("update event detected for ingress : {}", ingressNamespaceAndName);
					debouncer.debounce(ingressNamespaceAndName, () -> {
						try {
							visitorGroupIngressReconciler.reconcile(ingress);
						}
						catch (VisitorGroupNotFoundException e) {
							log.error("panic: could not resolve visitorGroup {} for ingress {}", e.getVisitorGroupName(), ingressNamespaceAndName, e);
						}
					});
					break;
				default:
					// do nothing on ingress deletion
					break;
			}
			return null;
		};
	}

	@PostConstruct
	public void startWatch() {
		watchIngressesOnLabel();
		if (additionalWatchersConfig.watchIngressAnnotations().isEnabled()) {
			watchIngressesOnAnnotation();
		}
	}

	@PreDestroy
	public void stopWatch() {
		closeIngressWatchOnLabel();
		closeIngressWatchOnAnnotations();
		retryContext.shutdown();
		debouncer.shutdown();
	}

	private void closeIngressWatchOnLabel() {
		if (nonNull(ingressWatchOnLabel)) {
			log.info("closing watch loop on ingress (by label)");
			ingressWatchOnLabel.close();
			ingressWatchOnLabel = null;
		}
	}

	private void closeIngressWatchOnAnnotations() {
		if (nonNull(ingressWatchOnAnnotations)) {
			log.info("closing watch loop on ingress (by annotations)");
			ingressWatchOnAnnotations.close();
			ingressWatchOnAnnotations = null;
		}
	}

	private void watchIngressesOnLabel() {
		closeIngressWatchOnLabel();
		log.info("starting watch loop on ingress (by label)");
		ingressWatchOnLabel = kubernetesClient.network().v1().ingresses()
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

	private void watchIngressesOnAnnotation() {
		closeIngressWatchOnAnnotations();
		log.info("starting watch loop on ingress (by annotations)");
		ingressWatchOnAnnotations = kubernetesClient.network().v1().ingresses()
				.inAnyNamespace()
				.withoutLabel(MUTABLE_LABEL_KEY, MUTABLE_LABEL_VALUE) // exclude because already retrieved by previous watcher
				.watch(new RetryableWatcher<>(
						retryContext,
						String.format("%s-onAnnotations", Ingress.class.getSimpleName()),
						this::watchIngressesOnAnnotation,
						ingress -> getAnnotationValue(MUTABLE_LABEL_KEY, ingress, "").equalsIgnoreCase(MUTABLE_LABEL_VALUE),
						onEventReceived
				));
	}
}
