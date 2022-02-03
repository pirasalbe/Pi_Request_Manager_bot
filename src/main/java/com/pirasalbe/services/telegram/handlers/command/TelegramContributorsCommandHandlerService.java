package com.pirasalbe.services.telegram.handlers.command;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import com.pirasalbe.models.UserRole;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.telegram.handlers.TelegramCondition;
import com.pirasalbe.models.telegram.handlers.TelegramHandler;
import com.pirasalbe.services.GroupService;
import com.pirasalbe.services.RequestManagementService;
import com.pirasalbe.services.SchedulerService;
import com.pirasalbe.utils.TelegramUtils;

/**
 * Service to manage contributors related commands
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramContributorsCommandHandlerService {

	public static final String COMMAND_DONE = "/done";
	public static final String COMMAND_PENDING = "/pending";

	public static final UserRole ROLE = UserRole.CONTRIBUTOR;

	@Autowired
	private GroupService groupService;

	@Autowired
	private RequestManagementService requestManagementService;

	@Autowired
	private SchedulerService schedulerService;

	private String getLink(Message message) {
		String chatId = message.chat().id().toString();
		String messageId = message.messageId().toString();
		if (chatId.startsWith("-100")) {
			chatId = chatId.substring(4);
		}

		return "https://t.me/c/" + chatId + "/" + messageId;
	}

	public TelegramCondition markDoneCondition() {
		return update -> update.message() != null && update.message().replyToMessage() != null;
	}

	public TelegramHandler markDone() {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);

			Optional<Group> optional = groupService.findById(chatId);

			if (optional.isPresent()) {
				Message message = update.message().replyToMessage();
				String text = update.message().text().substring(COMMAND_DONE.length()).trim();

				boolean success = requestManagementService.markDone(message);

				String link = getLink(message);
				StringBuilder stringBuilder = new StringBuilder();
				stringBuilder.append(TelegramUtils.tagUser(message));
				stringBuilder.append(" ").append(text).append("\n");
				stringBuilder.append("<a href='");
				stringBuilder.append(link);
				stringBuilder.append("'>Request</a>");
				if (success) {
					stringBuilder.append(" marked as done");
				} else {
					stringBuilder.append(" not found");
				}
				SendMessage sendMessage = new SendMessage(chatId, stringBuilder.toString());
				sendMessage.replyToMessageId(message.messageId());
				sendMessage.parseMode(ParseMode.HTML);
				DeleteMessage deleteMessage = new DeleteMessage(chatId, update.message().messageId());

				bot.execute(sendMessage);
				bot.execute(deleteMessage);
			}
		};
	}

	public TelegramCondition markDoneWithFileCondition() {
		return update -> update.message() != null && update.message().replyToMessage() != null
				&& (update.message().document() != null || update.message().audio() != null);
	}

	public TelegramHandler markDoneWithFile() {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);

			Optional<Group> optional = groupService.findById(chatId);

			if (optional.isPresent()) {
				Message message = update.message().replyToMessage();

				boolean success = requestManagementService.markDone(message);

				String link = getLink(message);
				StringBuilder stringBuilder = new StringBuilder();
				stringBuilder.append("<a href='");
				stringBuilder.append(link);
				stringBuilder.append("'>Request</a>");
				if (success) {
					stringBuilder.append(" marked as done");
				} else {
					stringBuilder.append(" not found");
				}
				SendMessage sendMessage = new SendMessage(chatId, stringBuilder.toString());
				sendMessage.replyToMessageId(update.message().messageId());
				sendMessage.parseMode(ParseMode.HTML);

				SendResponse response = bot.execute(sendMessage);

				// schedule delete
				// TODO fix
				schedulerService.schedule(() -> bot.execute(new DeleteMessage(chatId, response.message().messageId())),
						1, TimeUnit.SECONDS);
			}
		};
	}

}
