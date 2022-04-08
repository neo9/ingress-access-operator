package io.neo9.ingress.access.controllers.kubernetes;

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.neo9.ingress.access.config.AdditionalWatchersConfig;
import io.neo9.ingress.access.exceptions.VisitorGroupNotFoundException;
import io.neo9.ingress.access.services.VisitorGroupIngressReconciler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static io.neo9.ingress.access.config.MutationLabels.*;
import static io.neo9.ingress.access.utils.common.KubernetesUtils.*;

@Component
@Slf4j
public class IngressAllController extends ReconnectableSingleWatcher<Ingress, IngressList> {

	public IngressAllController(KubernetesClient kubernetesClient,
			VisitorGroupIngressReconciler visitorGroupIngressReconciler,
			AdditionalWatchersConfig additionalWatchersConfig) {
		super(
				/* activation condition */
				additionalWatchersConfig.defaultFiltering().isEnabled(),
				/* unique name */
				"ingress-all",
				/* watch what */
				kubernetesClient.network().v1().ingresses().inAnyNamespace(),
				/* on event */
				(action, ingress) -> {
					String ingressNamespaceAndName = getResourceNamespaceAndName(ingress);
					switch (action) {
					case ADDED:
					case MODIFIED:
						log.info("update event detected for ingress : {}", ingressNamespaceAndName);
						try {
							visitorGroupIngressReconciler.reconcile(ingress);
						}
						catch (VisitorGroupNotFoundException e) {
							log.error("panic: could not resolve visitorGroup {} for ingress {}",
									e.getVisitorGroupName(), ingressNamespaceAndName, e);
						}
						break;
					default:
						// do nothing on ingress deletion
						break;
					}
					return null;
				});
	}

}
