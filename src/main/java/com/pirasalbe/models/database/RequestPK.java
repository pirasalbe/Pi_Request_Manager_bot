package com.pirasalbe.models.database;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Request Primary Key
 *
 * @author pirasalbe
 *
 */
@Embeddable
public class RequestPK implements Serializable {

	private static final long serialVersionUID = 3126637537858452476L;

	@Column(name = "MESSAGE_ID")
	private Long messageId;

	@Column(name = "GROUP_ID")
	private Long groupId;

	public RequestPK() {
		super();
	}

	public RequestPK(Long messageId, Long groupId) {
		super();
		this.messageId = messageId;
		this.groupId = groupId;
	}

	public Long getMessageId() {
		return messageId;
	}

	public void setMessageId(Long messageId) {
		this.messageId = messageId;
	}

	public Long getGroupId() {
		return groupId;
	}

	public void setGroupId(Long groupId) {
		this.groupId = groupId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(groupId, messageId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		RequestPK other = (RequestPK) obj;
		return Objects.equals(groupId, other.groupId) && Objects.equals(messageId, other.messageId);
	}

	@Override
	public String toString() {
		return "RequestPK [messageId=" + messageId + ", groupId=" + groupId + "]";
	}

}
