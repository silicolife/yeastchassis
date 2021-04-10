package com.silicolife.yeastchassis.env.exception;

public class NonExistingReactionException extends Exception {

	private static final long serialVersionUID = 1L;

	public NonExistingReactionException(String reactionId) {
		super("Invalid reaction id: " + reactionId);
	}

}
