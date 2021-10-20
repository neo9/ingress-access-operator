package io.neo9.ingress.access.controllers.kubernetes;

import java.util.function.BiFunction;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.neo9.ingress.access.config.AdditionalWatchersConfig;
import io.neo9.ingress.access.exceptions.ResourceNotManagedByOperatorException;
import io.neo9.ingress.access.services.IstioSidecarReconciler;
import io.neo9.ingress.access.utils.Debouncer;
import io.neo9.ingress.access.utils.RetryContext;
import io.neo9.ingress.access.utils.RetryableWatcher;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import static java.util.Objects.nonNull;

@Component
@Slf4j
public class NamespaceController implements ReconnectableWatcher {

	private final KubernetesClient kubernetesClient;

	private final AdditionalWatchersConfig additionalWatchersConfig;

	private final RetryContext retryContext = new RetryContext();

	private final Debouncer debouncer = new Debouncer();

	private final BiFunction<Action, Namespace, Void> onEventReceived;

	private Watch namespaceWatchOnLabel;

	public NamespaceController(KubernetesClient kubernetesClient, AdditionalWatchersConfig additionalWatchersConfig, IstioSidecarReconciler istioSidecarReconciler) {
		this.kubernetesClient = kubernetesClient;
		this.additionalWatchersConfig = additionalWatchersConfig;
		this.onEventReceived = (action, namespace) -> {
			log.info("update event detected for namespace : {}", namespace.getMetadata().getName());
			// always reconcile sidecar config
			debouncer.debounce("all-namespaces-debounce", () -> {
				try {
					istioSidecarReconciler.reconcile();
				}
				catch (ResourceNotManagedByOperatorException e) {
					log.error("panic: could not work on resource {}", e.getResourceNamespaceName(), e);
				}
			});
			return null;
		};
	}

	public void startWatch(ReconnectableControllerOrchestrator reconnectableControllerOrchestrator) {
		if (additionalWatchersConfig.updateIstioIngressSidecar().isEnabled()) {
			watchNamespaces(reconnectableControllerOrchestrator);
		}
	}

	public void stopWatch() {
		closeNamespaceWatch();
		retryContext.shutdown();
		debouncer.shutdown();
	}

	private void closeNamespaceWatch() {
		if (nonNull(namespaceWatchOnLabel)) {
			log.info("closing watch loop on namespaces");
			namespaceWatchOnLabel.close();
			namespaceWatchOnLabel = null;
		}
	}

	private void watchNamespaces(ReconnectableControllerOrchestrator reconnectableControllerOrchestrator) {
		closeNamespaceWatch();
		log.info("starting watch loop on namespaces");
		namespaceWatchOnLabel = kubernetesClient.namespaces()
				.watch(new RetryableWatcher<>(
						retryContext,
						String.format("%s-all", Namespace.class.getSimpleName()),
						reconnectableControllerOrchestrator::startOrRestartWatch,
						namespace -> true,
						onEventReceived
				));
	}
}