package com.pirasalbe.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pengrad.telegrambot.model.Message;
import com.pirasalbe.models.LastRequestInfo;
import com.pirasalbe.models.LastRequestInfo.Type;
import com.pirasalbe.models.RequestResult;
import com.pirasalbe.models.RequestResult.Result;
import com.pirasalbe.models.UpdateRequestAction;
import com.pirasalbe.models.Validation;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.database.Request;
import com.pirasalbe.models.database.RequestPK;
import com.pirasalbe.models.request.Format;
import com.pirasalbe.models.request.RequestStatus;
import com.pirasalbe.models.request.Source;
import com.pirasalbe.utils.DateUtils;
import com.pirasalbe.utils.RequestUtils;
import com.pirasalbe.utils.StringUtils;
import com.pirasalbe.utils.TelegramUtils;

/**
 * Service that manages the requests
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

	@Scheduled(cron = "0 0 0 1-3 * ?")
	public void deleteOldRequests() {
		LOGGER.info("Start scheduled cleaning");
		try {
			requestService.deleteOldRequests();
		} catch (Exception e) {
			LOGGER.error("Cannot delete old requests", e);
		}
	}

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
				Request pendingRequest = requestService.getLastAudiobookRequestOfUser(userId);
				Request resolvedRequest = requestService.getLastAudiobookResolvedOfUser(userId);

				validation = isValidAudiobookRequest(userId, group.getAudiobooksDaysWait(),
						group.getEnglishAudiobooksDaysWait(), pendingRequest, resolvedRequest);
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

	private String getRequestLink(Request request, String text) {
		StringBuilder builder = new StringBuilder();
		builder.append("(").append("<a href='");
		builder.append(TelegramUtils.getLink(request.getId().getGroupId().toString(),
				request.getId().getMessageId().toString()));
		builder.append("'>");
		builder.append(text).append("</a>").append(")");

		return builder.toString();
	}

	private Validation isValidAudiobookRequest(Long userId, Integer audiobooksDaysWait,
			Integer englishAudiobooksDaysWait, Request pendingRequest, Request resolvedRequest) {
		Validation validation = Validation.valid();

		LastRequestInfo requestInfo = getLastRequestInfo(pendingRequest, resolvedRequest);

		if (requestInfo != null) {
			// if null -> language was English
			int days = requestInfo.getOtherTags() == null ? englishAudiobooksDaysWait : audiobooksDaysWait;

			// last audiobook
			LocalDateTime nextValidRequest = requestInfo.getDate().plusDays(days);
			if (LocalDateTime.now().isBefore(nextValidRequest)) {
				LOGGER.warn("User {}, last audiobook request/resolved {} (tags {}), next valid request {}", userId,
						requestInfo.getDate(), requestInfo.getOtherTags() != null ? requestInfo.getOtherTags() : "",
						nextValidRequest);

				StringBuilder stringBuilder = new StringBuilder();
				stringBuilder.append("You’ve already ");
				stringBuilder.append(requestInfo.getType().name().toLowerCase());
				stringBuilder.append(" an audiobook in one of our groups on ");
				stringBuilder.append(DateUtils.formatDate(requestInfo.getDate()));
				stringBuilder.append(" ").append(getRequestLink(requestInfo.getRequest(), "link")).append(".\n");
				stringBuilder.append(RequestUtils.getComeBackAgain(DateUtils.getNow(), nextValidRequest));
				validation = Validation.invalid(stringBuilder.toString());
			}
		}

		return validation;
	}

	private LastRequestInfo getLastRequestInfo(Request pendingRequest, Request resolvedRequest) {
		LastRequestInfo requestInfo = null;

		if (resolvedRequest != null && pendingRequest != null) {
			// take the latter
			if (resolvedRequest.getResolvedDate().isAfter(pendingRequest.getRequestDate())) {
				requestInfo = new LastRequestInfo(Type.RECEIVED, resolvedRequest.getResolvedDate(), resolvedRequest);
			} else {
				requestInfo = new LastRequestInfo(Type.REQUESTED, pendingRequest.getRequestDate(), pendingRequest);
			}
		} else if (resolvedRequest != null) {
			requestInfo = new LastRequestInfo(Type.RECEIVED, resolvedRequest.getResolvedDate(), resolvedRequest);
		} else if (pendingRequest != null) {
			requestInfo = new LastRequestInfo(Type.REQUESTED, pendingRequest.getRequestDate(), pendingRequest);
		}

		return requestInfo;
	}

	private Validation isValidEbookRequest(Long userId, Integer requestLimit, LocalDateTime requestTime) {
		Validation validation = Validation.valid();

		LocalDateTime last24Hours = requestTime.minusHours(24);
		List<Request> requests = requestService.getUserEbookRequestsOfToday(userId, last24Hours);
		long requestCount = requests.size();
		// it's invalid if already reached the limit
		if (requestCount >= requestLimit) {
			Request lastRequest = requests.get(0);
			LocalDateTime lastRequestDate = lastRequest.getRequestDate();
			LOGGER.warn("User {}, new request {}, {} ebook requested since {}", userId, requestTime, requestCount,
					lastRequestDate);

			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("You’re only allowed to request ").append(requestLimit > 1 ? "up to " : "");
			stringBuilder.append(requestLimit).append(" book").append(StringUtils.getPlural(requestLimit));
			stringBuilder.append(" every 24 hours");
			stringBuilder.append(" ").append(getRequestLink(lastRequest, "last request")).append(".\n");
			stringBuilder.append(RequestUtils.getComeBackAgain(requestTime, lastRequestDate.plusHours(24)));
			validation = Validation.invalid(stringBuilder.toString());
		}

		return validation;
	}

	/**
	 * Get update action to perform
	 *
	 * @param messageId Message Id of the request
	 * @param groupId   Group where the request was sent
	 * @param userId    User that sent the request
	 * @param link      Link of the requested content
	 * @return Action to perform
	 */
	public UpdateRequestAction getUpdateRequestAction(Long messageId, Long groupId, Long userId, String link) {
		UpdateRequestAction result = null;

		// get request by PK
		Optional<Request> optional = requestService.findById(messageId, groupId);

		if (optional.isPresent()) {
			// if exists, the user is the creator
			result = UpdateRequestAction.UPDATE_REQUEST;
		} else {
			result = UpdateRequestAction.NEW_REQUEST;
		}

		return result;
	}

	/**
	 * Manage the insert of a request
	 *
	 * @param messageId   Message Id of the request
	 * @param content     Text content
	 * @param link        Link from the request
	 * @param format      Format
	 * @param source      Source of the link
	 * @param otherTags   Language or other tags
	 * @param userId      User that made the request
	 * @param group       Group of the request
	 * @param requestDate Date of the request
	 * @return Result of the operation
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public RequestResult manageRequest(Long messageId, String content, String link, Format format, Source source,
			String otherTags, Long userId, Group group, LocalDateTime requestDate) {
		RequestResult result = null;

		Request request = requestService.findByUniqueKey(group.getId(), userId, link);
		if (request == null) {
			// request doesn't exists
			requestService.insert(messageId, group.getId(), link, content, format, source, otherTags, userId,
					requestDate);
			result = new RequestResult(Result.NEW);
		} else {
			// request exists, repeat it
			result = repeatRequest(request, group, messageId, userId, link, content, format, source, otherTags,
					requestDate);
		}

		return result;
	}

	private RequestResult repeatRequest(Request request, Group group, Long newMessageId, Long userId, String link,
			String content, Format format, Source source, String otherTags, LocalDateTime requestDate) {
		RequestResult result = null;

		// request exists
		LocalDateTime previousRequestDate = request.getRequestDate();

		// new request date should be after a cooldown period
		LocalDateTime minDateForNewRequest = previousRequestDate.plusHours(HOURS_BEFORE_REPEATING_REQUEST);

		boolean specialTags = hasSpecialTags(group, request.getSource());
		boolean isCancelled = request.getStatus() == RequestStatus.CANCELLED;
		if (isCancelled || (!specialTags && minDateForNewRequest.isBefore(requestDate))) {
			// allow repeating cancelled requests
			// allow repeating no special tags after 48 hours
			updateOrDeleteInsertRequest(request, newMessageId, link, content, format, source, otherTags, requestDate);
			result = new RequestResult(Result.REPEATED_REQUEST);
		} else if (specialTags) {
			// special tags request
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("You already requested this title on ");
			stringBuilder.append(DateUtils.formatDate(previousRequestDate));
			stringBuilder.append(" ").append(getRequestLink(request, "link")).append(".\n");
			stringBuilder.append("No need to bump requests with special hashtags.");
			result = new RequestResult(Result.CANNOT_REPEAT_REQUEST, stringBuilder.toString());
		} else {
			// no special tags, but before 48 hours
			LOGGER.warn("User {} repeated the request on {}, which is before {}", userId, requestDate,
					minDateForNewRequest);

			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("You already requested this title on ")
					.append(DateUtils.formatDate(previousRequestDate));
			stringBuilder.append(" ").append(getRequestLink(request, "original request")).append(".\n");
			stringBuilder.append(RequestUtils.getComeBackAgain(requestDate, minDateForNewRequest)).append("\n");
			stringBuilder.append(
					"If you have requested it many times and still haven't received the book, then it's most likely that the book is not available as of now. It's better if you request again after a month or so.");
			result = new RequestResult(Result.REQUEST_REPEATED_TOO_EARLY, stringBuilder.toString());
		}

		return result;
	}

	private void updateOrDeleteInsertRequest(Request request, Long newMessageId, String link, String content,
			Format format, Source source, String otherTags, LocalDateTime requestDate) {
		RequestPK id = request.getId();

		if (request.getStatus() == RequestStatus.CANCELLED) {
			// if request has been canceled, delete and insert again
			deleteRequest(id.getMessageId(), id.getGroupId());
			requestService.flushChanges();
			requestService.insert(newMessageId, id.getGroupId(), link, content, format, source, otherTags,
					request.getUserId(), requestDate);
		} else {
			// update request
			requestService.update(id.getMessageId(), id.getGroupId(), link, content, format, source, otherTags,
					requestDate);
		}
	}

	private boolean hasSpecialTags(Group group, Source source) {
		List<Source> sources = RequestUtils.getNoRepeatSources(group.getNoRepeat());

		return sources.contains(source);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void deleteGroupRequests(Long groupId) {
		requestService.deleteByGroupId(groupId);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public boolean deleteRequest(Long messageId, Long groupId) {
		return requestService.deleteById(messageId, groupId);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public boolean markPending(Message message) {
		return updateStatus(message, RequestStatus.PENDING);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public boolean markOutstanding(Message message) {
		return updateStatus(message, RequestStatus.OUTSTANDING);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public boolean markDone(Message message) {
		return updateStatus(message, RequestStatus.RESOLVED);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public boolean markCancelled(Long messageId, Long groupId) {
		boolean success = false;

		Optional<Request> optional = requestService.findById(messageId, groupId);
		if (optional.isPresent()) {
			// mark request as done
			requestService.updateStatus(optional.get(), RequestStatus.CANCELLED);
			success = true;
		}

		return success;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	private boolean updateStatus(Message message, RequestStatus status) {
		String link = RequestUtils.getLink(message.text(), message.entities());

		boolean success = false;

		if (link != null) {
			Request request = requestService.findByUniqueKey(message.chat().id(), message.from().id(), link);
			if (request != null) {
				// update status
				updateStatus(request, status);
				success = true;
			}
		}

		return success;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void updateStatus(Request request, RequestStatus status) {
		requestService.updateStatus(request, status);
	}

}
