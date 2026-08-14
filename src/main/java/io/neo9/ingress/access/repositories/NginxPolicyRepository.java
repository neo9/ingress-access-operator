package io.neo9.ingress.access.repositories;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.neo9.ingress.access.customresources.external.nginx.NginxPolicy;

import org.springframework.stereotype.Component;

@Component
public class NginxPolicyRepository {

	private final KubernetesClient kubernetesClient;

	public NginxPolicyRepository(KubernetesClient kubernetesClient) {
		this.kubernetesClient = kubernetesClient;
	}

	public NginxPolicy createOrReplace(NginxPolicy policy) {
		return kubernetesClient.resource(policy).createOrReplace();
	}

	public NginxPolicy get(String namespace, String name) {
		return kubernetesClient.resources(NginxPolicy.class).inNamespace(namespace).withName(name).get();
	}

	public Boolean delete(String namespace, String name) {
		return !kubernetesClient.resources(NginxPolicy.class).inNamespace(namespace).withName(name).delete().isEmpty();
	}

}
