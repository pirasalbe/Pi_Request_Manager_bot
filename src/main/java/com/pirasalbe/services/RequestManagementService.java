package com.pirasalbe.services;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pirasalbe.models.Format;
import com.pirasalbe.models.RequestAssociationInfo;
import com.pirasalbe.models.Source;
import com.pirasalbe.models.UserRequestRole;
import com.pirasalbe.models.database.Request;

/**
 * Service that manages the user request table
 *
 * @author pirasalbe
 *
 */
@Component
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class RequestManagementService {

	private RequestService requestService;

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
	public void manageRequest(Long messageId, String content, String link, Format format, Source source,
			String otherTags, Long userId, Long groupId, LocalDateTime requestDate) {

		Request request = requestService.findByLink(link);
		// request doesn't exists
		if (request == null) {
			insertNewRequest(messageId, content, link, format, source, otherTags, userId, groupId, requestDate);
		} else {
			manageAssociation(userId, requestDate, request);
		}
	}

	private void insertNewRequest(Long messageId, String content, String link, Format format, Source source,
			String otherTags, Long userId, Long groupId, LocalDateTime requestDate) {
		// new request
		requestService.insert(messageId, groupId, link, content, format, source, otherTags);

		// new association
		userRequestService.insert(messageId, groupId, userId, UserRequestRole.CREATOR, requestDate);
	}

	private void manageAssociation(Long userId, LocalDateTime requestDate, Request request) {
		// request exists
		Long messageId = request.getId().getMessageId();
		Long groupId = request.getId().getGroupId();

		// check if exists association
		boolean exists = userRequestService.existsById(messageId, groupId, userId);
		if (exists) {
			// update association
			userRequestService.updateDate(messageId, groupId, userId, requestDate);
		} else {
			// create association
			userRequestService.insert(messageId, groupId, userId, UserRequestRole.SUBSCRIBER, requestDate);
		}
	}

}
