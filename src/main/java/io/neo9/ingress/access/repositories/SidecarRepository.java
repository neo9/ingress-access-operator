package io.neo9.ingress.access.repositories;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.neo9.ingress.access.customresources.external.istio.Sidecar;
import io.neo9.ingress.access.customresources.external.istio.SidecarList;

import org.springframework.stereotype.Component;

@Component
public class SidecarRepository {

	private final MixedOperation<Sidecar, SidecarList, Resource<Sidecar>> sidecarClient;

	public SidecarRepository(KubernetesClient kubernetesClient) {
		this.sidecarClient = kubernetesClient.resources(Sidecar.class, SidecarList.class);
	}

	public Sidecar createOrReplace(Sidecar sidecar) {
		return sidecarClient.inNamespace(sidecar.getMetadata().getNamespace())
			.withName(sidecar.getMetadata().getName())
			.createOrReplace(sidecar);
	}

	public Sidecar getSidecar(String namespace, String name) {
		return sidecarClient.inNamespace(namespace).withName(name).fromServer().get();
	}

}
