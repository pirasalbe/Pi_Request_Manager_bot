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

	@Column(name = "REQUEST_ID")
	private Long requestId;

	@Column(name = "USER_ID")
	private Long userId;

	public UserRequestPK() {
		super();
	}

	public UserRequestPK(Long requestId, Long userId) {
		super();
		this.requestId = requestId;
		this.userId = userId;
	}

	public Long getRequestId() {
		return requestId;
	}

	public void setRequestId(Long requestId) {
		this.requestId = requestId;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(requestId, userId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		UserRequestPK other = (UserRequestPK) obj;
		return Objects.equals(requestId, other.requestId) && Objects.equals(userId, other.userId);
	}

}
