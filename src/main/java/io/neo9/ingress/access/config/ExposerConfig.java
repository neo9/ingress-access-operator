package io.neo9.ingress.access.config;

import java.util.List;

import lombok.Data;

@Data
public class ExposerConfig {

	private boolean enabled;

	private String domain;

	private String hostnameTemplate;

	private List<String> tlsEnabledDetectionAnnotation;
}
