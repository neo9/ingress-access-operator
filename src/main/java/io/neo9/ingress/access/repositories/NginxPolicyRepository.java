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
		return kubernetesClient.customResources(NginxPolicy.class).inNamespace(policy.getMetadata().getNamespace())
				.withName(policy.getMetadata().getName()).createOrReplace(policy);
	}

	public NginxPolicy get(String namespace, String name) {
		return kubernetesClient.customResources(NginxPolicy.class).inNamespace(namespace).withName(name).fromServer().get();
	}

	public Boolean delete(String namespace, String name) {
		return kubernetesClient.customResources(NginxPolicy.class).inNamespace(namespace).withName(name).delete();
	}

}
