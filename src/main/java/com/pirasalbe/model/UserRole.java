package com.pirasalbe.model;

import java.util.ArrayList;
import java.util.List;

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

	public static String getRoles() {
		List<String> names = new ArrayList<>();
		for (UserRole role : UserRole.values()) {
			if (role.authorityLevel > 0) {
				names.add(role.name());
			}
		}

		return String.join("/", names);
	}
}
