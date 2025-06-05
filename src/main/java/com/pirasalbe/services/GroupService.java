package com.pirasalbe.services;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pirasalbe.models.FormatAllowed;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.request.Source;
import com.pirasalbe.repositories.GroupRepository;
import com.pirasalbe.utils.RequestUtils;

/**
 * Service that manages the admin table
 *
 * @author pirasalbe
 *
 */
@Component
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class GroupService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GroupService.class);

	@Autowired
	private GroupRepository repository;

	@Autowired
	private RequestManagementService requestManagementService;

	public List<Group> findAll() {
		return repository.findAll();
	}

	public boolean existsById(Long id) {
		return repository.existsById(id);
	}

	public Optional<Group> findById(Long id) {
		return repository.findById(id);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void insertIfNotExists(Long id, String name) {
		// insert
		Optional<Group> optional = repository.findById(id);
		if (optional.isEmpty()) {
			// add
			Group group = new Group();
			group.setId(id);
			group.setName(name);
			group.setRequestLimit(1);
			group.setAudiobooksDaysWait(15);
			group.setEnglishAudiobooksDaysWait(8);
			group.setRepeatHoursWait(48);
			group.setAllowEbooks(true);
			group.setAllowAudiobooks(true);

			repository.save(group);
			LOGGER.info("New group: [{}]", id);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void deleteIfExists(Long id) {
		if (repository.existsById(id)) {
			requestManagementService.deleteGroupRequests(id);
			repository.deleteById(id);
		}
		LOGGER.info("Deleted group: [{}]", id);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
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

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
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

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
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

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public boolean updateRepeatHoursWait(Long id, int hoursWait) {
		boolean updated = false;

		// update
		Optional<Group> optional = repository.findById(id);
		boolean present = optional.isPresent();
		if (present) {
			// add
			Group group = optional.get();
			group.setRepeatHoursWait(hoursWait);

			repository.save(group);
			updated = true;
			LOGGER.info("Update group: [{}] Repeat hours wait [{}]", id, hoursWait);
		}

		return updated;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
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

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public boolean updateNoRepeat(Long id, List<Source> noRepeatSources) {
		boolean updated = false;

		// update
		Optional<Group> optional = repository.findById(id);
		boolean present = optional.isPresent();
		if (present) {
			// add
			Group group = optional.get();
			String noRepeat = RequestUtils.getNoRepeatSources(noRepeatSources);
			group.setNoRepeat(noRepeat);

			repository.save(group);
			updated = true;
			LOGGER.info("Update group: [{}] No repeat [{}]", id, noRepeat);
		}

		return updated;
	}

}
