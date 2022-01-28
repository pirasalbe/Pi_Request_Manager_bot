package com.pirasalbe.models;

/**
 * Validation result
 *
 * @author pirasalbe
 *
 */
public class Validation {

	private boolean valid;

	private String reason;

	private Validation(boolean valid, String reason) {
		super();
		this.valid = valid;
		this.reason = reason;
	}

	public static Validation valid() {
		return new Validation(true, null);
	}

	public static Validation invalid(String reason) {
		return new Validation(false, reason);
	}

	public boolean isValid() {
		return valid;
	}

	public String getReason() {
		return reason;
	}

}
