package io.neo9.kubernetesmutationoperator.controllers.operators;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.Context;
import io.javaoperatorsdk.operator.api.Controller;
import io.javaoperatorsdk.operator.api.DeleteControl;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.UpdateControl;
import io.neo9.kubernetesmutationoperator.customresource.VisitorGroup;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class VisitorGroupController implements ResourceController<VisitorGroup> {

	private final KubernetesClient kubernetesClient;

	public VisitorGroupController(KubernetesClient kubernetesClient) {
		this.kubernetesClient = kubernetesClient;
	}

	@Override
	public UpdateControl<VisitorGroup> createOrUpdateResource(VisitorGroup resource, Context<VisitorGroup> context) {
		log.info("update event detected for visitor group : {}", resource.getMetadata().getName());
		return UpdateControl.updateCustomResource(resource);
	}

	@Override
	public DeleteControl deleteResource(VisitorGroup resource, Context<VisitorGroup> context) {
		log.info("delete event detected for visitor group : {}", resource.getMetadata().getName());
		return DeleteControl.DEFAULT_DELETE;
	}
}
