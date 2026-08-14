package io.neo9.ingress.access.customresources.external.nginx;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class NginxPolicyReflectionTest {

	@Test
	void canInstantiateViaDefaultConstructor() throws Exception {
		assertNotNull(NginxPolicy.class.getDeclaredConstructor().newInstance());
	}

}
