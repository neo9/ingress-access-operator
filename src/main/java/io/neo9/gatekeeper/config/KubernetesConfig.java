package io.neo9.gatekeeper.config;

import java.util.List;

import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.config.runtime.DefaultConfigurationService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KubernetesConfig {

	@Bean
	public KubernetesClient kubernetesClient() {
		return new DefaultKubernetesClient();
	}

	//  Register all controller beans
	@Bean
	public Operator operator(KubernetesClient client, List<ResourceController> controllers) {
		Operator operator = new Operator(client, DefaultConfigurationService.instance());
		controllers.forEach(operator::register);
		return operator;
	}

}
