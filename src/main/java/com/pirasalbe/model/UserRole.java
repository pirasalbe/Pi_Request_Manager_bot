package com.pirasalbe.model;

/**
 * Role that a user can have
 *
 * @author pirasalbe
 *
 */
public enum UserRole {
	USER(0), CONTRIBUTOR(1), MANAGER(2), SUPERADMIN(3);

	private int authorityLevel;

	private UserRole(int authorityLevel) {
		this.authorityLevel = authorityLevel;
	}

	public int getAuthorityLevel() {
		return authorityLevel;
	}
}
