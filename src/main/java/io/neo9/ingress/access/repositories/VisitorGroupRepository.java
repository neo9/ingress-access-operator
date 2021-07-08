package io.neo9.ingress.access.repositories;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.neo9.ingress.access.customresources.VisitorGroup;
import io.neo9.ingress.access.exceptions.VisitorGroupNotFoundException;

import org.springframework.stereotype.Component;

@Component
public class VisitorGroupRepository {

	private final KubernetesClient kubernetesClient;

	public VisitorGroupRepository(KubernetesClient kubernetesClient) {
		this.kubernetesClient = kubernetesClient;
	}

	public VisitorGroup getVisitorGroupByName(String visitorGroupName) {
		VisitorGroup visitorGroup = kubernetesClient.customResources(VisitorGroup.class).withName(visitorGroupName).get();
		if (visitorGroup == null) {
			throw new VisitorGroupNotFoundException(visitorGroupName);
		}
		return visitorGroup;
	}
}
