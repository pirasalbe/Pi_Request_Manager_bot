package com.pirasalbe.services.telegram.handlers.command;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.MessageEntity;
import com.pengrad.telegrambot.model.MessageEntity.Type;
import com.pengrad.telegrambot.model.Update;
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

	public static final String COMMAND_PENDING = "/pending";
	public static final String COMMAND_DONE = "/done";
	public static final String COMMAND_SILENT_DONE = "/sdone";

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

	public TelegramCondition replyToMessageCondition() {
		return this::replyToMessage;
	}

	private boolean replyToMessage(Update update) {
		return update.message() != null && update.message().replyToMessage() != null;
	}

	public TelegramHandler markPending() {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);

			Optional<Group> optional = groupService.findById(chatId);

			if (optional.isPresent()) {
				Message message = update.message().replyToMessage();

				boolean success = requestManagementService.markPending(message);

				// send a message to notify operation
				String link = getLink(message);
				StringBuilder stringBuilder = new StringBuilder();
				stringBuilder.append(requestStatusMessage(link, success, "marked as pending"));
				SendMessage sendMessage = new SendMessage(chatId, stringBuilder.toString());
				sendMessage.parseMode(ParseMode.HTML);
				SendResponse response = bot.execute(sendMessage);

				// schedule delete
				schedulerService.schedule((b, r) -> b.execute(new DeleteMessage(chatId, r.message().messageId())),
						response, 5, TimeUnit.SECONDS);

				// delete command
				DeleteMessage deleteMessage = new DeleteMessage(chatId, update.message().messageId());
				bot.execute(deleteMessage);
			}
		};
	}

	private String requestStatusMessage(String link, boolean success, String successMessage) {
		StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append("<a href='");
		stringBuilder.append(link);
		stringBuilder.append("'>Request</a>");
		if (success) {
			stringBuilder.append(" ").append(successMessage);
		} else {
			stringBuilder.append(" not found");
		}

		return stringBuilder.toString();
	}

	public TelegramHandler markDone() {
		return markDone(true);
	}

	public TelegramHandler markDoneSilently() {
		return markDone(false);
	}

	private TelegramHandler markDone(boolean reply) {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);

			Optional<Group> optional = groupService.findById(chatId);

			if (optional.isPresent()) {
				Message message = update.message().replyToMessage();

				boolean success = requestManagementService.markDone(message);

				// send a message to notify operation
				String link = getLink(message);
				StringBuilder stringBuilder = new StringBuilder();
				if (reply) {
					// reply to the user only if reply
					String text = removeDoneCommand(update.message().text(), update.message().entities()).trim();
					stringBuilder.append(TelegramUtils.tagUser(message));
					stringBuilder.append(" ").append(text).append("\n");
				}
				stringBuilder.append(requestStatusMessage(link, success, "marked as done"));
				SendMessage sendMessage = new SendMessage(chatId, stringBuilder.toString());
				if (reply) {
					sendMessage.replyToMessageId(message.messageId());
				}
				sendMessage.parseMode(ParseMode.HTML);
				SendResponse response = bot.execute(sendMessage);

				// schedule delete for no reply
				if (!reply) {
					schedulerService.schedule((b, r) -> b.execute(new DeleteMessage(chatId, r.message().messageId())),
							response, 5, TimeUnit.SECONDS);
				}

				// delete command
				DeleteMessage deleteMessage = new DeleteMessage(chatId, update.message().messageId());
				bot.execute(deleteMessage);
			}
		};
	}

	private String removeDoneCommand(String text, MessageEntity[] entities) {
		StringBuilder builder = new StringBuilder();

		if (entities != null) {
			for (int i = 0; i < entities.length && builder.length() == 0; i++) {
				MessageEntity entity = entities[i];
				if (entity.type() == Type.bot_command) {
					Integer offset = entity.offset();
					builder.append(text.substring(0, offset));
					builder.append(text.substring(offset + entity.length()));
				}
			}
		}

		return builder.toString();
	}

	public TelegramCondition replyToMessageWithFileCondition() {
		return update -> replyToMessage(update)
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
				stringBuilder.append(requestStatusMessage(link, success, "marked as done"));
				SendMessage sendMessage = new SendMessage(chatId, stringBuilder.toString());
				sendMessage.parseMode(ParseMode.HTML);

				SendResponse response = bot.execute(sendMessage);

				// schedule delete
				schedulerService.schedule((b, r) -> b.execute(new DeleteMessage(chatId, r.message().messageId())),
						response, 5, TimeUnit.SECONDS);
			}
		};
	}

}
