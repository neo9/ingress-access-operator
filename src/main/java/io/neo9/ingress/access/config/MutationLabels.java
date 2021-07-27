package io.neo9.ingress.access.config;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MutationLabels {

	public static final String MUTABLE_LABEL_KEY = "ingress.neo9.io/access-operator-enabled";

	public static final String MUTABLE_LABEL_VALUE = "true";

	public static final String MANAGED_BY_OPERATOR_KEY = "managed-by";

	public static final String MANAGED_BY_OPERATOR_VALUE = "ingress-access-operator";

	public static final String ISTIO_WATCH_NAMESPACE_LABEL_KEY = "istio-injection";

	public static final String ISTIO_WATCH_NAMESPACE_LABEL_VALUE = "true";
}