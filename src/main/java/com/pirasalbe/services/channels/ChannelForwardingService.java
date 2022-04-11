package com.pirasalbe.services.channels;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Scheduled;
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
import com.pirasalbe.models.SyncRequest;
import com.pirasalbe.models.database.Channel;
import com.pirasalbe.models.database.ChannelRequest;
import com.pirasalbe.models.database.ChannelRule;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.database.Request;
import com.pirasalbe.models.database.RequestPK;
import com.pirasalbe.services.GroupService;
import com.pirasalbe.services.RequestManagementService;
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

	@Autowired
	private TelegramConfiguration configuration;

	@Autowired
	private ChannelService channelService;

	@Autowired
	private ChannelRuleService channelRuleService;

	@Autowired
	private ChannelRequestService channelRequestService;

	@Autowired
	private ChannelForwardingQueueService channelForwardingQueueService;

	@Autowired
	private GroupService groupService;

	@Autowired
	private RequestManagementService requestManagementService;

	private TelegramBot bot;

	@Autowired
	private TelegramUserBotService userBotService;

	public ChannelForwardingService(TelegramBotService telegramBotService) {
		this.bot = telegramBotService.getBot();
	}

	@Scheduled(fixedDelay = 5, timeUnit = TimeUnit.SECONDS)
	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void consumeQueues() {
		boolean consumed = consumeForwardQueue();

		if (!consumed) {
			consumed = consumeDeleteQueue();
		}

		if (!consumed) {
			consumed = consumeDeleteByGroupIdQueue();
		}

		if (!consumed) {
			consumed = consumeSyncQueue();
		}

		if (consumed) {

			int syncQueueSize = channelForwardingQueueService.syncQueueSize();
			int forwardQueueSize = channelForwardingQueueService.forwardQueueSize();
			int deleteQueueSize = channelForwardingQueueService.deleteQueueSize();
			int deleteByGroupIdQueueSize = channelForwardingQueueService.deleteByGroupIdQueueSize();

			LOGGER.info("Managed a request. {} to sync, {} to forward, {} to delete, {} to delete by groupId",
					syncQueueSize, forwardQueueSize, deleteQueueSize, deleteByGroupIdQueueSize);
		}
	}

	private boolean consumeForwardQueue() {
		boolean consumed = false;

		RequestPK forward = channelForwardingQueueService.pollForwardQueue();

		if (forward != null) {
			consumed = true;
			forwardRequest(forward);
		}

		return consumed;
	}

	private boolean consumeDeleteQueue() {
		boolean consumed = false;

		RequestPK delete = channelForwardingQueueService.pollDeleteQueue();

		if (delete != null) {
			consumed = true;
			deleteForwardedRequest(delete);
		}

		return consumed;
	}

	private boolean consumeDeleteByGroupIdQueue() {
		boolean consumed = false;

		Long delete = channelForwardingQueueService.pollDeleteByGroupIdQueue();

		if (delete != null) {
			consumed = true;
			deleteForwardedRequestsByGroupId(delete);
		}

		return consumed;
	}

	private boolean consumeSyncQueue() {
		boolean consumed = false;

		SyncRequest syncRequest = channelForwardingQueueService.pollSyncQueue();

		if (syncRequest != null) {
			consumed = true;
			syncRequest(syncRequest);
		}

		return consumed;
	}

	private void forwardRequest(RequestPK requestId) {
		Optional<Request> optional = requestManagementService.findById(requestId);

		if (optional.isPresent()) {
			Optional<Group> group = groupService.findById(requestId.getGroupId());

			// delete request from all channels
			deleteForwardedRequest(requestId);

			// forward request to the right channels
			forwardRequestToChannels(optional.get(), getGroupName(group));
		}
	}

	private String getGroupName(Optional<Group> group) {
		String groupName = null;

		if (group.isPresent()) {
			groupName = group.get().getName();
		} else {
			groupName = "unknown";
		}

		return groupName;
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

	private void deleteForwardedRequest(RequestPK requestId) {
		// find all channels with the request
		List<ChannelRequest> channelRequests = channelRequestService.findByRequest(requestId.getGroupId(),
				requestId.getMessageId());

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

		TelegramUtils.cooldown(2000);
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

	private void deleteForwardedRequestsByGroupId(Long groupId) {
		// find all request of a group
		List<ChannelRequest> channelRequests = channelRequestService.findByGroupId(groupId);

		deleteChannelRequests(channelRequests);
	}

	public void refreshChannel(Long channelId) {
		LOGGER.info("Refresh {} started", channelId);

		int page = 0;
		int size = 100;
		boolean keep = true;

		// groups list to map
		List<Group> groups = groupService.findAll();
		Map<Long, String> groupNames = groups.stream().collect(Collectors.toMap(Group::getId, Group::getName));

		// get all requests paginated
		while (keep) {
			LOGGER.info("Refresh {} page {}", channelId, page);
			Page<Request> requestPage = requestManagementService.findAll(page, size);

			// forward all requests
			List<Request> requests = requestPage.toList();

			for (Request request : requests) {
				String groupName = groupNames.get(request.getId().getGroupId());

				channelForwardingQueueService.syncRequest(new SyncRequest(channelId, request.getId(), groupName));
			}

			keep = requestPage.hasNext();
			page++;
		}

		LOGGER.info("Refresh {} ended", channelId);
	}

	private void syncRequest(SyncRequest syncRequest) {
		Optional<Request> optional = requestManagementService.findById(syncRequest.getRequestId());

		if (optional.isPresent()) {
			syncRequest(optional.get(), syncRequest.getGroupName(), syncRequest.getChannelId());
		}
	}

	/**
	 * Send a request updated to a channel<br>
	 * <b>It's highly recommended to call this method on a different thread</b>
	 *
	 * @param request   Request to update
	 * @param groupName Name of the group of the request
	 * @return True if the message was forwarded
	 */
	private void syncRequest(Request request, String groupName, Long channelId) {
		// delete request from the channel
		deleteRequestIfNotMatching(request, channelId);

		// forward request to the channel
		forwardRequestToChannel(request, groupName, channelId);
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
