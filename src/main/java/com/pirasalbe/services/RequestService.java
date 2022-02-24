package com.pirasalbe.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pirasalbe.models.database.Request;
import com.pirasalbe.models.database.RequestPK;
import com.pirasalbe.models.request.Format;
import com.pirasalbe.models.request.RequestStatus;
import com.pirasalbe.models.request.Source;
import com.pirasalbe.repositories.RequestRepository;
import com.pirasalbe.utils.DateUtils;

/**
 * Service that manages the request table
 *
 * @author pirasalbe
 *
 */
@Component
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class RequestService {

	@Autowired
	private RequestRepository repository;

	@PersistenceContext
	private EntityManager entityManager;

	public Request findByUniqueKey(Long groupId, Long userId, String link) {
		return repository.findByUniqueKey(groupId, userId, link);
	}

	public Optional<Request> findById(Long messageId, Long groupId) {
		return repository.findById(new RequestPK(messageId, groupId));
	}

	public boolean deleteById(Long messageId, Long groupId) {
		boolean deleted = false;

		RequestPK id = new RequestPK(messageId, groupId);
		if (repository.existsById(id)) {
			repository.deleteById(id);
			deleted = true;
		}

		return deleted;
	}

	public void flushChanges() {
		entityManager.flush();
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void deleteOldRequests() {
		LocalDateTime twoMonths = DateUtils.getNow().minusMonths(2);
		repository.deleteOldCancelled(twoMonths);
		repository.deleteOldResolved(twoMonths);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void insert(Long messageId, Long groupId, String link, String content, Format format, Source source,
			String otherTags, Long userId, LocalDateTime requestDate) {
		Request request = new Request();

		request.setId(new RequestPK(messageId, groupId));
		request.setLink(link);
		request.setStatus(RequestStatus.PENDING);
		request.setContent(content);
		request.setFormat(format);
		request.setSource(source);
		request.setOtherTags(otherTags);
		request.setUserId(userId);
		request.setRequestDate(requestDate);

		repository.save(request);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void update(Long messageId, Long groupId, String link, String content, Format format, Source source,
			String otherTags) {
		update(messageId, groupId, link, content, format, source, otherTags, null);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void update(Long messageId, Long groupId, String link, String content, Format format, Source source,
			String otherTags, LocalDateTime requestDate) {
		Optional<Request> optional = findById(messageId, groupId);

		if (optional.isPresent()) {
			Request request = optional.get();
			request.setLink(link);
			request.setContent(content);
			request.setFormat(format);
			request.setSource(source);
			request.setOtherTags(otherTags);

			if (requestDate != null) {
				request.setRequestDate(requestDate);
			}

			repository.save(request);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void deleteByGroupId(Long groupId) {
		repository.deleteByGroupId(groupId);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void updateStatus(Request request, RequestStatus status) {
		request.setStatus(status);
		if (status == RequestStatus.RESOLVED) {
			request.setResolvedDate(DateUtils.getNow());
		} else {
			request.setResolvedDate(null);
		}

		repository.save(request);
	}

	public Request getLastEbookRequestOfUser(Long userId) {
		Request request = null;

		List<Request> requests = repository.getLastEbookRequestOfUser(userId, PageRequest.of(0, 1));
		if (!requests.isEmpty()) {
			request = requests.get(0);
		}

		return request;
	}

	public Request getLastAudiobookRequestOfUser(Long userId) {
		Request request = null;

		List<Request> requests = repository.getLastAudiobookRequestOfUser(userId, PageRequest.of(0, 1));
		if (!requests.isEmpty()) {
			request = requests.get(0);
		}

		return request;
	}

	public Request getLastAudiobookResolvedOfUser(Long userId) {
		Request request = null;

		List<Request> requests = repository.getLastAudiobookResolvedOfUser(userId, PageRequest.of(0, 1));
		if (!requests.isEmpty()) {
			request = requests.get(0);
		}

		return request;
	}

	public List<Request> getUserEbookRequestsOfToday(Long userId, LocalDateTime last24Hours) {
		return repository.getUserEbookRequestsOfToday(userId, last24Hours);
	}

	public List<Request> findRequests(Optional<Long> groupId, RequestStatus status, Optional<Source> source,
			Optional<Format> format, boolean descendent) {
		List<Request> requests = null;
		Direction direction = descendent ? Direction.DESC : Direction.ASC;
		Sort sort = Sort.by(direction, "requestDate");

		if (groupId.isPresent() && source.isPresent() && format.isPresent()) {
			requests = repository.findByFilters(groupId.get(), status, source.get(), format.get(), sort);
		} else if (groupId.isPresent() && source.isPresent()) {
			requests = repository.findByFilters(groupId.get(), status, source.get(), sort);
		} else if (groupId.isPresent() && format.isPresent()) {
			requests = repository.findByFilters(groupId.get(), status, format.get(), sort);
		} else if (groupId.isPresent()) {
			requests = repository.findByFilters(groupId.get(), status, sort);
		} else if (source.isPresent() && format.isPresent()) {
			requests = repository.findByFilters(status, source.get(), format.get(), sort);
		} else if (source.isPresent()) {
			requests = repository.findByFilters(status, source.get(), sort);
		} else if (format.isPresent()) {
			requests = repository.findByFilters(status, format.get(), sort);
		} else {
			requests = repository.findByFilters(status, sort);
		}

		return requests;
	}

}
