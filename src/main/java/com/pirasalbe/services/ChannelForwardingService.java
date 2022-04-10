package com.pirasalbe.services;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pengrad.telegrambot.TelegramBot;
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
import com.pirasalbe.services.telegram.TelegramUserBotService;
import com.pirasalbe.utils.RequestUtils;
import com.pirasalbe.utils.TelegramUtils;

import it.tdlight.client.Result;
import it.tdlight.jni.TdApi;
import it.tdlight.jni.TdApi.MessageLinkInfo;
import it.tdlight.jni.TdApi.Ok;

/**
 * Service that manages the channels
 *
 * @author pirasalbe
 *
 */
@Component
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class ChannelForwardingService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChannelForwardingService.class);

	private Lock lock;

	@Autowired
	private TelegramConfiguration configuration;

	@Autowired
	private ChannelService channelService;

	@Autowired
	private ChannelRuleService channelRuleService;

	@Autowired
	private ChannelRequestService channelRequestService;

	private TelegramBot bot;

	@Autowired
	private TelegramUserBotService userBotService;

	public ChannelForwardingService(TelegramBotService telegramBotService) {
		this.bot = telegramBotService.getBot();
		this.lock = new ReentrantLock();
	}

	/**
	 * Send a request updated to the channels<br>
	 * <b>It's highly recommended to call this method on a different thread</b>
	 *
	 * @param request   Request to update
	 * @param groupName Name of the group of the request
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void forwardRequest(Request request, String groupName) {
		lock.lock();

		try {
			// delete request from all channels
			deleteForwardedRequest(request.getId().getGroupId(), request.getId().getMessageId());

			// forward request to the right channels
			forwardRequestToChannels(request, groupName);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Send a request to a channel<br>
	 * <b>It's highly recommended to call this method on a different thread</b>
	 *
	 * @param request   Request to forward
	 * @param groupName Name of the group of the request
	 */
	private void forwardRequestToChannels(Request request, String groupName) {
		List<Channel> channels = channelService.findAll();

		for (Channel channel : channels) {
			forwardRequestToChannel(request, groupName, channel.getId());
		}
	}

	private boolean forwardRequestToChannel(Request request, String groupName, Long channelId) {
		boolean forwarded = false;

		// check that the request matches with the channel rules
		if (requestMatchRules(channelId, request)) {

			// check unique key
			ChannelRequest channelRequest = channelRequestService.findByUniqueKey(channelId,
					request.getId().getGroupId(), request.getId().getMessageId());

			// send request if doesn't exists
			if (channelRequest == null) {
				// forward request
				Long messageId = forwardRequest(channelId, request, groupName);

				// update channel request table
				if (messageId != null) {
					channelRequestService.insert(channelId, messageId, request.getId());
					forwarded = true;
				}

				TelegramUtils.cooldown(2000);
			}
		}

		return forwarded;
	}

	private boolean requestMatchRules(Long channelId, Request request) {

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

		List<ChannelRule> rules = channelRuleService.findByChannelIdAndType(channelId, ruleType);

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

		InlineKeyboardMarkup inlineKeyboard = RequestUtils.getRequestKeyboard(configuration.getUsername(),
				request.getId().getGroupId(), request.getId().getMessageId(), request.getStatus(), "⚙️ Actions in PM");

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

	/**
	 * Delete a request forwarded to a channel<br>
	 * <b>It's highly recommended to call this method on a different thread</b>
	 *
	 * @param request Request to delete
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void deleteForwardedRequest(RequestPK requestId) {
		lock.lock();

		try {
			deleteForwardedRequest(requestId.getGroupId(), requestId.getMessageId());
		} finally {
			lock.unlock();
		}
	}

	private void deleteForwardedRequest(Long groupId, Long messageId) {
		// find all channels with the request
		List<ChannelRequest> channelRequests = channelRequestService.findByRequest(groupId, messageId);

		deleteChannelRequests(channelRequests);
	}

	private void deleteChannelRequests(List<ChannelRequest> channelRequests) {
		for (ChannelRequest channelRequest : channelRequests) {
			deleteChannelRequest(channelRequest);
		}
	}

	private void deleteChannelRequest(ChannelRequest channelRequest) {
		// delete message
		Long channelId = channelRequest.getId().getChannelId();
		Long messageId = channelRequest.getId().getMessageId();

		// get tdlib message id
		deleteMessageWithUserBot(channelId, messageId);

		// delete record
		channelRequestService.delete(channelId, messageId);
	}

	private void deleteMessageWithUserBot(Long channelId, Long messageId) {
		Result<MessageLinkInfo> messageInfo = userBotService.getMessageId(channelId, messageId);

		if (messageInfo.isError()) {
			LOGGER.error("Cannot get userBot messageId for request {} from channel {} with errors {}", messageId,
					channelId, messageInfo.getError());
			deleteMessageWithBot(channelId, messageId);
		} else if (messageInfo.get().message != null && messageInfo.get().message.canBeDeletedForAllUsers) {
			// read the id
			long id = messageInfo.get().message.id;

			// delete message with userbot
			Result<Ok> deleteResult = userBotService
					.sendSync(new TdApi.DeleteMessages(channelId, new long[] { id }, true));

			if (deleteResult.isError()) {
				LOGGER.error("Error deleting request {} with userbot from channel {} with errors {}", messageId,
						channelId, deleteResult.getError());

				deleteMessageWithBot(channelId, messageId);
			} else {
				LOGGER.debug("Request with messageId {} with userBot in channel {} deleted successfully", messageId,
						channelId);
			}
		} else {
			LOGGER.error(
					"Error deleting request {} with userbot from channel {} because not allowed to delete for all users",
					messageId, channelId);

			deleteMessageWithBot(channelId, messageId);
		}
	}

	private void deleteMessageWithBot(Long channelId, Long messageId) {
		BaseResponse response = bot.execute(new DeleteMessage(channelId, messageId.intValue()));

		if (response.isOk()) {
			LOGGER.debug("Request with messageId {} in channel {} deleted successfully", messageId, channelId);
		} else {
			String responseDescription = response.description();
			LOGGER.error("Error deleting request {} from channel {} with errors {}", messageId, channelId,
					responseDescription);
		}
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void deleteForwardedRequestsByGroupId(Long groupId) {
		// find all request of a group
		List<ChannelRequest> channelRequests = channelRequestService.findByGroupId(groupId);

		deleteChannelRequests(channelRequests);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void deleteForwardedRequestsByChannelId(Long channelId) {
		// find all channels requests
		List<ChannelRequest> channelRequests = channelRequestService.findByChannelId(channelId);

		deleteChannelRequests(channelRequests);
	}

	/**
	 * Send a request updated to a channel<br>
	 * <b>It's highly recommended to call this method on a different thread</b>
	 *
	 * @param request   Request to update
	 * @param groupName Name of the group of the request
	 * @return True if the message was forwarded
	 */
	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public boolean syncRequest(Request request, String groupName, Long channelId) {
		// delete request from the channel
		boolean delete = deleteRequestIfNotMatching(request, channelId);

		// forward request to the channel
		boolean forward = forwardRequestToChannel(request, groupName, channelId);

		return delete || forward;
	}

	private boolean deleteRequestIfNotMatching(Request request, Long channelId) {
		boolean result = false;

		if (!requestMatchRules(channelId, request)) {
			RequestPK requestId = request.getId();
			ChannelRequest channelRequest = channelRequestService.findByUniqueKey(channelId, requestId.getGroupId(),
					requestId.getMessageId());

			if (channelRequest != null) {
				deleteChannelRequest(channelRequest);
				result = true;
			}
		}

		return result;
	}

}
