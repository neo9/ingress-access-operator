package io.neo9.ingress.access.config;

import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "extension")
public class AdditionalWatchersConfig {

	@Setter
	private WatchIngressAnnotationsConfig watchIngressAnnotations;

	@Setter
	private UpdateIstioIngressSidecarConfig updateIstioIngressSidecar;

	public WatchIngressAnnotationsConfig watchIngressAnnotations() {
		return watchIngressAnnotations;
	}

	public UpdateIstioIngressSidecarConfig updateIstioIngressSidecar() {
		return updateIstioIngressSidecar;
	}

}
