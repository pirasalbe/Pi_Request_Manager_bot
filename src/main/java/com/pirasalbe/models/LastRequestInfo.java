package com.pirasalbe.models;

import java.time.LocalDateTime;

public class LastRequestInfo {

	public enum Type {
		REQUESTED, RECEIVED;
	}

	private Type type;

	private LocalDateTime date;

	private String otherTags;

	public LastRequestInfo(Type type, LocalDateTime date, String otherTags) {
		super();
		this.type = type;
		this.date = date;
		this.otherTags = otherTags;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public LocalDateTime getDate() {
		return date;
	}

	public String getOtherTags() {
		return otherTags;
	}

}
