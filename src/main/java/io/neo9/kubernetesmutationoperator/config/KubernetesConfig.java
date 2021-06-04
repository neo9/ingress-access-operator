package io.neo9.kubernetesmutationoperator.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.config.runtime.DefaultConfigurationService;
import io.neo9.kubernetesmutationoperator.controllers.operators.VisitorGroupController;

@Configuration
public class KubernetesConfig {

	@Bean
	public KubernetesClient kubernetesClient() {
		return new DefaultKubernetesClient();
	}

	@Bean
	public VisitorGroupController customServiceController(KubernetesClient client) {
		return new VisitorGroupController(client);
	}

	//  Register all controller beans
	@Bean
	public Operator operator(KubernetesClient client, List<ResourceController> controllers) {
		Operator operator = new Operator(client, DefaultConfigurationService.instance());
		controllers.forEach(operator::register);
		return operator;
	}

}
