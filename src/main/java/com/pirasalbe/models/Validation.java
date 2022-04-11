package com.pirasalbe.models;

/**
 * Validation result
 *
 * @author pirasalbe
 *
 */
public class Validation<T> {

	private boolean valid;

	private T reason;

	private Validation(boolean valid, T reason) {
		super();
		this.valid = valid;
		this.reason = reason;
	}

	public static <T> Validation<T> valid() {
		return new Validation<>(true, null);
	}

	public static <T> Validation<T> invalid(T reason) {
		return new Validation<>(false, reason);
	}

	public boolean isValid() {
		return valid;
	}

	public T getReason() {
		return reason;
	}

}
