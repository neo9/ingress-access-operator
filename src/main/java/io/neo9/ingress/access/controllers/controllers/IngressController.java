package io.neo9.ingress.access.controllers.controllers;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.neo9.ingress.access.exceptions.VisitorGroupNotFoundException;
import io.neo9.ingress.access.services.VisitorGroupIngressReconciler;
import io.neo9.ingress.access.utils.RetryContext;
import io.neo9.ingress.access.utils.RetryableWatcher;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import static io.neo9.ingress.access.config.MutationLabels.MUTABLE_LABEL_KEY;
import static io.neo9.ingress.access.config.MutationLabels.MUTABLE_LABEL_VALUE;

@Component
@Slf4j
public class IngressController {

	private final KubernetesClient kubernetesClient;

	private final VisitorGroupIngressReconciler visitorGroupIngressReconciler;

	private final RetryContext retryContext = new RetryContext();

	private Watch ingressWatch;

	public IngressController(KubernetesClient kubernetesClient, VisitorGroupIngressReconciler visitorGroupIngressReconciler) {
		this.kubernetesClient = kubernetesClient;
		this.visitorGroupIngressReconciler = visitorGroupIngressReconciler;
	}

	@PostConstruct
	public void startWatch() {
		log.info("starting watch loop on ingress");
		watchIngresses();
	}

	@PreDestroy
	public void stopWatch() {
		log.info("closing watch loop on ingress");
		ingressWatch.close();
		retryContext.shutdown();
	}

	private void watchIngresses() {
		Watcher<Ingress> watcher = new RetryableWatcher<>(retryContext, Ingress.class.getSimpleName(), this::watchIngresses, (action, ingress) -> {
			switch (action) {
				case ADDED:
				case MODIFIED:
					String ingressName = ingress.getMetadata().getName();
					log.info("update event detected for ingress : {}", ingressName);
					try {
						visitorGroupIngressReconciler.reconcile(ingress);
					}
					catch (VisitorGroupNotFoundException e) {
						log.error("panic: could not resolve visitorGroup {} for ingress {}", e.getVisitorGroupName(), ingressName, e);
					}
					break;
				default:
					// do nothing on ingress deletion
			}
			return null;
		});

		ingressWatch = kubernetesClient.network().ingresses()
				.inAnyNamespace()
				.withLabel(MUTABLE_LABEL_KEY, MUTABLE_LABEL_VALUE)
				.watch(watcher);
	}

}
