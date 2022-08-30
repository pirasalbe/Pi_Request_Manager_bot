package com.pirasalbe.services.telegram;

import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import com.pirasalbe.configurations.TelegramConfiguration;
import com.pirasalbe.models.LogEvent;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.services.GroupService;
import com.pirasalbe.utils.RequestUtils;
import com.pirasalbe.utils.TelegramUtils;

/**
 * Service for logging events
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramLogService {

	private static final Logger LOGGER = LoggerFactory.getLogger(TelegramLogService.class);

	private DateTimeFormatter formatter;

	@Autowired
	protected TelegramConfiguration configuration;

	@Autowired
	private TelegramBotService telegramBotService;

	@Autowired
	private GroupService groupService;

	protected Queue<LogEvent> botQueue;

	protected TelegramLogService() {
		this.botQueue = new LinkedBlockingQueue<>();
		formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");
	}

	@Scheduled(fixedDelay = 5, timeUnit = TimeUnit.SECONDS)
	public void consumeQueues() {
		LogEvent event = botQueue.poll();

		if (event != null) {
			sendLog(telegramBotService.getBot(), event);

			LOGGER.info("Sent a bot log. {} left", botQueue.size());
		}
	}

	private void sendLog(TelegramBot bot, LogEvent log) {

		StringBuilder messageBuilder = new StringBuilder();

		messageBuilder.append("<b>Timestamp</b>: ").append(log.getTimestamp().format(formatter)).append("\n");

		Long groupId = log.getGroupId();
		Long userId = log.getUserId();
		if (userId != null && groupId != null) {
			String user = RequestUtils.getUser(bot, groupId, userId).trim();

			messageBuilder.append("<b>User</b>: ").append(user).append(" (<code>").append(userId).append("</code>)")
					.append("\n");

			messageBuilder.append("<b>Group</b>: ").append(getGroupName(groupId)).append(" (<code>").append(groupId)
					.append("</code>)").append("\n");
		}
		messageBuilder.append("<b>Reason</b>: ").append(log.getReason());
		if (log.getOriginalMessage() != null) {
			messageBuilder.append("\n\n<b>Original message</b>:\n").append(log.getOriginalMessage());
		}

		String message = messageBuilder.toString();

		SendMessage sendMessage = new SendMessage(configuration.getLogChat(), message);
		sendMessage.disableWebPagePreview(true);
		sendMessage.parseMode(ParseMode.HTML);

		SendResponse execute = bot.execute(sendMessage);

		if (!execute.isOk() && LOGGER.isErrorEnabled()) {
			LOGGER.error("Cannot log message:\n{}\nErrors: {}", message, execute.description());
		}
	}

	private String getGroupName(Long groupId) {
		String groupName = null;
		Optional<Group> optional = groupService.findById(groupId);

		if (optional.isPresent()) {
			groupName = optional.get().getName();
		} else {
			groupName = groupId.toString();
		}

		StringBuilder linkBuilder = new StringBuilder();
		linkBuilder.append("<a href='");
		linkBuilder.append(TelegramUtils.getLink(groupId, 1l));
		linkBuilder.append("'>");
		linkBuilder.append(groupName);
		linkBuilder.append("</a>");

		return linkBuilder.toString();
	}

	/**
	 * Add log to the queue
	 */
	public void log(LogEvent event) {
		botQueue.add(event);
	}

}
