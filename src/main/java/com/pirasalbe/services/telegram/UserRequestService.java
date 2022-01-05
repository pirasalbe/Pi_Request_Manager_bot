package com.pirasalbe.services.telegram;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pirasalbe.models.Format;
import com.pirasalbe.models.LastRequestInfo;
import com.pirasalbe.models.Source;
import com.pirasalbe.models.UserRequestRole;
import com.pirasalbe.models.Validation;
import com.pirasalbe.models.database.Group;
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

	public void insertRequest(String content, Format format, Source source, String otherTags, Long userId, Long groupId,
			LocalDateTime timestamp) {
		long requestId = requestService.insert(content, format, source, otherTags);

		UserRequest userRequest = new UserRequest();
		UserRequestPK primaryKey = new UserRequestPK(requestId, userId);
		userRequest.setId(primaryKey);
		userRequest.setGroupId(groupId);
		userRequest.setRole(UserRequestRole.CREATOR);
		userRequest.setDate(timestamp);

		repository.save(userRequest);
	}

}
