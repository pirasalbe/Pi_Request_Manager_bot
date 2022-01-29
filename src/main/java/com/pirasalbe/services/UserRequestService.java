package com.pirasalbe.services;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pirasalbe.models.LastRequestInfo;
import com.pirasalbe.models.UserRequestRole;
import com.pirasalbe.models.Validation;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.database.UserRequest;
import com.pirasalbe.models.database.UserRequestPK;
import com.pirasalbe.models.request.Format;
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

	/**
	 * Check if user can send a new request
	 *
	 * @param group       Group of the request
	 * @param userId      User that wants to send a request
	 * @param format      Format of what has been requested
	 * @param requestTime Time of the request
	 * @return Validation
	 */
	public Validation canRequest(Group group, Long userId, Format format, LocalDateTime requestTime) {
		Validation validation = isFormatAllowed(group.isAllowEbooks(), group.isAllowAudiobooks(), format);

		// check format allowed
		if (validation.isValid()) {

			// check request limit for ebooks
			if (format.equals(Format.EBOOK)) {
				validation = isValidEbookRequest(userId, group.getRequestLimit(), requestTime);

			} else if (format.equals(Format.AUDIOBOOK)) {

				// check audiobook limit
				LastRequestInfo lastRequestInfo = repository.getLastAudiobookRequestOfUserInGroup(userId);

				validation = isValidAudiobookRequest(group.getAudiobooksDaysWait(),
						group.getEnglishAudiobooksDaysWait(), lastRequestInfo);
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

	private Validation isValidEbookRequest(Long userId, Integer requestLimit, LocalDateTime requestTime) {
		Validation validation = Validation.valid();

		long requests = repository.countUserEbookRequestsInGroupOfToday(userId, requestTime.minusDays(1));
		// it's invalid if already reached the limit
		if (requests >= requestLimit) {
			validation = Validation.invalid("You’re only allowed to request up to " + requestLimit + " book"
					+ (requestLimit > 1 ? "s" : "") + " per day. Come back again tomorrow.");
		}

		return validation;
	}

	public boolean existsById(Long messageId, Long groupId, Long userId) {
		return repository.existsById(new UserRequestPK(messageId, groupId, userId));
	}

	public Optional<UserRequest> findById(Long messageId, Long groupId, Long userId) {
		return repository.findById(new UserRequestPK(messageId, groupId, userId));
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void insert(Long messageId, Long groupId, Long userId, UserRequestRole role, LocalDateTime requestDate) {
		UserRequest userRequest = new UserRequest();
		UserRequestPK primaryKey = new UserRequestPK(messageId, groupId, userId);
		userRequest.setId(primaryKey);
		userRequest.setGroupId(groupId);
		userRequest.setRole(role);
		userRequest.setDate(requestDate);

		repository.save(userRequest);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void updateDate(Long messageId, Long groupId, Long userId, LocalDateTime date) {
		Optional<UserRequest> optional = findById(messageId, groupId, userId);

		if (optional.isPresent()) {
			UserRequest userRequest = optional.get();
			userRequest.setDate(date);

			repository.save(userRequest);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void deleteByGroupId(Long groupId) {
		repository.deleteByGroupId(groupId);
	}

}
