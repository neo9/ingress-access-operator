package io.neo9.ingress.access.repositories;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.neo9.ingress.access.customresources.VisitorGroup;
import io.neo9.ingress.access.exceptions.VisitorGroupNotFoundException;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class VisitorGroupRepository {

	private final KubernetesClient kubernetesClient;

	public VisitorGroupRepository(KubernetesClient kubernetesClient) {
		this.kubernetesClient = kubernetesClient;
	}

	public VisitorGroup getVisitorGroupByName(String visitorGroupName) {
		VisitorGroup visitorGroup = kubernetesClient.customResources(VisitorGroup.class).withName(visitorGroupName)
				.get();
		if (visitorGroup == null) {
			throw new VisitorGroupNotFoundException(visitorGroupName);
		}
		return visitorGroup;
	}

	public List<VisitorGroup> getByLabel(String key, String value) {
		return kubernetesClient.customResources(VisitorGroup.class).withLabel(key, value).list().getItems();
	}

}
