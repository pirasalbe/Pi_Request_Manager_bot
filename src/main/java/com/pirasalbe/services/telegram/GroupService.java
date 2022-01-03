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

	public void insertIfNotExists(Long id) {
		Group group = null;

		// update
		Optional<Group> optional = repository.findById(id);
		if (optional.isEmpty()) {
			// add
			group = new Group();
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

}
