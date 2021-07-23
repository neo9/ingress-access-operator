package io.neo9.ingress.access.repositories;

import io.fabric8.kubernetes.client.KubernetesClient;
import me.snowdrop.istio.api.networking.v1beta1.Sidecar;
import me.snowdrop.istio.api.networking.v1beta1.SidecarBuilder;

import org.springframework.stereotype.Component;

@Component
public class SidecarRepository {

	private final KubernetesClient kubernetesClient;

	public SidecarRepository(KubernetesClient kubernetesClient) {
		this.kubernetesClient = kubernetesClient;
	}

	public Sidecar createOrReplace(Sidecar sidecar) {
		return kubernetesClient
				.resource(sidecar)
				.createOrReplace();
	}

	public Sidecar getSidecar(String namespace, String name) {
		Sidecar sidecar = new SidecarBuilder()
				.withNewMetadata()
				.withNamespace(namespace)
				.withName(name)
				.endMetadata()
				.build();
		return kubernetesClient
				.resource(sidecar)
				.inNamespace(namespace)
				.fromServer()
				.get();
	}

}
