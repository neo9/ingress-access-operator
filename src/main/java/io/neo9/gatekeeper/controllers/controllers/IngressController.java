package io.neo9.gatekeeper.controllers.controllers;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.neo9.gatekeeper.exceptions.SafeTaskInterruptionOnErrorException;
import io.neo9.gatekeeper.services.VisitorGroupIngressReconciler;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import static io.neo9.gatekeeper.config.MutationLabels.MUTABLE_LABEL_KEY;
import static io.neo9.gatekeeper.config.MutationLabels.MUTABLE_LABEL_VALUE;

@Component
@Slf4j
public class IngressController {

	private final KubernetesClient kubernetesClient;

	private final VisitorGroupIngressReconciler visitorGroupIngressReconciler;

	private Watch ingressWatch;

	public IngressController(KubernetesClient kubernetesClient, VisitorGroupIngressReconciler visitorGroupIngressReconciler) {
		this.kubernetesClient = kubernetesClient;
		this.visitorGroupIngressReconciler = visitorGroupIngressReconciler;
	}

	@PostConstruct
	public void startWatchIngressChanges() {
		log.info("starting watch loop on ingress");
		ingressWatch = kubernetesClient.network().ingresses().withLabel(MUTABLE_LABEL_KEY, MUTABLE_LABEL_VALUE).watch(new Watcher<>() {
			@Override
			public boolean reconnecting() {
				return true;
			}

			@Override
			public void eventReceived(Action action, Ingress ingress) {
				switch (action) {
					case ADDED:
					case MODIFIED:
						log.info("update event detected for ingress : {}", ingress.getMetadata().getName());
						try {
							visitorGroupIngressReconciler.reconcile(ingress);
						}
						catch (SafeTaskInterruptionOnErrorException e) {
							log.error(e.getMessage());
						}
						break;
					default:
						// do nothing on ingress deletion
				}
			}

			@Override
			public void onClose() {
				Watcher.super.onClose();
			}

			@Override
			public void onClose(WatcherException cause) {
			}
		});
	}

	@PreDestroy
	public void stopWatchIngressChanges() {
		ingressWatch.close();
	}
}
