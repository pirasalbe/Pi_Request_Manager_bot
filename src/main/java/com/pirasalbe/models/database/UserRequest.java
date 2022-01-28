package com.pirasalbe.models.database;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.pirasalbe.models.UserRequestRole;

/**
 * User Request
 *
 * @author pirasalbe
 *
 */
@Entity
@Table(name = "USER_REQUESTS")
public class UserRequest {

	@Id
	private UserRequestPK id;

	@Column(name = "GROUP_ID")
	private Long groupId;

	@Enumerated(EnumType.STRING)
	private UserRequestRole role;

	private LocalDateTime date;

	public UserRequest() {
		super();
	}

	public UserRequestPK getId() {
		return id;
	}

	public void setId(UserRequestPK id) {
		this.id = id;
	}

	public Long getGroupId() {
		return groupId;
	}

	public void setGroupId(Long groupId) {
		this.groupId = groupId;
	}

	public UserRequestRole getRole() {
		return role;
	}

	public void setRole(UserRequestRole role) {
		this.role = role;
	}

	public LocalDateTime getDate() {
		return date;
	}

	public void setDate(LocalDateTime date) {
		this.date = date;
	}

}
