package com.pirasalbe.models.database;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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

	private String role;

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

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public LocalDateTime getDate() {
		return date;
	}

	public void setDate(LocalDateTime date) {
		this.date = date;
	}

}
