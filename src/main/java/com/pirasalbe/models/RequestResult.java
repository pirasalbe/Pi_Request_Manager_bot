package com.pirasalbe.models;

/**
 * Result of a request
 *
 * @author pirasalbe
 *
 */
public class RequestResult {
	public enum Result {
		NEW(true), REPEATED_REQUEST(true), REQUEST_REPEATED_TOO_EARLY, CANNOT_REPEAT_REQUEST, DIFFERENT_GROUP;

		private boolean ok;

		private Result(boolean ok) {
			this.ok = ok;
		}

		private Result() {
			this(false);
		}

		public boolean isOk() {
			return ok;
		}
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
