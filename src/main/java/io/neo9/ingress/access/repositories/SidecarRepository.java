package io.neo9.ingress.access.repositories;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.neo9.ingress.access.customresources.external.istio.Sidecar;

import org.springframework.stereotype.Component;

@Component
public class SidecarRepository {

	private final KubernetesClient kubernetesClient;

	public SidecarRepository(KubernetesClient kubernetesClient) {
		this.kubernetesClient = kubernetesClient;
	}

	public Sidecar createOrReplace(Sidecar sidecar) {
		return kubernetesClient
				.customResources(Sidecar.class)
				.inNamespace(sidecar.getMetadata().getNamespace())
				.withName(sidecar.getMetadata().getName())
				.createOrReplace(sidecar);
	}

	public Sidecar getSidecar(String namespace, String name) {
		return kubernetesClient
				.customResources(Sidecar.class)
				.inNamespace(namespace)
				.withName(name)
				.fromServer()
				.get();
	}

}
