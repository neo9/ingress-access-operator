package io.neo9.ingress.access.config;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MutationAnnotations {

	public static final String MUTABLE_INGRESS_VISITOR_GROUP_KEY = "ingress.neo9.io/allowed-visitors";

	public static final String NGINX_INGRESS_WHITELIST_ANNOTATION_KEY = "nginx.ingress.kubernetes.io/whitelist-source-range";

	public static final String EXPOSE_INGRESS_ADDITIONAL_LABELS = "ingress.neo9.io/expose-labels";

	public static final String EXPOSE_INGRESS_ADDITIONAL_ANNOTATIONS = "ingress.neo9.io/expose-annotations";

	public static final String EXPOSE_INGRESS_HOSTNAME = "ingress.neo9.io/expose-hostname";

	public static final String FILTERING_MANAGED_BY_OPERATOR_KEY = "filtering-managed-by";

	public static final String FORECASTLE_EXPOSE = "forecastle.stakater.com/expose";

	public static final String FORECASTLE_NETWORK_RESTRICTED = "forecastle.stakater.com/network-restricted";

}
