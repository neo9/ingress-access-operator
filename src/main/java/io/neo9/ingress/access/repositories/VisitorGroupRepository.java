package io.neo9.ingress.access.repositories;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.neo9.ingress.access.customresources.VisitorGroup;
import io.neo9.ingress.access.customresources.VisitorGroupList;
import io.neo9.ingress.access.customresources.external.istio.Sidecar;
import io.neo9.ingress.access.customresources.external.istio.SidecarList;
import io.neo9.ingress.access.exceptions.VisitorGroupNotFoundException;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class VisitorGroupRepository {

	private final MixedOperation<VisitorGroup, VisitorGroupList, Resource<VisitorGroup>> visitorGroupClient;

	public VisitorGroupRepository(KubernetesClient kubernetesClient) {
		this.visitorGroupClient = kubernetesClient.resources(VisitorGroup.class, VisitorGroupList.class);
	}

	public VisitorGroup getVisitorGroupByName(String visitorGroupName) {
		VisitorGroup visitorGroup = visitorGroupClient.withName(visitorGroupName).get();
		if (visitorGroup == null) {
			throw new VisitorGroupNotFoundException(visitorGroupName);
		}
		return visitorGroup;
	}

	public List<VisitorGroup> getByLabel(String key, String value) {
		return visitorGroupClient.withLabel(key, value).list().getItems();
	}

}
