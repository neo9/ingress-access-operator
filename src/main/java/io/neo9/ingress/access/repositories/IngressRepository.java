package io.neo9.ingress.access.repositories;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

import org.springframework.stereotype.Component;

@Component
public class IngressRepository {

	private final KubernetesClient kubernetesClient;

	public IngressRepository(KubernetesClient kubernetesClient) {
		this.kubernetesClient = kubernetesClient;
	}

	public List<Ingress> listAllIngress() {
		return kubernetesClient.network().v1().ingresses().inAnyNamespace().list().getItems();
	}

	public List<Ingress> listIngressWithLabel(String labelKey, String labelValue) {
		return kubernetesClient.network().v1().ingresses().inAnyNamespace().withLabel(labelKey, labelValue).list()
				.getItems();
	}

	public Ingress patchWithAnnotations(Ingress ingress, Map<String, String> annotations) {
		return kubernetesClient.network().v1().ingresses().inNamespace(ingress.getMetadata().getNamespace())
				.withName(ingress.getMetadata().getName())
				.edit(ing -> new IngressBuilder(ing).editMetadata().addToAnnotations(annotations).and().build());
	}

	public Ingress patchAnnotations(Ingress ingress, Map<String, String> annotationsToAdd,
			String... annotationsToRemove) {
		return kubernetesClient.network().v1().ingresses().inNamespace(ingress.getMetadata().getNamespace())
				.withName(ingress.getMetadata().getName()).edit(ing -> {
					var metadata = new IngressBuilder(ing).editMetadata();
					if (annotationsToRemove != null) {
						for (String key : annotationsToRemove) {
							metadata.removeFromAnnotations(key);
						}
					}
					if (annotationsToAdd != null && !annotationsToAdd.isEmpty()) {
						metadata.addToAnnotations(annotationsToAdd);
					}
					return metadata.and().build();
				});
	}

	public Ingress createOrReplace(Ingress ingress) {
		return kubernetesClient.resource(ingress).createOrReplace();
	}

	public Ingress get(String namespace, String name) {
		return kubernetesClient.network().v1().ingresses().inNamespace(namespace).withName(name).get();
	}

	public Boolean delete(String namespace, String name) {
		return !kubernetesClient.network().v1().ingresses().inNamespace(namespace).withName(name).delete().isEmpty();
	}

}
