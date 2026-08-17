package io.neo9.ingress.access.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Fabric8 loads the client impl reflectively; keep this assertion so upgrades do not drop
 * the implementation jar from the classpath again.
 */
class Fabric8KubernetesClientImplClasspathTest {

	@Test
	void kubernetesClientImplIsOnClasspath() throws ClassNotFoundException {
		assertThat(Class.forName("io.fabric8.kubernetes.client.impl.KubernetesClientImpl")).isNotNull();
	}

}
