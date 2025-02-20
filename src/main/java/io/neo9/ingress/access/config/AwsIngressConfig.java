package io.neo9.ingress.access.config;

import lombok.Data;

@Data
public class AwsIngressConfig {

	private boolean enabled;

	private String wafArn;

}
