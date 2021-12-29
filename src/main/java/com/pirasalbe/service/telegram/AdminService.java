package com.pirasalbe.service.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger LOGGER = LoggerFactory.getLogger(AdminService.class);

	public UserRole getAuthority(Long id) {
		// TODO implement DB
		// default value is USER
		return UserRole.SUPERADMIN;
	}

	public void insertAdmin(Long id, UserRole role) {
		LOGGER.info("New admin: [{}] with role [{}]", id, role);
	}

}
