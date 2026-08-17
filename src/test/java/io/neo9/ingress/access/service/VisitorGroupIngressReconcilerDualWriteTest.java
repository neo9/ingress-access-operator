package io.neo9.ingress.access.service;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.neo9.ingress.access.config.AdditionalWatchersConfig;
import io.neo9.ingress.access.config.MutationAnnotations;
import io.neo9.ingress.access.config.MutationLabels;
import io.neo9.ingress.access.config.NginxWhitelistConfig;
import io.neo9.ingress.access.customresources.VisitorGroup;
import io.neo9.ingress.access.customresources.external.nginx.NginxPolicy;
import io.neo9.ingress.access.customresources.spec.V1VisitorGroupSpec;
import io.neo9.ingress.access.customresources.spec.V1VisitorGroupSpecSources;
import io.neo9.ingress.access.repositories.IngressRepository;
import io.neo9.ingress.access.repositories.NginxPolicyRepository;
import io.neo9.ingress.access.repositories.ServiceRepository;
import io.neo9.ingress.access.repositories.VisitorGroupRepository;
import io.neo9.ingress.access.services.VisitorGroupIngressReconciler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitorGroupIngressReconcilerDualWriteTest {

	@Mock
	private VisitorGroupRepository visitorGroupRepository;

	@Mock
	private IngressRepository ingressRepository;

	@Mock
	private ServiceRepository serviceRepository;

	@Mock
	private NginxPolicyRepository nginxPolicyRepository;

	private VisitorGroupIngressReconciler reconciler;

	@BeforeEach
	void setUp() {
		AdditionalWatchersConfig config = new AdditionalWatchersConfig();
		NginxWhitelistConfig nginxWhitelist = new NginxWhitelistConfig();
		nginxWhitelist.setBackend(NginxWhitelistConfig.BACKEND_DUAL_WRITE);
		config.setNginxWhitelist(nginxWhitelist);

		reconciler = new VisitorGroupIngressReconciler(visitorGroupRepository, ingressRepository, serviceRepository,
				nginxPolicyRepository, config);
	}

	@Test
	void dualWritePatchesBothNginxAnnotationsInOneUpdate() {
		VisitorGroup visitorGroup = visitorGroup("neo9", "10.1.1.1/32");
		when(visitorGroupRepository.getVisitorGroupByName("neo9")).thenReturn(visitorGroup);
		when(nginxPolicyRepository.get("default", "ingress-access-demo")).thenReturn(null);

		Ingress ingress = new IngressBuilder().withNewMetadata().withName("demo").withNamespace("default")
				.addToLabels(MutationLabels.MUTABLE_LABEL_KEY, MutationLabels.MUTABLE_LABEL_VALUE)
				.addToAnnotations(MutationAnnotations.MUTABLE_INGRESS_VISITOR_GROUP_KEY, "neo9").endMetadata().build();

		reconciler.reconcile(ingress);

		verify(nginxPolicyRepository).createOrReplace(any(NginxPolicy.class));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, String>> annotationsCaptor = ArgumentCaptor.forClass(Map.class);
		verify(ingressRepository).patchWithAnnotations(eq(ingress), annotationsCaptor.capture());

		Map<String, String> annotations = annotationsCaptor.getValue();
		assertThat(annotations).containsEntry(MutationAnnotations.NGINX_ORG_POLICIES_ANNOTATION_KEY,
				"ingress-access-demo");
		assertThat(annotations).containsEntry(MutationAnnotations.NGINX_INGRESS_WHITELIST_ANNOTATION_KEY,
				"10.1.1.1/32");
	}

	private static VisitorGroup visitorGroup(String name, String cidr) {
		VisitorGroup visitorGroup = new VisitorGroup();
		visitorGroup.getMetadata().setName(name);
		visitorGroup.setSpec(V1VisitorGroupSpec.builder()
				.sources(List.of(V1VisitorGroupSpecSources.builder().name("office").cidr(cidr).build())).build());
		return visitorGroup;
	}

}
