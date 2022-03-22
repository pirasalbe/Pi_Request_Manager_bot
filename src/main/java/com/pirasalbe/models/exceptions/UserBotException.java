package com.pirasalbe.models.exceptions;

/**
 * Exception for userbot methods
 *
 * @author pirasalbe
 *
 */
public class UserBotException extends RuntimeException {

	private static final long serialVersionUID = -8665099436523201731L;

	public UserBotException() {
		super();
	}

	public UserBotException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public UserBotException(String message, Throwable cause) {
		super(message, cause);
	}

	public UserBotException(String message) {
		super(message);
	}

	public UserBotException(Throwable cause) {
		super(cause);
	}

}
