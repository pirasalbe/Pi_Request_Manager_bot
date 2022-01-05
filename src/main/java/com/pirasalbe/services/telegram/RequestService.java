package com.pirasalbe.services.telegram;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pirasalbe.models.Format;
import com.pirasalbe.models.RequestStatus;
import com.pirasalbe.models.Source;
import com.pirasalbe.models.database.Request;
import com.pirasalbe.repositories.RequestRepository;

/**
 * Service that manages the request table
 *
 * @author pirasalbe
 *
 */
@Component
public class RequestService {

	@Autowired
	private RequestRepository repository;

	public long insert(String content, Format format, Source source, String otherTags) {
		Request request = new Request();

		request.setStatus(RequestStatus.NEW);
		request.setContent(content);
		request.setFormat(format);
		request.setSource(source);
		request.setOtherTags(otherTags);

		repository.save(request);

		return request.getId();
	}

}
