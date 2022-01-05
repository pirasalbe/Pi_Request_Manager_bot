package com.pirasalbe.services.telegram;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pirasalbe.models.FormatAllowed;
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

	public boolean updateAudiobooksDaysWait(Long id, int daysWait) {
		boolean updated = false;

		// update
		Optional<Group> optional = repository.findById(id);
		boolean present = optional.isPresent();
		if (present) {
			// add
			Group group = optional.get();
			group.setAudiobooksDaysWait(daysWait);

			repository.save(group);
			updated = true;
			LOGGER.info("Update group: [{}] audiobooks days wait [{}]", id, daysWait);
		}

		return updated;
	}

	public boolean updateEnglishAudiobooksDaysWait(Long id, int daysWait) {
		boolean updated = false;

		// update
		Optional<Group> optional = repository.findById(id);
		boolean present = optional.isPresent();
		if (present) {
			// add
			Group group = optional.get();
			group.setEnglishAudiobooksDaysWait(daysWait);

			repository.save(group);
			updated = true;
			LOGGER.info("Update group: [{}] English audiobooks days wait [{}]", id, daysWait);
		}

		return updated;
	}

	public boolean updateAllow(Long id, FormatAllowed allowed) {
		boolean updated = false;

		// update
		Optional<Group> optional = repository.findById(id);
		boolean present = optional.isPresent();
		if (present) {
			// add
			Group group = optional.get();
			switch (allowed) {
			case AUDIOBOOKS:
				group.setAllowAudiobooks(true);
				group.setAllowEbooks(false);
				break;
			case EBOOKS:
				group.setAllowAudiobooks(false);
				group.setAllowEbooks(true);
				break;
			case BOTH:
			default:
				group.setAllowAudiobooks(true);
				group.setAllowEbooks(true);
				break;
			}

			repository.save(group);
			updated = true;
			LOGGER.info("Update group: [{}] allow [{}]", id, allowed);
		}

		return updated;
	}

}
