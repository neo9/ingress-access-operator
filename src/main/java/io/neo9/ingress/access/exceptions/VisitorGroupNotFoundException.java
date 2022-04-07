package io.neo9.ingress.access.exceptions;

import lombok.Getter;

public class VisitorGroupNotFoundException extends RuntimeException {

	@Getter
	private final String visitorGroupName;

	public VisitorGroupNotFoundException(String visitorGroupName) {
		super(String.format("could not retrieve visitorGroup with name %s . May it does not exists ?",
				visitorGroupName));
		this.visitorGroupName = visitorGroupName;
	}

}
