package io.neo9.ingress.access.config;

import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class MutationLabels {

	public static final String LEGACY_MUTABLE_LABEL_KEY = "ingress.neo9.io/access-operator-enabled";

	public static final String MUTABLE_LABEL_KEY = "ingress.neo9.io/access-filtered";

	public static final List<String> MUTABLE_FILTERING_LABELS = List.of(LEGACY_MUTABLE_LABEL_KEY, MUTABLE_LABEL_KEY);

	public static final String MUTABLE_LABEL_VALUE = "true";

	public static final String MANAGED_BY_OPERATOR_KEY = "managed-by";

	public static final String MANAGED_BY_OPERATOR_VALUE = "ingress-access-operator";

	public static final String ISTIO_WATCH_NAMESPACE_LABEL_KEY = "istio-injection";

	public static final String ISTIO_WATCH_NAMESPACE_LABEL_VALUE = "enabled";

	public static final String EXPOSE_LABEL_KEY = "ingress.neo9.io/expose";

	public static final String EXPOSE_LABEL_VALUE = "true";
}
