package io.neo9.ingress.access.controllers.kubernetes;

import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.neo9.ingress.access.customresources.VisitorGroup;
import io.neo9.ingress.access.services.VisitorGroupIngressReconciler;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@ControllerConfiguration
@Component
@Slf4j
public class VisitorGroupController implements Reconciler<VisitorGroup>, Cleaner<VisitorGroup> {

	private final VisitorGroupIngressReconciler visitorGroupIngressReconciler;

	public VisitorGroupController(VisitorGroupIngressReconciler visitorGroupIngressReconciler) {
		this.visitorGroupIngressReconciler = visitorGroupIngressReconciler;
	}

	@Override
	public UpdateControl<VisitorGroup> reconcile(VisitorGroup visitorGroup, Context<VisitorGroup> context) {
		String visitorGroupName = visitorGroup.getMetadata().getName();
		log.info("update event detected for visitor group : {}", visitorGroupName);
		try {
			visitorGroupIngressReconciler.reconcile(visitorGroup);
		}
		catch (RuntimeException e) {
			log.error("reconcile failed for visitor group {}", visitorGroupName, e);
			throw e;
		}
		// CRD has no status subresource; a statusUpdate 404s and exhausts retries.
		return UpdateControl.noUpdate();
	}

	@Override
	public DeleteControl cleanup(VisitorGroup visitorGroup, Context<VisitorGroup> context) {
		String visitorGroupName = visitorGroup.getMetadata().getName();
		log.info("delete event detected for visitor group : {}", visitorGroupName);
		try {
			visitorGroupIngressReconciler.reconcile(visitorGroup); // will display panic
																	// message if there
																	// still
		}
		catch (RuntimeException e) {
			log.error("cleanup reconcile failed for visitor group {}", visitorGroupName, e);
			throw e;
		}
		return DeleteControl.defaultDelete();
	}

}
