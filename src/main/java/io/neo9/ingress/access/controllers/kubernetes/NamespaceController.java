package io.neo9.ingress.access.controllers.kubernetes;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.neo9.ingress.access.config.AdditionalWatchersConfig;
import io.neo9.ingress.access.exceptions.ResourceNotManagedByOperatorException;
import io.neo9.ingress.access.services.IstioSidecarReconciler;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NamespaceController extends ReconnectableSingleWatcher<Namespace, NamespaceList> {

	public NamespaceController(KubernetesClient kubernetesClient, IstioSidecarReconciler istioSidecarReconciler, AdditionalWatchersConfig additionalWatchersConfig) {
		super(
				/* activation condition */
				additionalWatchersConfig.updateIstioIngressSidecar().isEnabled(),
				/* unique name */
				"namespaces-all",
				/* watch what */
				kubernetesClient.namespaces(),
				/* on event */
				(action, namespace) -> {
					log.info("update event detected for namespace : {}", namespace.getMetadata().getName());
					try {
						istioSidecarReconciler.reconcile();
					}
					catch (ResourceNotManagedByOperatorException e) {
						log.error("panic: could not work on resource {}", e.getResourceNamespaceName(), e);
					}
					return null;
				}
		);
	}

}
