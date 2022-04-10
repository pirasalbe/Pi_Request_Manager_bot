package com.pirasalbe.models;

import com.pirasalbe.models.database.Request;

/**
 * Request to forward to the channels
 *
 * @author pirasalbe
 *
 */
public class ForwardRequest {

	private Request request;

	private String groupName;

	public ForwardRequest(Request request, String groupName) {
		super();
		this.request = request;
		this.groupName = groupName;
	}

	public Request getRequest() {
		return request;
	}

	public String getGroupName() {
		return groupName;
	}

}
