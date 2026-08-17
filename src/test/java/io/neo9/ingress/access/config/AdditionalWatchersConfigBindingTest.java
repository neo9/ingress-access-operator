package io.neo9.ingress.access.config;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class AdditionalWatchersConfigBindingTest {

	@Test
	void bindsNginxWhitelistBackendFromExtensionProperties() {
		MapConfigurationPropertySource source = new MapConfigurationPropertySource();
		source.put("extension.nginx-whitelist.backend", "dual-write");

		AdditionalWatchersConfig config = new Binder(source)
				.bind("extension", Bindable.of(AdditionalWatchersConfig.class)).get();

		assertThat(config.nginxWhitelist().getBackend()).isEqualTo(NginxWhitelistConfig.BACKEND_DUAL_WRITE);
		assertThat(config.nginxWhitelist().isDualWrite()).isTrue();
		assertThat(config.nginxWhitelist().isCommunityAnnotation()).isTrue();
	}

	@Test
	void bindsNginxWhitelistBackendFromEnvironmentStyleKey() {
		SystemEnvironmentPropertySource source = new SystemEnvironmentPropertySource("testEnv",
				Map.of("EXTENSION_NGINX_WHITELIST_BACKEND", "f5-policy"));

		NginxWhitelistConfig config = new Binder(ConfigurationPropertySources.from(source))
				.bind("extension.nginx-whitelist", Bindable.of(NginxWhitelistConfig.class)).get();

		assertThat(config.getBackend()).isEqualTo(NginxWhitelistConfig.BACKEND_F5_POLICY);
		assertThat(config.isDualWrite()).isFalse();
		assertThat(config.isCommunityAnnotation()).isFalse();
		assertThat(config.isF5Policy()).isTrue();
	}

}
