package com.pirasalbe.service.telegram;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.pirasalbe.model.Pagination;
import com.pirasalbe.model.UserRole;
import com.pirasalbe.model.database.Admin;

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

	public void insert(Long id, UserRole role) {
		// TODO add or update
		LOGGER.info("New admin: [{}] with role [{}]", id, role);
	}

	public Pagination<Admin> list(int offset, int limit) {
		// TODO list item
		return new Pagination<>(12, new ArrayList<>());
	}

	public void delete(Long id) {
		// TODO remove
		LOGGER.info("Deleted admin: [{}]", id);
	}

}
