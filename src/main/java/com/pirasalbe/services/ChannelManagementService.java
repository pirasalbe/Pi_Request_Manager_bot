package com.pirasalbe.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.response.SendResponse;
import com.pirasalbe.configurations.TelegramConfiguration;
import com.pirasalbe.models.ChannelRuleType;
import com.pirasalbe.models.database.Channel;
import com.pirasalbe.models.database.ChannelRequest;
import com.pirasalbe.models.database.ChannelRule;
import com.pirasalbe.models.database.Request;
import com.pirasalbe.models.database.RequestPK;
import com.pirasalbe.services.telegram.TelegramBotService;
import com.pirasalbe.utils.RequestUtils;
import com.pirasalbe.utils.TelegramUtils;

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
	private TelegramConfiguration configuration;

	@Autowired
	private ChannelService channelService;

	@Autowired
	private ChannelRuleService channelRuleService;

	@Autowired
	private ChannelRequestService channelRequestService;

	private TelegramBot bot;

	public ChannelManagementService(TelegramBotService telegramBotService) {
		this.bot = telegramBotService.getBot();
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

	/**
	 * Send a request updated to a channel<br>
	 * <b>It's highly recommended to call this method on a different thread</b>
	 *
	 * @param request   Request to update
	 * @param groupName Name of the group of the request
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void forwardRequest(Request request, String groupName) {
		// delete request from all channels
		deleteRequest(request.getId());

		// forward request again to the right channels
		sendRequestToChannel(request, groupName);
	}

	/**
	 * Send a request to a channel<br>
	 * <b>It's highly recommended to call this method on a different thread</b>
	 *
	 * @param request   Request to forward
	 * @param groupName Name of the group of the request
	 */
	private void sendRequestToChannel(Request request, String groupName) {
		List<Channel> channels = channelService.findAll();

		for (Channel channel : channels) {

			// check that the request matches with the channel rules
			if (requestMatchRules(channel, request)) {

				// forward request
				Long messageId = forwardRequest(channel.getId(), request, groupName);

				// update channel request table
				if (messageId != null) {
					channelRequestService.insert(channel.getId(), messageId, request.getId());
				}
			}
		}
	}

	private boolean requestMatchRules(Channel channel, Request request) {
		Long channelId = channel.getId();

		// check group
		return existsRuleWithValue(channelId, ChannelRuleType.GROUP, request.getId().getGroupId()) &&
		// check format
				existsRuleWithValue(channelId, ChannelRuleType.FORMAT, request.getFormat()) &&
				// check source
				existsRuleWithValue(channelId, ChannelRuleType.SOURCE, request.getSource()) &&
				// check status
				existsRuleWithValue(channelId, ChannelRuleType.STATUS, request.getStatus());
	}

	private boolean existsRuleWithValue(Long channelId, ChannelRuleType ruleType, Object value) {
		boolean result = false;

		List<ChannelRule> rules = findChannelRulesByType(channelId, ruleType);

		if (rules.isEmpty()) {
			result = true;
		} else {
			result = rules.stream().anyMatch(r -> r.getId().getValue().equals(value.toString()));
		}

		return result;
	}

	private Long forwardRequest(Long channelId, Request request, String groupName) {
		String message = RequestUtils.getRequestInfo(bot, groupName, request);

		SendMessage sendMessage = new SendMessage(channelId, message);
		sendMessage.parseMode(ParseMode.HTML);
		sendMessage.disableWebPagePreview(true);

		InlineKeyboardMarkup inlineKeyboard = getRequestKeyboard(request.getId().getGroupId(),
				request.getId().getMessageId());

		sendMessage.replyMarkup(inlineKeyboard);

		Long messageId = null;
		SendResponse sendResponse = bot.execute(sendMessage);
		if (sendResponse.isOk()) {
			messageId = sendResponse.message().messageId().longValue();

			LOGGER.debug("Request {} forwarded to channel {} successfully with messageId {}", request.getId(),
					channelId, messageId);
		} else {
			String responseDescription = sendResponse.description();
			LOGGER.error("Error forwarding request {} to channel {}: {}", request.getId(), channelId,
					responseDescription);
		}

		return messageId;
	}

	private InlineKeyboardMarkup getRequestKeyboard(Long groupId, Long messageId) {
		InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();

		InlineKeyboardButton requestButton = new InlineKeyboardButton("📚 Request")
				.url(TelegramUtils.getLink(groupId, messageId));
		InlineKeyboardButton actionsButton = new InlineKeyboardButton("⚙️ Actions")
				.url(RequestUtils.getActionsLink(configuration.getUsername(), messageId, groupId));

		inlineKeyboard.addRow(requestButton, actionsButton);

		return inlineKeyboard;
	}

	/**
	 * Delete a request forwarded to a channel<br>
	 * <b>It's highly recommended to call this method on a different thread</b>
	 *
	 * @param request Request to delete
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void deleteRequest(RequestPK requestId) {
		// find all channels with the request
		List<ChannelRequest> channelRequests = channelRequestService.findByRequest(requestId.getGroupId(),
				requestId.getMessageId());

		for (ChannelRequest channelRequest : channelRequests) {
			deleteChannelRequest(channelRequest);
		}
	}

	private void deleteChannelRequest(ChannelRequest channelRequest) {
		// delete message
		Long channelId = channelRequest.getId().getChannelId();
		Long messageId = channelRequest.getId().getMessageId();

		BaseResponse response = bot.execute(new DeleteMessage(channelId, messageId.intValue()));

		if (response.isOk()) {
			LOGGER.debug("Request {} deleted successfully", channelRequest.getId());
		} else {
			String responseDescription = response.description();
			LOGGER.error("Error deleting request {} from channel {} with errors {}", messageId, channelId,
					responseDescription);
		}

		// delete record
		channelRequestService.delete(channelId, messageId);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void deleteRequestByGroupId(Long groupId) {
		// find all channels with the request
		List<ChannelRequest> channelRequests = channelRequestService.findByGroupId(groupId);

		for (ChannelRequest channelRequest : channelRequests) {
			deleteChannelRequest(channelRequest);
		}
	}

}
