package com.pirasalbe.models.database;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Channel Request Primary Key
 *
 * @author pirasalbe
 *
 */
@Embeddable
public class ChannelRequestPK implements Serializable {

	private static final long serialVersionUID = 3126637537858452476L;

	@Column(name = "CHANNEL_ID")
	private Long channelId;

	@Column(name = "MESSAGE_ID")
	private Long messageId;

	public ChannelRequestPK() {
		super();
	}

	public ChannelRequestPK(Long channelId, Long messageId) {
		super();
		this.messageId = messageId;
		this.channelId = channelId;
	}

	public Long getMessageId() {
		return messageId;
	}

	public void setMessageId(Long messageId) {
		this.messageId = messageId;
	}

	public Long getChannelId() {
		return channelId;
	}

	public void setChannelId(Long channelId) {
		this.channelId = channelId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(channelId, messageId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		ChannelRequestPK other = (ChannelRequestPK) obj;
		return Objects.equals(channelId, other.channelId) && Objects.equals(messageId, other.messageId);
	}

}
