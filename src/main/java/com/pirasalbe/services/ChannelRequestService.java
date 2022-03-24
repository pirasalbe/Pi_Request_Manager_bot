package com.pirasalbe.services;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pirasalbe.models.database.ChannelRequest;
import com.pirasalbe.models.database.ChannelRequestPK;
import com.pirasalbe.models.database.RequestPK;
import com.pirasalbe.repositories.ChannelRequestRepository;

/**
 * Service that manages the channel request table
 *
 * @author pirasalbe
 *
 */
@Component
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class ChannelRequestService {

	@Autowired
	private ChannelRequestRepository repository;

	@PersistenceContext
	private EntityManager entityManager;

	public boolean existsById(Long channelId, Long messageId) {
		ChannelRequestPK id = new ChannelRequestPK(channelId, messageId);
		return repository.existsById(id);
	}

	public ChannelRequest findByUniqueKey(Long channelId, Long requestGroupId, Long requestMessageId) {
		return repository.findByUniqueKey(channelId, requestGroupId, requestMessageId);
	}

	public List<ChannelRequest> findByRequest(Long requestGroupId, Long requestMessageId) {
		return repository.findByRequest(requestGroupId, requestMessageId);
	}

	public List<ChannelRequest> findByGroupId(Long requestGroupId) {
		return repository.findByGroupId(requestGroupId);
	}

	public List<ChannelRequest> findByChannelId(Long channelId) {
		return repository.findByChannelId(channelId);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void insert(Long channelId, Long messageId, RequestPK requestId) {
		// insert
		ChannelRequest channelRequest = new ChannelRequest();
		ChannelRequestPK id = new ChannelRequestPK(channelId, messageId);
		channelRequest.setId(id);
		channelRequest.setRequestMessageId(requestId.getMessageId());
		channelRequest.setRequestGroupId(requestId.getGroupId());

		repository.save(channelRequest);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void delete(Long channelId, Long messageId) {
		ChannelRequestPK id = new ChannelRequestPK(channelId, messageId);
		repository.deleteById(id);

		entityManager.flush();
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void deleteByChannelId(Long channelId) {
		repository.deleteByChannelId(channelId);
	}

}
