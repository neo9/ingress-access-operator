package io.neo9.ingress.access.exceptions;

import lombok.Getter;

import static io.neo9.ingress.access.config.MutationLabels.MANAGED_BY_OPERATOR_KEY;
import static io.neo9.ingress.access.config.MutationLabels.MANAGED_BY_OPERATOR_VALUE;

public class ResourceNotManagedByOperatorException extends RuntimeException {

	@Getter
	private String resourceNamespaceName;

	public ResourceNotManagedByOperatorException(String resourceNamespaceName) {
		super(String.format("should not manipulate resource %s . It sounds not managed by operator (labels not detected %s=%s)", resourceNamespaceName, MANAGED_BY_OPERATOR_KEY, MANAGED_BY_OPERATOR_VALUE));
		this.resourceNamespaceName = resourceNamespaceName;
	}

}
