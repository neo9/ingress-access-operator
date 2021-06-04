package io.neo9.gatekeeper.exceptions;

public class VisitorGroupNotFoundException extends RuntimeException {

	public VisitorGroupNotFoundException(String visitorGroupName) {
		super(String.format("could not retrieve visitorGroup with name %s . May it does not exists ?", visitorGroupName));
	}

}
