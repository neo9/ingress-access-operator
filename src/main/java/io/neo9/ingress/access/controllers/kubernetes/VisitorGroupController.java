package io.neo9.ingress.access.controllers.kubernetes;

import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.neo9.ingress.access.customresources.VisitorGroup;
import io.neo9.ingress.access.services.VisitorGroupIngressReconciler;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

@Controller
@Component
@Slf4j
public class VisitorGroupController implements ResourceController<VisitorGroup> {

	private final VisitorGroupIngressReconciler visitorGroupIngressReconciler;

	public VisitorGroupController(VisitorGroupIngressReconciler visitorGroupIngressReconciler) {
		this.visitorGroupIngressReconciler = visitorGroupIngressReconciler;
	}

	@Override
	public UpdateControl<VisitorGroup> createOrUpdateResource(VisitorGroup visitorGroup, Context<VisitorGroup> context) {
		log.info("update event detected for visitor group : {}", visitorGroup.getMetadata().getName());
		visitorGroupIngressReconciler.reconcile(visitorGroup);
		return UpdateControl.updateCustomResource(visitorGroup);
	}

	@Override
	public DeleteControl deleteResource(VisitorGroup visitorGroup, Context<VisitorGroup> context) {
		log.info("delete event detected for visitor group : {}", visitorGroup.getMetadata().getName());
		visitorGroupIngressReconciler.reconcile(visitorGroup); // will display panic message if there still
		return DeleteControl.DEFAULT_DELETE;
	}
}
