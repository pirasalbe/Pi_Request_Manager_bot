package com.pirasalbe.services;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.querydsl.QPageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class AdminService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AdminService.class);

	@Autowired
	private AdminRepository repository;

	public UserRole getAuthority(Long id) {
		UserRole role = UserRole.USER;

		Optional<Admin> optional = repository.findById(id);
		if (optional.isPresent()) {
			role = optional.get().getRole();
		}

		return role;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void insertUpdate(Long id, String name, UserRole role) {
		Admin admin = null;

		// update
		Optional<Admin> optional = repository.findById(id);
		if (optional.isPresent()) {
			admin = optional.get();
		} else {
			// add
			admin = new Admin();
			admin.setId(id);
		}

		admin.setName(name);
		admin.setRole(role);

		repository.save(admin);
		LOGGER.info("New admin: [{}] ({}) with role [{}]", name, id, role);
	}

	public Pagination<Admin> list(int page, int size) {
		Pageable pageable = QPageRequest.of(page, size);
		Page<Admin> adminPage = repository.findAll(pageable);

		return new Pagination<>(adminPage.getTotalPages(), adminPage.getContent());
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void deleteIfExists(Long id) {
		if (repository.existsById(id)) {
			repository.deleteById(id);
		}
		LOGGER.info("Deleted admin: [{}]", id);
	}

}
