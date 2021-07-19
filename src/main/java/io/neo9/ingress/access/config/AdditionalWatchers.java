package io.neo9.ingress.access.config;

import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "extension")
public class AdditionalWatchers {

	@Setter
	private Boolean watchIngressAnnotations;

	public Boolean watchIngressAnnotations() {
		return watchIngressAnnotations;
	}
}
