package com.pirasalbe.model.database;

import com.pirasalbe.model.UserRole;

/**
 * Admin
 *
 * @author pirasalbe
 *
 */
public class Admin {

	private Long id;

	private UserRole role;

	public Admin() {
		super();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public UserRole getRole() {
		return role;
	}

	public void setRole(UserRole role) {
		this.role = role;
	}

}
