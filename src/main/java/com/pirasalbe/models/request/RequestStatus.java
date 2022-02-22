package com.pirasalbe.models.request;

public enum RequestStatus {
	NEW("pending"), RESOLVED("fulfilled"), CANCELLED("cancelled");

	private String description;

	private RequestStatus(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
