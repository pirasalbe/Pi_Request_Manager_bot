package com.pirasalbe.models;

import java.time.LocalDateTime;

public class LastRequestInfo {

	private LocalDateTime date;

	private String otherTags;

	public LastRequestInfo(LocalDateTime date, String otherTags) {
		super();
		this.date = date;
		this.otherTags = otherTags;
	}

	public LocalDateTime getDate() {
		return date;
	}

	public String getOtherTags() {
		return otherTags;
	}

}
