package io.neo9.ingress.access.utils;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StringUtilsTest {

	@Test
	public void shouldParseRawBlockToMap() {
		// given
		String rawBlock = " kubernetes.io/ingress.class: nginx\n"
				+ "      test: http://$host/test\n"
				+ "  # this line is a comment";

		// when
		Map<String, String> stringStringMap = StringUtils.rawBlockToMap(rawBlock);

		// then
		assertThat(stringStringMap).hasSize(2);
		assertThat(stringStringMap)
				.extractingByKeys("kubernetes.io/ingress.class", "test")
				.contains("nginx", "http://$host/test");
	}

}
