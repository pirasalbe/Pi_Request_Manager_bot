package com.pirasalbe.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
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

	public Request findByUniqueKey(Long groupId, String link) {
		return repository.findByUniqueKey(groupId, link);
	}

	public Optional<Request> findById(Long messageId, Long groupId) {
		return repository.findById(new RequestPK(messageId, groupId));
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void insert(Long messageId, Long groupId, String link, String content, Format format, Source source,
			String otherTags) {
		Request request = new Request();

		request.setId(new RequestPK(messageId, groupId));
		request.setLink(link);
		request.setStatus(RequestStatus.NEW);
		request.setContent(content);
		request.setFormat(format);
		request.setSource(source);
		request.setOtherTags(otherTags);

		repository.save(request);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void update(Long messageId, Long groupId, String link, String content, Format format, Source source,
			String otherTags) {
		Optional<Request> optional = findById(messageId, groupId);

		if (optional.isPresent()) {
			Request request = optional.get();
			request.setLink(link);
			request.setContent(content);
			request.setFormat(format);
			request.setSource(source);
			request.setOtherTags(otherTags);

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

}
