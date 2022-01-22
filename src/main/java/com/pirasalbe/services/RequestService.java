package com.pirasalbe.services;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pirasalbe.models.Format;
import com.pirasalbe.models.RequestStatus;
import com.pirasalbe.models.Source;
import com.pirasalbe.models.database.Request;
import com.pirasalbe.models.database.RequestPK;
import com.pirasalbe.repositories.RequestRepository;

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

	public Request findByLink(String link) {
		return repository.findByLink(link);
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

}
