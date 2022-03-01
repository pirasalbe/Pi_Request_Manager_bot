package com.pirasalbe.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pirasalbe.models.ChannelRuleType;
import com.pirasalbe.models.database.ChannelRule;
import com.pirasalbe.models.database.ChannelRulePK;
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

	public boolean existsById(Long channelId, ChannelRuleType type, String value) {
		ChannelRulePK id = new ChannelRulePK(channelId, type, value);
		return repository.existsById(id);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void insert(Long channelId, ChannelRuleType type, String value) {
		ChannelRule rule = new ChannelRule();
		ChannelRulePK id = new ChannelRulePK(channelId, type, value);
		rule.setId(id);

		repository.save(rule);
	}

	public void delete(Long channelId, ChannelRuleType type, String value) {
		ChannelRulePK id = new ChannelRulePK(channelId, type, value);

		repository.deleteById(id);
	}

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
