package com.pirasalbe.models.database;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * User Request Primary Key
 *
 * @author pirasalbe
 *
 */
@Embeddable
public class UserRequestPK implements Serializable {

	private static final long serialVersionUID = 3126637537858452476L;

	@Column(name = "REQUEST_MESSAGE_ID")
	private Long messageId;

	@Column(name = "REQUEST_GROUP_ID")
	private Long groupId;

	@Column(name = "USER_ID")
	private Long userId;

	public UserRequestPK() {
		super();
	}

	public UserRequestPK(Long messageId, Long groupId, Long userId) {
		super();
		this.messageId = messageId;
		this.groupId = groupId;
		this.userId = userId;
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

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(groupId, messageId, userId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		UserRequestPK other = (UserRequestPK) obj;
		return Objects.equals(groupId, other.groupId) && Objects.equals(messageId, other.messageId)
				&& Objects.equals(userId, other.userId);
	}

}
