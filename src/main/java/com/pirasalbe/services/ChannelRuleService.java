package com.pirasalbe.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pirasalbe.models.ChannelRuleType;
import com.pirasalbe.models.database.ChannelRule;
import com.pirasalbe.repositories.ChannelRuleRepository;

/**
 * Service that manages the channel rule table
 *
 * @author pirasalbe
 *
 */
@Component
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class ChannelRuleService {

	@Autowired
	private ChannelRuleRepository repository;

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void deleteByChannelId(Long channelId) {
		repository.deleteByChannelId(channelId);
	}

	public List<ChannelRule> getByChannelId(Long channelId) {
		return repository.getByChannelId(channelId);
	}

	public List<ChannelRule> getByChannelIdAndType(Long channelId, ChannelRuleType type) {
		return repository.getByChannelIdAndType(channelId, type);
	}

}
