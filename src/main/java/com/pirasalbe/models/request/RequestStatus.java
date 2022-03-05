package com.pirasalbe.models.request;

public enum RequestStatus {
	PENDING("pending"), PAUSED("paused"), RESOLVED("fulfilled"), CANCELLED("cancelled");

	private String description;

	private RequestStatus(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
