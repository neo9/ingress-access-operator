package io.neo9.ingress.access.controllers.kubernetes;

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.neo9.ingress.access.config.AdditionalWatchersConfig;
import io.neo9.ingress.access.exceptions.VisitorGroupNotFoundException;
import io.neo9.ingress.access.services.VisitorGroupIngressReconciler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static io.neo9.ingress.access.config.MutationLabels.LEGACY_MUTABLE_LABEL_KEY;
import static io.neo9.ingress.access.config.MutationLabels.MUTABLE_LABEL_VALUE;
import static io.neo9.ingress.access.utils.common.KubernetesUtils.getResourceNamespaceAndName;

@Component
@Slf4j
public class IngressOnLegacyLabelController extends ReconnectableSingleWatcher<Ingress, IngressList> {

	public IngressOnLegacyLabelController(KubernetesClient kubernetesClient,
			VisitorGroupIngressReconciler visitorGroupIngressReconciler,
			AdditionalWatchersConfig additionalWatchersConfig) {
		super(
				/* activation condition */
				// avoid double reconciliation
				!additionalWatchersConfig.defaultFiltering().isEnabled(),
				/* unique name */
				"ingress-onLabelLegacy",
				/* watch what */
				kubernetesClient.network().v1().ingresses().inAnyNamespace().withLabel(LEGACY_MUTABLE_LABEL_KEY,
						MUTABLE_LABEL_VALUE),
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
