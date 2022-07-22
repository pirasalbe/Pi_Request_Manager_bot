package com.pirasalbe.models;

/**
 * Result of a request
 *
 * @author pirasalbe
 *
 */
public class RequestResult {
	public enum Result {
		NEW, REPEATED_REQUEST, REQUEST_REPEATED_TOO_EARLY, CANNOT_REPEAT_REQUEST, DIFFERENT_GROUP;
	}

	private Result result;

	private String reason;

	public RequestResult(Result result) {
		this.result = result;
	}

	public RequestResult(Result result, String reason) {
		this(result);
		this.reason = reason;
	}

	public Result getResult() {
		return result;
	}

	public String getReason() {
		return reason;
	}

}
