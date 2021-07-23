package io.neo9.ingress.access.config;

import java.util.List;

import lombok.Data;

@Data
public class UpdateIstioIngressSidecarConfig {

	private boolean enabled;

	private String ingressNamespace;

	private List<String> additionalEgressRulesEntries;

}
