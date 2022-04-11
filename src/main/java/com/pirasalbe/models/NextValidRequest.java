package com.pirasalbe.models;

import java.time.LocalDateTime;

/**
 * Information about the next valid request
 *
 * @author pirasalbe
 *
 */
public class NextValidRequest {

	private LocalDateTime nextRequest;

	private String message;

	public NextValidRequest(LocalDateTime nextRequest, String message) {
		super();
		this.nextRequest = nextRequest;
		this.message = message;
	}

	public NextValidRequest(String messsage) {
		this(null, messsage);
	}

	public LocalDateTime getNextRequest() {
		return nextRequest;
	}

	public String getMessage() {
		return message;
	}

}
