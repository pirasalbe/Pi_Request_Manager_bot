package com.pirasalbe.services;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pirasalbe.models.RequestAssociationInfo;
import com.pirasalbe.models.RequestResult;
import com.pirasalbe.models.RequestResult.Result;
import com.pirasalbe.models.UserRequestRole;
import com.pirasalbe.models.database.Request;
import com.pirasalbe.models.database.UserRequest;
import com.pirasalbe.models.request.Format;
import com.pirasalbe.models.request.Source;
import com.pirasalbe.utils.DateUtils;

/**
 * Service that manages the user request table
 *
 * @author pirasalbe
 *
 */
@Component
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class RequestManagementService {

	private static final Logger LOGGER = LoggerFactory.getLogger(RequestManagementService.class);

	private static final long HOURS_BEFORE_REPEATING_REQUEST = 48l;

	@Autowired
	private RequestService requestService;

	@Autowired
	private UserRequestService userRequestService;

	/**
	 * Get Request and UserRequest info
	 *
	 * @param messageId Message Id of the request
	 * @param groupId   Group where the request was sent
	 * @param userId    User that sent the request
	 * @param link      Link of the requested content
	 * @return True if exists, False otherwise
	 */
	public RequestAssociationInfo getRequestAssociationInfo(Long messageId, Long groupId, Long userId, String link) {
		RequestAssociationInfo result = null;

		// get request by PK
		Optional<Request> optional = requestService.findById(messageId, groupId);

		if (optional.isPresent()) {
			// if exists, the user is the creator
			result = RequestAssociationInfo.creator();
		} else {
			// get request by UQ
			Request request = requestService.findByLink(link);

			if (request == null) {
				// the request doesn't exists
				result = RequestAssociationInfo.noRequest();
			} else if (userRequestService.existsById(messageId, groupId, userId)) {
				// the request exists, the association exists as subscriber
				result = RequestAssociationInfo.subscriber();
			} else {
				// the request exists, but no association
				result = RequestAssociationInfo.noAssociation();
			}
		}

		return result;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public RequestResult manageRequest(Long messageId, String content, String link, Format format, Source source,
			String otherTags, Long userId, Long groupId, LocalDateTime requestDate) {
		RequestResult result = null;

		Request request = requestService.findByLink(link);
		// request doesn't exists
		if (request == null) {
			insertNewRequest(messageId, content, link, format, source, otherTags, userId, groupId, requestDate);
			result = new RequestResult(Result.NEW);
		} else {
			result = manageAssociation(userId, requestDate, request);
		}

		return result;
	}

	private void insertNewRequest(Long messageId, String content, String link, Format format, Source source,
			String otherTags, Long userId, Long groupId, LocalDateTime requestDate) {
		// new request
		requestService.insert(messageId, groupId, link, content, format, source, otherTags);

		// new association
		userRequestService.insert(messageId, groupId, userId, UserRequestRole.CREATOR, requestDate);
	}

	private RequestResult manageAssociation(Long userId, LocalDateTime requestDate, Request request) {
		RequestResult result = null;

		// request exists
		Long messageId = request.getId().getMessageId();
		Long groupId = request.getId().getGroupId();

		// check if exists association
		Optional<UserRequest> optional = userRequestService.findById(messageId, groupId, userId);
		if (optional.isPresent()) {
			// update association
			result = updateAssociation(userId, requestDate, messageId, groupId, optional.get());
		} else {
			// create association
			userRequestService.insert(messageId, groupId, userId, UserRequestRole.SUBSCRIBER, requestDate);
			result = new RequestResult(Result.SUBSCRIBED);
		}

		return result;
	}

	private RequestResult updateAssociation(Long userId, LocalDateTime requestDate, Long messageId, Long groupId,
			UserRequest userRequest) {
		LocalDateTime previousRequestDate = userRequest.getDate();

		// update date
		userRequestService.updateDate(messageId, groupId, userId, requestDate);

		// new request date should be after a cooldown period
		LocalDateTime minDateForNewRequest = previousRequestDate.plusHours(HOURS_BEFORE_REPEATING_REQUEST);

		RequestResult result = null;
		if (minDateForNewRequest.isBefore(requestDate)) {
			result = new RequestResult(Result.REPEATED_REQUEST);
		} else {
			LOGGER.warn("User {} repeated the request on {}, which is before {}", userId, requestDate,
					minDateForNewRequest);

			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("You already requested this title on ");
			stringBuilder.append(DateUtils.formatDateTime(previousRequestDate)).append(".\n");
			stringBuilder.append("Wait 48 hours from now before repeating a request.\n");
			stringBuilder.append(
					"If you have requested it many times and still haven't received the book, then it's most likely that the book is not available as of now. It's better if you request again after a month or so.");
			result = new RequestResult(Result.REQUEST_REPEATED_TOO_EARLY, stringBuilder.toString());
		}

		return result;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void deleteGroupRequests(Long groupId) {
		userRequestService.deleteByGroupId(groupId);
		requestService.deleteByGroupId(groupId);
	}

}
