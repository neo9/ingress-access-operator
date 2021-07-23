package io.neo9.ingress.access.config;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.ControllerUtils;
import io.javaoperatorsdk.operator.Operator;
import io.javaoperatorsdk.operator.api.ResourceController;
import io.javaoperatorsdk.operator.api.config.AbstractConfigurationService;
import io.javaoperatorsdk.operator.api.config.RetryConfiguration;
import io.javaoperatorsdk.operator.api.config.Utils;
import io.javaoperatorsdk.operator.config.runtime.AnnotationConfiguration;
import io.javaoperatorsdk.operator.springboot.starter.ControllerProperties;
import io.javaoperatorsdk.operator.springboot.starter.OperatorConfigurationProperties;
import io.javaoperatorsdk.operator.springboot.starter.RetryProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.GenericTypeResolver;

@Configuration
public class NativeOperatorConfig extends AbstractConfigurationService {

	public NativeOperatorConfig() {
		super(Utils.loadFromProperties());
	}

	@Bean
	public Operator operator(
			KubernetesClient kubernetesClient,
			List<ResourceController<?>> resourceControllers,
			OperatorConfigurationProperties configuration) {
		Operator operator = new Operator(kubernetesClient, this);
		resourceControllers.forEach(r -> {
			Class<?>[] typeArguments = GenericTypeResolver.resolveTypeArguments(r.getClass(), ResourceController.class);
			operator.register(processController(r, typeArguments[0], configuration));
		});
		return operator;
	}

	private ResourceController<?> processController(ResourceController<?> controller, Class<?> typeArgument, OperatorConfigurationProperties configuration) {
		final var controllerPropertiesMap = configuration.getControllers();
		final var name = ControllerUtils.getNameFor(controller);
		var controllerProps = controllerPropertiesMap.get(name);
		register(new NativeOperatorConfig.ConfigurationWrapper(controller, controllerProps, typeArgument));
		return controller;
	}

	private static class ConfigurationWrapper<R extends CustomResource>
			extends AnnotationConfiguration<R> {
		private final Optional<ControllerProperties> properties;

		private Class<?> customResourceType;

		private ConfigurationWrapper(
				ResourceController<R> controller, ControllerProperties properties, Class<?> customResourceType) {
			super(controller);
			this.properties = Optional.ofNullable(properties);
			this.customResourceType = customResourceType;
		}

		@Override
		public String getCRDName() {
			return properties.map(ControllerProperties::getCRDName).orElse(super.getCRDName());
		}

		@Override
		public String getFinalizer() {
			return properties.map(ControllerProperties::getFinalizer).orElse(super.getFinalizer());
		}

		@Override
		public boolean isGenerationAware() {
			return properties
					.map(ControllerProperties::isGenerationAware)
					.orElse(super.isGenerationAware());
		}

		@Override
		public Class<R> getCustomResourceClass() {
			return (Class<R>) customResourceType;
		}

		@Override
		public Set<String> getNamespaces() {
			return properties.map(ControllerProperties::getNamespaces).orElse(super.getNamespaces());
		}

		@Override
		public RetryConfiguration getRetryConfiguration() {
			return properties
					.map(ControllerProperties::getRetry)
					.map(RetryProperties::asRetryConfiguration)
					.orElse(RetryConfiguration.DEFAULT);
		}
	}
}
