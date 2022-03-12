package com.pirasalbe.models.request;

public enum RequestStatus {
	PENDING("pending", "⏳"), PAUSED("paused", "⏸"), RESOLVED("fulfilled", "⏳"), CANCELLED("cancelled", "✖️");

	private String description;
	private String icon;

	private RequestStatus(String description, String icon) {
		this.description = description;
		this.icon = icon;
	}

	public String getDescription() {
		return description;
	}

	public String getIcon() {
		return icon;
	}
}
