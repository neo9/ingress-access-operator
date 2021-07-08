package io.neo9.ingress.access.config;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MutationAnnotations {

	public static final String MUTABLE_INGRESS_VISITOR_GROUP_KEY = "ingress.neo9.io/allowed-visitors";

	public static final String NGINX_INGRESS_WHITELIST_ANNOTATION_KEY = "nginx.ingress.kubernetes.io/whitelist-source-range";

}
