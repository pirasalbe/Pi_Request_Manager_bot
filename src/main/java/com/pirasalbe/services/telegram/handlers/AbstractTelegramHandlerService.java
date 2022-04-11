package com.pirasalbe.services.telegram.handlers;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import com.pirasalbe.configurations.TelegramConfiguration;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.database.Request;
import com.pirasalbe.services.GroupService;
import com.pirasalbe.services.SchedulerService;
import com.pirasalbe.services.telegram.TelegramBotService;
import com.pirasalbe.utils.DateUtils;
import com.pirasalbe.utils.RequestUtils;
import com.pirasalbe.utils.TelegramConditionUtils;
import com.pirasalbe.utils.TelegramUtils;

/**
 * Service with common methods
 *
 * @author pirasalbe
 *
 */
@Component
public class AbstractTelegramHandlerService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTelegramHandlerService.class);

	@Autowired
	protected TelegramConfiguration configuration;

	@Autowired
	protected SchedulerService schedulerService;

	@Autowired
	protected GroupService groupService;

	@Autowired
	private TelegramBotService telegramBotService;

	protected Queue<Consumer<TelegramBot>> botQueue;

	protected AbstractTelegramHandlerService() {
		this.botQueue = new LinkedBlockingQueue<>();
	}

	@Scheduled(fixedDelay = 2, timeUnit = TimeUnit.SECONDS)
	public void consumeQueues() {
		Consumer<TelegramBot> consumer = botQueue.poll();

		if (consumer != null) {
			consumer.accept(telegramBotService.getBot());

			LOGGER.info("Executed a bot operation. {} left", botQueue.size());
		}
	}

	/**
	 * Delete a message
	 *
	 * @param bot     Bot to delete the message
	 * @param message Message to delete
	 * @param delete  True to delete the message
	 */
	protected void deleteMessage(TelegramBot bot, Message message, boolean delete) {
		if (delete) {
			deleteMessage(bot, message);
		}
	}

	/**
	 * Delete a message
	 *
	 * @param bot     Bot to delete the message
	 * @param message Message to delete
	 */
	protected void deleteMessage(TelegramBot bot, Message message) {
		bot.execute(new DeleteMessage(message.chat().id(), message.messageId()));
	}

	/**
	 * Send a message and delete it after a timeout
	 *
	 * @param bot         Bot to send the message
	 * @param sendMessage Message to send
	 * @param timeout     Timeout before deleting the request
	 * @param timeUnit    Unit of the timeout
	 */
	protected void sendMessageAndDelete(TelegramBot bot, SendMessage sendMessage, long timeout, TimeUnit timeUnit) {
		sendMessageAndDelete(bot, sendMessage, timeout, timeUnit, true);
	}

	/**
	 * Send a message and optionally delete it after a timeout
	 *
	 * @param bot         Bot to send the message
	 * @param sendMessage Message to send
	 * @param timeout     Timeout before deleting the request
	 * @param timeUnit    Unit of the timeout
	 * @param delete      True to delete the message
	 */
	protected void sendMessageAndDelete(TelegramBot bot, SendMessage sendMessage, long timeout, TimeUnit timeUnit,
			boolean delete) {

		SendResponse response = bot.execute(sendMessage);

		// schedule delete
		if (delete) {
			schedulerService.schedule(
					(b, r) -> b.execute(new DeleteMessage(r.message().chat().id(), r.message().messageId())), response,
					timeout, timeUnit);
		}
	}

	protected Optional<Long> getGroup(Long chatId, String text, boolean isPrivate) {
		Optional<Long> group;

		if (isPrivate) {
			// get requests in PM
			group = TelegramConditionUtils.getGroupId(text);
		} else {
			// get requests of the group
			group = Optional.of(chatId);
		}

		return group;
	}

	protected void sendRequestList(Long chatId, Optional<Long> group, String title, List<Request> requests,
			boolean actions) {
		StringBuilder builder = new StringBuilder(title);

		// chat name, when in PM
		Map<Long, String> chatNames = new HashMap<>();
		LocalDateTime now = DateUtils.getNow();
		boolean deleteMessages = group.isPresent();

		// create text foreach request
		for (int i = 0; i < requests.size(); i++) {
			Request request = requests.get(i);

			String requestText = getRequestText(request, i, chatNames, now, actions);

			// if length is > message limit, send current text
			if (builder.length() + requestText.length() > 4096) {
				sendRequestListMessage(chatId, builder.toString(), deleteMessages);
				builder = new StringBuilder(title);
			}
			builder.append(requestText);
			// send last message
			if (i == requests.size() - 1) {
				sendRequestListMessage(chatId, builder.toString(), deleteMessages);
			}
		}
	}

	protected String getRequestText(Request request, int i, Map<Long, String> chatNames, LocalDateTime now,
			boolean actions) {
		// build request text
		StringBuilder requestBuilder = new StringBuilder();
		Long messageId = request.getId().getMessageId();
		Long groupId = request.getId().getGroupId();

		// request link
		requestBuilder.append("<a href='").append(TelegramUtils.getLink(groupId.toString(), messageId.toString()))
				.append("'>");

		requestBuilder.append(i + 1).append(" ");
		requestBuilder.append(getChatName(chatNames, groupId)).append("</a> ");

		// request date
		requestBuilder.append(RequestUtils.getTimeBetweenDates(request.getRequestDate(), now, true)).append(" ago ");

		// request tags
		requestBuilder.append("#").append(request.getFormat().name().toLowerCase()).append(" #")
				.append(request.getSource().name().toLowerCase()).append(" #").append(request.getOtherTags());

		requestBuilder.append(" ");

		// request actions
		if (actions) {
			requestBuilder.append("[<a href='")
					.append(RequestUtils.getActionsLink(configuration.getUsername(), messageId, groupId))
					.append("'>Actions</a> for <code>").append(messageId).append("</code>]");
		}

		requestBuilder.append("\n");

		return requestBuilder.toString();
	}

	private void sendRequestListMessage(Long chatId, String message, boolean deleteMessages) {
		SendMessage sendMessage = new SendMessage(chatId, message);
		sendMessage.parseMode(ParseMode.HTML);
		sendMessage.disableWebPagePreview(true);

		botQueue.add(b -> sendMessageAndDelete(b, sendMessage, 5, TimeUnit.MINUTES, deleteMessages));
	}

	private String getChatName(Map<Long, String> chatNames, Long groupId) {
		String chatName = null;
		if (chatNames.containsKey(groupId)) {
			chatName = chatNames.get(groupId);
		} else {
			Optional<Group> optional = groupService.findById(groupId);
			if (optional.isPresent()) {
				chatName = optional.get().getName();
				chatNames.put(groupId, chatName);
			} else {
				chatName = "Unknown";
			}
		}

		return chatName;
	}

}
