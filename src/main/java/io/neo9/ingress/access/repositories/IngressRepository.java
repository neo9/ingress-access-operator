package io.neo9.ingress.access.repositories;

import java.util.List;

import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

import org.springframework.stereotype.Component;

@Component
public class IngressRepository {

	private final KubernetesClient kubernetesClient;

	public IngressRepository(KubernetesClient kubernetesClient) {
		this.kubernetesClient = kubernetesClient;
	}

	public List<Ingress> listIngressWithLabel(String labelKey, String labelValue) {
		return kubernetesClient.network().ingresses().inAnyNamespace().withLabel(labelKey, labelValue).list().getItems();
	}

	public List<Ingress> listIngressWithoutLabel(String labelKey, String labelValue) {
		return kubernetesClient.network().ingresses().inAnyNamespace().withoutLabel(labelKey, labelValue).list().getItems();
	}

	public Ingress patchWithAnnotation(Ingress ingress, String annotationKey, String annotationValue) {
		return kubernetesClient.network().ingress()
				.inNamespace(ingress.getMetadata().getNamespace())
				.withName(ingress.getMetadata().getName())
				.edit(
						ing -> new IngressBuilder(ing)
								.editMetadata()
								.addToAnnotations(annotationKey, annotationValue)
								.and()
								.build()
				);
	}

	public Ingress createOrReplace(Ingress ingress) {
		return kubernetesClient
				.resource(ingress)
				.createOrReplace();
	}

	public Ingress get(String namespace, String name) {
		return kubernetesClient
				.network().ingresses()
				.inNamespace(namespace)
				.withName(name)
				.fromServer()
				.get();
	}

	public Boolean delete(String namespace, String name) {
		return kubernetesClient
				.network().ingresses()
				.inNamespace(namespace)
				.withName(name)
				.delete();
	}

}
