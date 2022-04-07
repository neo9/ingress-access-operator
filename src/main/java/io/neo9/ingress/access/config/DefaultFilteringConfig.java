package io.neo9.ingress.access.config;

import lombok.Data;

import java.util.List;

@Data
public class DefaultFilteringConfig {

	private boolean enabled;

	private List<String> categories;

}
