package com.pirasalbe.services.telegram;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pirasalbe.models.database.Group;
import com.pirasalbe.repositories.GroupRepository;

/**
 * Service that manages the admin table
 *
 * @author pirasalbe
 *
 */
@Component
public class GroupService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GroupService.class);

	@Autowired
	private GroupRepository repository;

	public Optional<Group> findById(Long id) {
		return repository.findById(id);
	}

	public void insertIfNotExists(Long id) {
		// insert
		Optional<Group> optional = repository.findById(id);
		if (optional.isEmpty()) {
			// add
			Group group = new Group();
			group.setId(id);
			group.setRequestLimit(1);
			group.setAudiobooksDaysWait(15);
			group.setEnglishAudiobooksDaysWait(8);
			group.setAllowEbooks(true);
			group.setAllowAudiobooks(true);

			repository.save(group);
			LOGGER.info("New group: [{}]", id);
		}
	}

	public void deleteIfExists(Long id) {
		if (repository.existsById(id)) {
			repository.deleteById(id);
		}
		LOGGER.info("Deleted group: [{}]", id);
	}

	public boolean updateRequestLimit(Long id, int requestLimit) {
		boolean updated = false;

		// update
		Optional<Group> optional = repository.findById(id);
		boolean present = optional.isPresent();
		if (present) {
			// add
			Group group = optional.get();
			group.setRequestLimit(requestLimit);

			repository.save(group);
			updated = true;
			LOGGER.info("Update group: [{}] request limit [{}]", id, requestLimit);
		}

		return updated;
	}

}
