package io.neo9.ingress.access.controllers.kubernetes;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.stereotype.Component;

/**
 * When we meet an exception, some listener are in a blocked state
 * and cannot recover by themself.
 *
 * The goal of this class is to centralize stop/start listen
 * action.
 */
@Component
public class RevivalControllerOrchestrator {

	private final IngressController ingressController;

	private final NamespaceController namespaceController;

	private final ServiceController serviceController;

	public RevivalControllerOrchestrator(IngressController ingressController, NamespaceController namespaceController, ServiceController serviceController) {
		this.ingressController = ingressController;
		this.namespaceController = namespaceController;
		this.serviceController = serviceController;
	}

	@PostConstruct
	public void startOrRestartWatch() {
		ingressController.startWatch(this);
		namespaceController.startWatch(this);
		serviceController.startWatch(this);
	}

	@PreDestroy
	public void stopWatch() {
		ingressController.stopWatch();
		namespaceController.stopWatch();
		serviceController.stopWatch();
	}
}
