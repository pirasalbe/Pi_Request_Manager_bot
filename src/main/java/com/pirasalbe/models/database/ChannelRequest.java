package com.pirasalbe.models.database;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Channel Request
 *
 * @author pirasalbe
 *
 */
@Entity
@Table(name = "CHANNEL_REQUESTS")
public class ChannelRequest {

	@Id
	private ChannelRequestPK id;

	@Column(name = "REQUEST_MESSAGE_ID")
	private Long requestMessageId;

	@Column(name = "REQUEST_GROUP_ID")
	private Long requestGroupId;

	public ChannelRequest() {
		super();
	}

	public ChannelRequestPK getId() {
		return id;
	}

	public void setId(ChannelRequestPK id) {
		this.id = id;
	}

	public Long getRequestMessageId() {
		return requestMessageId;
	}

	public void setRequestMessageId(Long requestMessageId) {
		this.requestMessageId = requestMessageId;
	}

	public Long getRequestGroupId() {
		return requestGroupId;
	}

	public void setRequestGroupId(Long requestGroupId) {
		this.requestGroupId = requestGroupId;
	}

}
