package com.pirasalbe.services.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.QPageRequest;
import org.springframework.stereotype.Component;

import com.pirasalbe.models.Pagination;
import com.pirasalbe.models.UserRole;
import com.pirasalbe.models.database.Admin;
import com.pirasalbe.repositories.AdminRepository;

/**
 * Service that manages the admin table
 *
 * @author pirasalbe
 *
 */
@Component
public class AdminService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AdminService.class);

	@Autowired
	private AdminRepository repository;

	public UserRole getAuthority(Long id) {
		UserRole role = UserRole.USER;

		if (repository.existsById(id)) {
			Admin admin = repository.getById(id);
			role = admin.getRole();
		}

		return role;
	}

	public void insert(Long id, UserRole role) {
		Admin admin = null;

		// update
		if (repository.existsById(id)) {
			admin = repository.getById(id);
		} else {
			// add
			admin = new Admin();
			admin.setId(id);
			admin.setRole(role);
		}

		repository.save(admin);
		LOGGER.info("New admin: [{}] with role [{}]", id, role);
	}

	public Pagination<Admin> list(int page, int size) {
		Pageable pageable = QPageRequest.of(page, size);
		Page<Admin> adminPage = repository.findAll(pageable);

		return new Pagination<>(adminPage.getTotalPages(), adminPage.getContent());
	}

	public void delete(Long id) {
		if (repository.existsById(id)) {
			repository.deleteById(id);
		}
		LOGGER.info("Deleted admin: [{}]", id);
	}

}
