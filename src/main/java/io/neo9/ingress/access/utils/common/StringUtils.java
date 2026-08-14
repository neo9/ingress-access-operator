package io.neo9.ingress.access.utils.common;

import java.util.Arrays;
import java.util.Map;

import lombok.experimental.UtilityClass;

import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.startsWith;

@UtilityClass
public class StringUtils {

	public static final String NEW_LINE = "\n";

	public static final String COLON = ":";

	public static final String EMPTY = "";

	public static final String COMMA = ",";

	public static final String SHARP = "#";

	/**
	 * Convert a raw block to map For example : a : aa b: bb Will be a map : [a: aa, b:
	 * bb]
	 */
	public static Map<String, String> rawBlockToMap(String raw) {
		if (isEmpty(raw)) {
			return Map.of();
		}
		return Arrays.asList(raw.split(NEW_LINE)).stream().filter(org.apache.commons.lang3.StringUtils::isNotBlank)
				.filter(s -> !startsWith(s.trim(), SHARP)).map(str -> str.split(COLON, 2))
				.collect(toMap(strSplit -> strSplit[0].trim(),
						strSplit -> (strSplit.length < 2 ? EMPTY : unquote(strSplit[1].trim()))));
	}

	public static String unquote(String s) {
		return s.replaceAll("^\"|\"$", "");
	}

	/**
	 * Whether {@code value} appears as an entry in a comma-separated list (trimmed).
	 */
	public static boolean commaSeparatedListContains(String list, String value) {
		if (isEmpty(value) || isEmpty(list)) {
			return false;
		}
		return Arrays.stream(list.split(COMMA)).map(String::trim)
				.filter(org.apache.commons.lang3.StringUtils::isNotBlank).anyMatch(value::equals);
	}

	/**
	 * Ensure {@code value} is present in a comma-separated list without removing other
	 * entries. Returns the original list when already present (no reordering).
	 */
	public static String ensureInCommaSeparatedList(String list, String value) {
		if (isEmpty(value)) {
			return isEmpty(list) ? EMPTY : list;
		}
		if (commaSeparatedListContains(list, value)) {
			return list;
		}
		if (isEmpty(list)) {
			return value;
		}
		return list.trim() + COMMA + value;
	}

}
