package io.neo9.gatekeeper.exceptions;

import lombok.Data;

@Data
public class VisitorGroupNotFoundException extends RuntimeException {

	private String visitorGroupName;

	public VisitorGroupNotFoundException(String visitorGroupName) {
		super(String.format("could not retrieve visitorGroup with name %s . May it does not exists ?", visitorGroupName));
		this.visitorGroupName = visitorGroupName;
	}

}
