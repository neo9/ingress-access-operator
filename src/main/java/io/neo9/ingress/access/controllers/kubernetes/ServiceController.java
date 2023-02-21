package io.neo9.ingress.access.controllers.kubernetes;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import io.neo9.ingress.access.config.AdditionalWatchersConfig;
import io.neo9.ingress.access.exceptions.ResourceNotManagedByOperatorException;
import io.neo9.ingress.access.exceptions.VisitorGroupNotFoundException;
import io.neo9.ingress.access.services.ServiceExposerReconciler;
import io.neo9.ingress.access.services.VisitorGroupIngressReconciler;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import static io.neo9.ingress.access.config.MutationLabels.*;
import static io.neo9.ingress.access.utils.common.KubernetesUtils.getResourceNamespaceAndName;
import static io.neo9.ingress.access.utils.common.KubernetesUtils.hasLabel;

@Component
@Slf4j
public class ServiceController extends ReconnectableSingleWatcher<Service, ServiceList, ServiceResource<Service>> {

	public ServiceController(KubernetesClient kubernetesClient, ServiceExposerReconciler serviceExposerReconciler,
			AdditionalWatchersConfig additionalWatchersConfig,
			VisitorGroupIngressReconciler visitorGroupIngressReconciler) {
		super(
				/* watch what */
				kubernetesClient.services().inAnyNamespace(),
				/* on event */
				(action, service) -> {
					String serviceNamespaceAndName = getResourceNamespaceAndName(service);
					log.trace("start process event on {}", serviceNamespaceAndName);
					switch (action) {
					case ADDED:
					case MODIFIED:
						log.info("update event detected for service : {}", serviceNamespaceAndName);
						// service whitelist
						if (hasLabel(service, MUTABLE_LABEL_KEY, MUTABLE_LABEL_VALUE)) {
							try {
								visitorGroupIngressReconciler.reconcile(service);
							}
							catch (VisitorGroupNotFoundException e) {
								log.error("panic: could not resolve visitorGroup {} for service {}",
										e.getVisitorGroupName(), serviceNamespaceAndName, e);
							}
						}
						// exposer
						if (additionalWatchersConfig.exposer().isEnabled()
								&& hasLabel(service, EXPOSE_LABEL_KEY, EXPOSE_LABEL_VALUE)) {
							try {
								serviceExposerReconciler.reconcile(service);
							}
							catch (ResourceNotManagedByOperatorException e) {
								log.error("panic: could not work on resource {}", e.getResourceNamespaceName(), e);
							}
						}
						break;
					case DELETED:
						log.info("delete event detected for service : {}", serviceNamespaceAndName);
						try {
							serviceExposerReconciler.reconcileOnDelete(service);
						}
						catch (ResourceNotManagedByOperatorException e) {
							log.error("panic: could not work on resource {}", e.getResourceNamespaceName(), e);
						}
						break;
					default:
						// do nothing on error
						break;
					}
					log.trace("end of process event on {}", serviceNamespaceAndName);
					return null;
				});
	}

}
