package com.pirasalbe.models;

import java.time.LocalDateTime;

import com.pirasalbe.models.database.Request;
import com.pirasalbe.utils.RequestUtils;

public class LastRequestInfo {

	public enum Type {
		REQUESTED, RECEIVED;
	}

	private Type type;

	private LocalDateTime date;

	private Request request;

	public LastRequestInfo(Type type, LocalDateTime date, Request request) {
		super();
		this.type = type;
		this.date = date;
		this.request = request;
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

	public Request getRequest() {
		return request;
	}

	public String getOtherTags() {
		return request.getOtherTags();
	}

	public boolean isEnglish() {
		return getOtherTags() == null || getOtherTags().equals(RequestUtils.OTHER_TAGS_ENGLISH);
	}

}
