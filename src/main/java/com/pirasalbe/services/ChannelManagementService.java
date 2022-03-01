package com.pirasalbe.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pirasalbe.models.ChannelRuleType;
import com.pirasalbe.models.database.ChannelRule;

/**
 * Service that manages the channels
 *
 * @author pirasalbe
 *
 */
@Component
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class ChannelManagementService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChannelManagementService.class);

	@Autowired
	private ChannelService channelService;

	@Autowired
	private ChannelRuleService channelRuleService;

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void insertIfNotExists(Long id, String name) {
		// insert
		if (!channelService.existsById(id)) {
			channelService.insert(id, name);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void deleteIfExists(Long id) {
		if (channelService.existsById(id)) {
			channelRuleService.deleteByChannelId(id);
			channelService.delete(id);
		}
	}

	public List<ChannelRule> getChannelRulesByType(Long channelId, ChannelRuleType type) {
		return channelRuleService.getByChannelIdAndType(channelId, type);
	}

}
