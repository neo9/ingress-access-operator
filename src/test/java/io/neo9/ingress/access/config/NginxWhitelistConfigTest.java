package io.neo9.ingress.access.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NginxWhitelistConfigTest {

	@Test
	void defaultsToF5Policy() {
		NginxWhitelistConfig config = new NginxWhitelistConfig();
		assertThat(config.isF5Policy()).isTrue();
		assertThat(config.isCommunityAnnotation()).isFalse();
		assertThat(config.isDualWrite()).isFalse();
	}

	@Test
	void dualWriteEnablesBothForMigration() {
		NginxWhitelistConfig config = new NginxWhitelistConfig();
		config.setBackend("dual-write");
		assertThat(config.isDualWrite()).isTrue();
		assertThat(config.isF5Policy()).isTrue();
		assertThat(config.isCommunityAnnotation()).isTrue();
	}

	@Test
	void communityOnly() {
		NginxWhitelistConfig config = new NginxWhitelistConfig();
		config.setBackend("community-annotation");
		assertThat(config.isF5Policy()).isFalse();
		assertThat(config.isCommunityAnnotation()).isTrue();
		assertThat(config.isDualWrite()).isFalse();
	}

}
