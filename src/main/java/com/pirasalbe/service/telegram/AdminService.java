package com.pirasalbe.service.telegram;

import org.springframework.stereotype.Component;

import com.pirasalbe.model.UserRole;

/**
 * Service that manages the admin table
 *
 * @author pirasalbe
 *
 */
@Component
public class AdminService {

	public UserRole getAuthority(Long id) {
		// TODO implement DB
		// default value is USER
		return UserRole.USER;
	}

}
