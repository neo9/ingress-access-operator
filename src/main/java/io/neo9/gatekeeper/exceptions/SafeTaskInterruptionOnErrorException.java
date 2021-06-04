package io.neo9.gatekeeper.exceptions;

public class SafeTaskInterruptionOnErrorException extends RuntimeException {

	public SafeTaskInterruptionOnErrorException(String message, Throwable cause) {
		super(message, cause);
	}

}
