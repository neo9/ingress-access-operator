package io.neo9.ingress.access.service;

import java.util.List;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.neo9.ingress.access.config.AdditionalWatchersConfig;
import io.neo9.ingress.access.config.ExposerConfig;
import io.neo9.ingress.access.repositories.IngressRepository;
import io.neo9.ingress.access.services.ServiceExposerReconciler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static io.neo9.ingress.access.config.MutationAnnotations.EXPOSE_INGRESS_HOSTNAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class ServiceExposerReconcilerTest {

	@Mock
	private IngressRepository ingressRepository;

	@Mock
	private AdditionalWatchersConfig additionalWatchersConfig;

	private ServiceExposerReconciler serviceExposerReconciler;

	@BeforeEach
	public void setUp() {
		ExposerConfig exposerConfig = new ExposerConfig();
		exposerConfig.setDomain("neokube.pro");
		exposerConfig.setHostnameTemplate("{{name}}.{{namespace}}.{{domain}}");
		exposerConfig.setTlsEnabledDetectionAnnotation(List.of("cert-manager.io/issuer", "cert-manager.io/cluster-issuer"));

		lenient().when(additionalWatchersConfig.exposer()).thenReturn(exposerConfig);

		serviceExposerReconciler = new ServiceExposerReconciler(ingressRepository, additionalWatchersConfig);
	}

	@Test
	public void shouldUseDefaultHostnameTemplate() {
		// given
		Service service = new ServiceBuilder()
				.withNewMetadata()
				.withNamespace("mynamespace")
				.withName("myname")
				.endMetadata()
				.build();

		// when
		String hostname = serviceExposerReconciler.generateHostname(service);

		// then
		assertThat(hostname).isEqualTo("myname.mynamespace.neokube.pro");
	}

	@Test
	public void shouldUseOverrideHostnameTemplate() {
		// given
		Service service = new ServiceBuilder()
				.withNewMetadata()
				.withNamespace("mynamespace")
				.withName("myname")
				.addToAnnotations(EXPOSE_INGRESS_HOSTNAME, "hello.{{domain}}")
				.endMetadata()
				.build();

		// when
		String hostname = serviceExposerReconciler.generateHostname(service);

		// then
		assertThat(hostname).isEqualTo("hello.neokube.pro");
	}

	@Test
	public void shouldNotEnableTlsByDefault() {
		// given
		Ingress ingress = new IngressBuilder()
				.withNewMetadata()
				.withNamespace("mynamespace")
				.withName("myname")
				.addToAnnotations(EXPOSE_INGRESS_HOSTNAME, "hello.{{domain}}")
				.endMetadata()
				.build();

		// when
		boolean shouldEnableTls = serviceExposerReconciler.shouldEnableTls(ingress.getMetadata().getAnnotations());

		// then
		assertThat(shouldEnableTls).isFalse();
	}

	@Test
	public void shouldNotEnableTlsIfAnnotationIsDetected() {
		// given
		Ingress ingress = new IngressBuilder()
				.withNewMetadata()
				.withNamespace("mynamespace")
				.withName("myname")
				.addToAnnotations(EXPOSE_INGRESS_HOSTNAME, "hello.{{domain}}")
				.addToAnnotations("cert-manager.io/cluster-issuer", "my-issuer")
				.endMetadata()
				.build();

		// when
		boolean shouldEnableTls = serviceExposerReconciler.shouldEnableTls(ingress.getMetadata().getAnnotations());

		// then
		assertThat(shouldEnableTls).isTrue();
	}
}
