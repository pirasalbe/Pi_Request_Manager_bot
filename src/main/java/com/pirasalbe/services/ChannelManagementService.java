package com.pirasalbe.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pirasalbe.models.ChannelRuleType;
import com.pirasalbe.models.database.Channel;
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

	@Autowired
	private ChannelRequestService channelRequestService;

	public List<Channel> findAllChannels() {
		return channelService.findAll();
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void insertIfNotExists(Long id, String name) {
		if (!channelService.existsById(id)) {
			channelService.insert(id, name);
			LOGGER.info("New channel: [{}]", id);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void deleteIfExists(Long id) {
		if (channelService.existsById(id)) {
			channelRequestService.deleteByChannelId(id);
			channelRuleService.deleteByChannelId(id);
			channelService.delete(id);
			LOGGER.info("Deleted channel: [{}]", id);
		}
	}

	public List<ChannelRule> findChannelRules(Long channelId) {
		return channelRuleService.findByChannelId(channelId);
	}

	public List<ChannelRule> findChannelRulesByType(Long channelId, ChannelRuleType type) {
		return channelRuleService.findByChannelIdAndType(channelId, type);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void toggleRule(Long channelId, ChannelRuleType type, String value) {
		if (channelService.existsById(channelId)) {

			if (channelRuleService.existsById(channelId, type, value)) {
				channelRuleService.delete(channelId, type, value);
				LOGGER.info("Deleted channel rule: [{}], [{}]", channelId, type);
			} else {
				channelRuleService.insert(channelId, type, value);
				LOGGER.info("Added channel rule: [{}], [{}]", channelId, type);
			}
		}
	}

}
