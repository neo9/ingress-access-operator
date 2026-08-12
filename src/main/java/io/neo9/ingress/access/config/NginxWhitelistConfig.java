package io.neo9.ingress.access.config;

import lombok.Data;

@Data
public class NginxWhitelistConfig {

	public static final String BACKEND_F5_POLICY = "f5-policy";

	public static final String BACKEND_COMMUNITY_ANNOTATION = "community-annotation";

	public static final String BACKEND_DUAL_WRITE = "dual-write";

	/**
	 * Backend used to apply VisitorGroup CIDRs on Ingress resources.
	 * <ul>
	 * <li>{@code f5-policy}: F5 NGINX Ingress Controller OSS only</li>
	 * <li>{@code community-annotation}: kubernetes/ingress-nginx only</li>
	 * <li>{@code dual-write}: both — use during migration between controllers</li>
	 * </ul>
	 */
	private String backend = BACKEND_F5_POLICY;

	public boolean isF5Policy() {
		return BACKEND_F5_POLICY.equalsIgnoreCase(backend) || isDualWrite();
	}

	public boolean isCommunityAnnotation() {
		return BACKEND_COMMUNITY_ANNOTATION.equalsIgnoreCase(backend) || isDualWrite();
	}

	public boolean isDualWrite() {
		return BACKEND_DUAL_WRITE.equalsIgnoreCase(backend);
	}

}
