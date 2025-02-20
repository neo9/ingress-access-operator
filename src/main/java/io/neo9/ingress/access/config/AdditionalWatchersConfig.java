package io.neo9.ingress.access.config;

import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "extension")
public class AdditionalWatchersConfig {

	@Setter
	private UpdateIstioIngressSidecarConfig updateIstioIngressSidecar;

	@Setter
	private ExposerConfig exposer;

	@Setter
	private DefaultFilteringConfig defaultFiltering;

	@Setter
	private AwsIngressConfig awsIngress;

	public UpdateIstioIngressSidecarConfig updateIstioIngressSidecar() {
		return updateIstioIngressSidecar;
	}

	public ExposerConfig exposer() {
		return exposer;
	}

	public DefaultFilteringConfig defaultFiltering() {
		return defaultFiltering;
	}

	public AwsIngressConfig awsIngress() {
		return awsIngress;
	}

}
