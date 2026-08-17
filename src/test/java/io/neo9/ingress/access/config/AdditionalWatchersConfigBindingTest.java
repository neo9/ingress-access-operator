package io.neo9.ingress.access.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class AdditionalWatchersConfigBindingTest {

	@Test
	void bindsNginxWhitelistBackendFromExtensionProperties() {
		MapConfigurationPropertySource source = new MapConfigurationPropertySource();
		source.put("extension.nginx-whitelist.backend", "dual-write");

		AdditionalWatchersConfig config = new Binder(source).bind("extension",
				Bindable.of(AdditionalWatchersConfig.class)).get();

		assertThat(config.nginxWhitelist().getBackend()).isEqualTo(NginxWhitelistConfig.BACKEND_DUAL_WRITE);
		assertThat(config.nginxWhitelist().isDualWrite()).isTrue();
		assertThat(config.nginxWhitelist().isCommunityAnnotation()).isTrue();
	}

	@Test
	void bindsNginxWhitelistBackendFromEnvironmentStyleKey() {
		MapConfigurationPropertySource source = new MapConfigurationPropertySource();
		source.put("EXTENSION_NGINX_WHITELIST_BACKEND", "dual-write");

		AdditionalWatchersConfig config = new Binder(source).bind("extension",
				Bindable.of(AdditionalWatchersConfig.class)).get();

		assertThat(config.nginxWhitelist().isDualWrite()).isTrue();
		assertThat(config.nginxWhitelist().isCommunityAnnotation()).isTrue();
	}

}
