package com.pirasalbe.models.database;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.pirasalbe.models.UserRole;

/**
 * Admin
 *
 * @author pirasalbe
 *
 */
@Entity
@Table(name = "ADMIN")
public class Admin {

	@Id
	private Long id;

	@Enumerated(EnumType.STRING)
	private UserRole role;

	private boolean backup;

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

	public boolean isBackup() {
		return backup;
	}

	public void setBackup(boolean backup) {
		this.backup = backup;
	}

}
