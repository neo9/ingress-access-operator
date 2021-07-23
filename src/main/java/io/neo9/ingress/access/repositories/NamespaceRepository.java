package io.neo9.ingress.access.repositories;

import java.util.List;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.client.KubernetesClient;

import org.springframework.stereotype.Component;

@Component
public class NamespaceRepository {

	private final KubernetesClient kubernetesClient;

	public NamespaceRepository(KubernetesClient kubernetesClient) {
		this.kubernetesClient = kubernetesClient;
	}

	public List<Namespace> listNamespacesWithLabel(String labelKey, String labelValue) {
		return kubernetesClient.namespaces().withLabel(labelKey, labelValue).list().getItems();
	}

}
