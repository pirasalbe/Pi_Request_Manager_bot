package com.pirasalbe.models;

import com.pirasalbe.models.database.RequestPK;

/**
 * Object with all data to sync a request in channel
 *
 * @author pirasalbe
 *
 */
public class SyncRequest {

	private Long channelId;

	private RequestPK requestId;

	private String groupName;

	public SyncRequest(Long channelId, RequestPK requestId, String groupName) {
		super();
		this.channelId = channelId;
		this.requestId = requestId;
		this.groupName = groupName;
	}

	public Long getChannelId() {
		return channelId;
	}

	public RequestPK getRequestId() {
		return requestId;
	}

	public String getGroupName() {
		return groupName;
	}

}
