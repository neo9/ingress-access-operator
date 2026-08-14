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
		return kubernetesClient.resource(sidecar).createOrReplace();
	}

	public Sidecar getSidecar(String namespace, String name) {
		return kubernetesClient.resources(Sidecar.class).inNamespace(namespace).withName(name).get();
	}

}
