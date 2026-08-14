package io.neo9.ingress.access.utils.common;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StringUtilsTest {

	@Test
	public void shouldParseRawBlockToMap() {
		// given
		String rawBlock = " kubernetes.io/ingress.class: nginx\n" + "      test: http://$host/test\n"
				+ "  # this line is a comment";

		// when
		Map<String, String> stringStringMap = StringUtils.rawBlockToMap(rawBlock);

		// then
		assertThat(stringStringMap).hasSize(2);
		assertThat(stringStringMap).extractingByKeys("kubernetes.io/ingress.class", "test").contains("nginx",
				"http://$host/test");
	}

	@Test
	void ensureInCommaSeparatedListKeepsExistingPolicies() {
		assertThat(StringUtils.ensureInCommaSeparatedList("cors", "ingress-access-foo"))
				.isEqualTo("cors,ingress-access-foo");
		assertThat(StringUtils.ensureInCommaSeparatedList("cors,ingress-access-foo", "ingress-access-foo"))
				.isEqualTo("cors,ingress-access-foo");
		assertThat(StringUtils.ensureInCommaSeparatedList("cors, ingress-access-foo", "ingress-access-foo"))
				.isEqualTo("cors, ingress-access-foo");
		assertThat(StringUtils.ensureInCommaSeparatedList("", "ingress-access-foo")).isEqualTo("ingress-access-foo");
		assertThat(StringUtils.ensureInCommaSeparatedList(null, "ingress-access-foo")).isEqualTo("ingress-access-foo");
	}

	@Test
	void commaSeparatedListContainsTrimsEntries() {
		assertThat(StringUtils.commaSeparatedListContains("cors, ingress-access-foo", "ingress-access-foo")).isTrue();
		assertThat(StringUtils.commaSeparatedListContains("cors", "ingress-access-foo")).isFalse();
		assertThat(StringUtils.commaSeparatedListContains("", "ingress-access-foo")).isFalse();
	}

}
