package io.neo9.ingress.access.service;

import java.util.List;
import java.util.Map;

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.neo9.ingress.access.config.AdditionalWatchersConfig;
import io.neo9.ingress.access.config.MutationAnnotations;
import io.neo9.ingress.access.customresources.VisitorGroup;
import io.neo9.ingress.access.customresources.spec.V1VisitorGroupSpec;
import io.neo9.ingress.access.customresources.spec.V1VisitorGroupSpecSources;
import io.neo9.ingress.access.exceptions.VisitorGroupNotFoundException;
import io.neo9.ingress.access.repositories.IngressRepository;
import io.neo9.ingress.access.repositories.VisitorGroupRepository;
import io.neo9.ingress.access.services.VisitorGroupIngressReconciler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class VisitorGroupServiceExposerReconcilerTest {

	private final static VisitorGroup visitorGroup1;

	private final static VisitorGroup visitorGroup2;

	private final static VisitorGroup visitorGroup1bis;

	private final static String visitorGroup1Ip1 = "10.0.1.1/32";

	private final static String visitorGroup1Ip2 = "10.0.1.2/32";

	private final static String visitorGroup1Ips = "10.0.1.1/32,10.0.1.2/32";

	private final static String visitorGroup2Ip1 = "10.0.2.1/32";

	private final static String visitorGroup2Ip2 = "10.0.2.2/32";

	private final static String visitorGroup2Ips = "10.0.2.1/32,10.0.2.2/32";

	private final static String visitorGroup1bisIp1 = "10.0.1.2/32";

	private final static String visitorGroup1bisIp2 = "10.0.1.3/32";

	private final static String visitorGroup1bisIps = "10.0.1.2/32,10.0.1.3/32";

	static {
		V1VisitorGroupSpec visitorGroup1Spec = V1VisitorGroupSpec
				.builder()
				.sources(List.of(
								V1VisitorGroupSpecSources.builder().name("ip1").cidr(visitorGroup1Ip1).build(),
								V1VisitorGroupSpecSources.builder().name("ip2").cidr(visitorGroup1Ip2).build()
						)
				)
				.build();
		visitorGroup1 = new VisitorGroup();
		visitorGroup1.setSpec(visitorGroup1Spec);

		V1VisitorGroupSpec visitorGroup2Spec = V1VisitorGroupSpec
				.builder()
				.sources(List.of(
								V1VisitorGroupSpecSources.builder().name("ip1").cidr(visitorGroup2Ip1).build(),
								V1VisitorGroupSpecSources.builder().name("ip2").cidr(visitorGroup2Ip2).build()
						)
				)
				.build();
		visitorGroup2 = new VisitorGroup();
		visitorGroup2.setSpec(visitorGroup2Spec);

		V1VisitorGroupSpec visitorGroup1bisSpec = V1VisitorGroupSpec
				.builder()
				.sources(List.of(
								V1VisitorGroupSpecSources.builder().name("ip1").cidr(visitorGroup1bisIp1).build(),
								V1VisitorGroupSpecSources.builder().name("ip2").cidr(visitorGroup1bisIp2).build()
						)
				)
				.build();
		visitorGroup1bis = new VisitorGroup();
		visitorGroup1bis.setSpec(visitorGroup1bisSpec);
	}

	@Mock
	private VisitorGroupRepository visitorGroupRepository;

	@Mock
	private IngressRepository ingressRepository;

	private VisitorGroupIngressReconciler visitorGroupIngressReconciler;

	@BeforeEach
	public void setUp() {
		lenient().when(visitorGroupRepository.getVisitorGroupByName("vg1")).thenReturn(visitorGroup1);
		lenient().when(visitorGroupRepository.getVisitorGroupByName("vg2")).thenReturn(visitorGroup2);
		lenient().when(visitorGroupRepository.getVisitorGroupByName("vg1bis")).thenReturn(visitorGroup1bis);
		lenient().when(visitorGroupRepository.getVisitorGroupByName("vgUndefined")).thenThrow(new VisitorGroupNotFoundException("vgUndefined"));

		visitorGroupIngressReconciler = new VisitorGroupIngressReconciler(visitorGroupRepository, ingressRepository, new AdditionalWatchersConfig());
	}

	@Test
	public void shouldWellComputeWhitelistForOneGroup() {
		// given
		Ingress ingress = new IngressBuilder()
				.withNewMetadata()
				.withName("test")
				.withAnnotations(Map.of(MutationAnnotations.MUTABLE_INGRESS_VISITOR_GROUP_KEY, "vg1"))
				.endMetadata()
				.build();

		// when
		String cidrListAsString = visitorGroupIngressReconciler.getCidrListAsString(ingress);

		// then
		assertThat(cidrListAsString).isEqualTo(visitorGroup1Ips);
	}

	@Test
	public void shouldWellComputeWhitelistForTwoGroup() {
		// given
		Ingress ingress = new IngressBuilder()
				.withNewMetadata()
				.withName("test")
				.withAnnotations(Map.of(MutationAnnotations.MUTABLE_INGRESS_VISITOR_GROUP_KEY, "vg1,vg2"))
				.endMetadata()
				.build();

		// when
		String cidrListAsString = visitorGroupIngressReconciler.getCidrListAsString(ingress);

		// then
		assertThat(cidrListAsString).isEqualTo(visitorGroup1Ips + "," + visitorGroup2Ips);
	}

	@Test
	public void shouldWellComputeWhitelistForTwoGroupWithWhitespaceInAnnotation() {
		// given
		Ingress ingress = new IngressBuilder()
				.withNewMetadata()
				.withName("test")
				.withAnnotations(Map.of(MutationAnnotations.MUTABLE_INGRESS_VISITOR_GROUP_KEY, "vg1 , vg2"))
				.endMetadata()
				.build();

		// when
		String cidrListAsString = visitorGroupIngressReconciler.getCidrListAsString(ingress);

		// then
		assertThat(cidrListAsString).isEqualTo(visitorGroup1Ips + "," + visitorGroup2Ips);
	}

	@Test
	public void shouldWellComputeWhitelistForTwoGroupWithCommaTypo() {
		// given
		Ingress ingress = new IngressBuilder()
				.withNewMetadata()
				.withName("test")
				.withAnnotations(Map.of(MutationAnnotations.MUTABLE_INGRESS_VISITOR_GROUP_KEY, "vg1,,vg2,"))
				.endMetadata()
				.build();

		// when
		String cidrListAsString = visitorGroupIngressReconciler.getCidrListAsString(ingress);

		// then
		assertThat(cidrListAsString).isEqualTo(visitorGroup1Ips + "," + visitorGroup2Ips);
	}


	@Test
	public void shouldWellComputeWhitelistForTwoGroupWithCommonRange() {
		// given
		Ingress ingress = new IngressBuilder()
				.withNewMetadata()
				.withName("test")
				.withAnnotations(Map.of(MutationAnnotations.MUTABLE_INGRESS_VISITOR_GROUP_KEY, "vg1,vg1bis"))
				.endMetadata()
				.build();

		// when
		String cidrListAsString = visitorGroupIngressReconciler.getCidrListAsString(ingress);

		// then
		assertThat(cidrListAsString).isEqualTo(visitorGroup1Ips + "," + visitorGroup1bisIp2);
	}

	@Test
	public void shouldUseDefaultConfigurationIfThereIsNoGroups() {
		// given
		Ingress ingress = new IngressBuilder()
				.withNewMetadata()
				.withName("test")
				.withAnnotations(Map.of(MutationAnnotations.MUTABLE_INGRESS_VISITOR_GROUP_KEY, ""))
				.endMetadata()
				.build();

		// when
		String cidrListAsString = visitorGroupIngressReconciler.getCidrListAsString(ingress);

		// then
		assertThat(cidrListAsString).isEqualTo("0.0.0.0/0");
	}

	@Test
	public void shouldUseDefaultConfigurationIfThereIsNoAnnotation() {
		// given
		Ingress ingress = new IngressBuilder()
				.withNewMetadata()
				.withName("test")
				.endMetadata()
				.build();

		// when
		String cidrListAsString = visitorGroupIngressReconciler.getCidrListAsString(ingress);

		// then
		assertThat(cidrListAsString).isEqualTo("0.0.0.0/0");
	}

	@Test
	public void shouldPanicOnUnknownVisitorGroup() {
		// given
		Ingress ingress = new IngressBuilder()
				.withNewMetadata()
				.withName("test")
				.withAnnotations(Map.of(MutationAnnotations.MUTABLE_INGRESS_VISITOR_GROUP_KEY, "vgUndefined"))
				.endMetadata()
				.build();

		// when / then
		assertThatThrownBy(
				() -> visitorGroupIngressReconciler.getCidrListAsString(ingress)
		).isInstanceOf(VisitorGroupNotFoundException.class);
	}
}
