package com.pirasalbe.models.database;

import javax.persistence.Column;
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
@Table(name = "ADMINS")
public class Admin {

	@Id
	private Long id;

	@Column(length = 128)
	private String name;

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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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
