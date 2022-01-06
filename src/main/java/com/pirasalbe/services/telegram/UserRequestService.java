package com.pirasalbe.services.telegram;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pirasalbe.models.Format;
import com.pirasalbe.models.LastRequestInfo;
import com.pirasalbe.models.Source;
import com.pirasalbe.models.UserRequestRole;
import com.pirasalbe.models.Validation;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.database.Request;
import com.pirasalbe.models.database.UserRequest;
import com.pirasalbe.models.database.UserRequestPK;
import com.pirasalbe.repositories.UserRequestRepository;
import com.pirasalbe.utils.DateUtils;

/**
 * Service that manages the user request table
 *
 * @author pirasalbe
 *
 */
@Component
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class UserRequestService {

	@Autowired
	private UserRequestRepository repository;

	@Autowired
	private RequestService requestService;

	public Validation canRequest(Group group, Long userId, Format format) {
		Validation validation = isFormatAllowed(group.isAllowEbooks(), group.isAllowAudiobooks(), format);

		// check format allowed
		if (validation.isValid()) {

			// check request limit
			long requests = repository.countUserRequestsInGroupOfToday(userId, DateUtils.getToday());
			if (requests < group.getRequestLimit()) {

				// check audiobook limit
				if (format.equals(Format.AUDIOBOOK)) {
					LastRequestInfo lastRequestInfo = repository.getLastAudiobookRequestOfUserInGroup(userId);

					validation = isValidAudiobookRequest(group.getAudiobooksDaysWait(),
							group.getEnglishAudiobooksDaysWait(), lastRequestInfo);
				}

			} else {
				validation = Validation.invalid("You’re only allowed to request up to " + group.getRequestLimit()
						+ " book" + (group.getRequestLimit() > 1 ? "s" : "") + " per day. Come back again tomorrow.");
			}
		}

		return validation;
	}

	private Validation isFormatAllowed(boolean ebooksAllowed, boolean audiobooksAllowed, Format format) {
		Validation validation = Validation.valid();

		if (!ebooksAllowed && format.equals(Format.EBOOK)) {
			validation = Validation.invalid("Ebooks are not allowed");
		} else if (!audiobooksAllowed && format.equals(Format.AUDIOBOOK)) {
			validation = Validation.invalid("Audiobooks are not allowed");
		}

		return validation;
	}

	private Validation isValidAudiobookRequest(Integer audiobooksDaysWait, Integer englishAudiobooksDaysWait,
			LastRequestInfo lastRequestInfo) {
		Validation validation = Validation.valid();

		if (lastRequestInfo != null) {
			// if null -> language was English
			int days = lastRequestInfo.getOtherTags() == null ? englishAudiobooksDaysWait : audiobooksDaysWait;

			// last audiobook
			LocalDateTime nextValidRequest = lastRequestInfo.getDate().plusDays(days);
			if (LocalDateTime.now().isBefore(nextValidRequest)) {
				validation = Validation.invalid("You’ve already requested an audiobook. Come back again on "
						+ DateUtils.formatDate(nextValidRequest) + ".");
			}
		}

		return validation;
	}

	/**
	 * Check if the association already exists
	 *
	 * @param messageId Message Id of the request
	 * @param groupId   Group where the request was sent
	 * @param userId    User that sent the request
	 * @param link      Link of the requested content
	 * @return True if exists, False otherwise
	 */
	public boolean exists(Long messageId, Long groupId, Long userId, String link) {
		boolean exists = false;

		// get request by PK
		Optional<Request> optional = requestService.findById(messageId, groupId);
		if (optional.isPresent()) {
			// exists, the user is the creator
			exists = true;
		} else {
			// get request by UQ
			Request request = requestService.findByLink(link);

			if (request != null) {
				// the request exists, check if association exists
				exists = repository.existsById(new UserRequestPK(messageId, groupId, userId));
			}
		}

		return exists;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void insert(Long messageId, String content, String link, Format format, Source source, String otherTags,
			Long userId, Long groupId, LocalDateTime timestamp) {
		// prepare request
		UserRequest userRequest = new UserRequest();
		userRequest.setGroupId(groupId);
		userRequest.setDate(timestamp);

		Request request = requestService.findByLink(link);
		// request doesn't exists
		if (request == null) {
			// new request
			requestService.insert(messageId, groupId, link, content, format, source, otherTags);

			UserRequestPK primaryKey = new UserRequestPK(messageId, groupId, userId);
			userRequest.setId(primaryKey);
			userRequest.setRole(UserRequestRole.CREATOR);

			repository.save(userRequest);
		} else {
			// request exists
			UserRequestPK primaryKey = new UserRequestPK(request.getId().getMessageId(), request.getId().getGroupId(),
					userId);
			if (!repository.existsById(primaryKey)) {

				// create association
				userRequest.setId(primaryKey);
				userRequest.setRole(UserRequestRole.SUBSCRIBER);

				repository.save(userRequest);
			}
		}
	}

}