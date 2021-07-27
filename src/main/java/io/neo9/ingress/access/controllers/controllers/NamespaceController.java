package io.neo9.ingress.access.controllers.controllers;

import java.util.function.BiFunction;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.neo9.ingress.access.config.AdditionalWatchersConfig;
import io.neo9.ingress.access.exceptions.ResourceNotManagedByOperatorException;
import io.neo9.ingress.access.services.IstioSidecarReconciler;
import io.neo9.ingress.access.utils.RetryContext;
import io.neo9.ingress.access.utils.RetryableWatcher;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import static java.util.Objects.nonNull;

@Component
@Slf4j
public class NamespaceController {

	private final KubernetesClient kubernetesClient;

	private final AdditionalWatchersConfig additionalWatchersConfig;

	private final RetryContext retryContext = new RetryContext();

	private final BiFunction<Action, Namespace, Void> onEventReceived;

	private Watch namespaceWatchOnLabel;

	public NamespaceController(KubernetesClient kubernetesClient, AdditionalWatchersConfig additionalWatchersConfig, IstioSidecarReconciler istioSidecarReconciler) {
		this.kubernetesClient = kubernetesClient;
		this.additionalWatchersConfig = additionalWatchersConfig;
		this.onEventReceived = (action, namespace) -> {
			log.info("update event detected for namespace : {}", namespace.getMetadata().getName());
			// always reconcile sidecar config
			try {
				istioSidecarReconciler.reconcile(namespace);
			}
			catch (ResourceNotManagedByOperatorException e) {
				log.error("panic: could not work on resource {}", e.getResourceNamespaceName(), e);
			}
			return null;
		};
	}

	@PostConstruct
	public void startWatch() {
		if (additionalWatchersConfig.updateIstioIngressSidecar().isEnabled()) {
			log.info("starting watch loop on namespaces");
			watchNamespaces();
		}
	}

	@PreDestroy
	public void stopWatch() {
		log.info("closing watch loop on namespaces");
		if (nonNull(namespaceWatchOnLabel)) {
			namespaceWatchOnLabel.close();
		}
		retryContext.shutdown();
	}

	private void watchNamespaces() {
		namespaceWatchOnLabel = kubernetesClient.namespaces()
				.watch(new RetryableWatcher<>(
						retryContext,
						String.format("%s-all", Namespace.class.getSimpleName()),
						this::watchNamespaces,
						namespace -> true,
						onEventReceived
				));
	}
}