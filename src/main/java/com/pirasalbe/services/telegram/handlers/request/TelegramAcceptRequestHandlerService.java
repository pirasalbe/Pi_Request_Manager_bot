package com.pirasalbe.services.telegram.handlers.request;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import com.pirasalbe.models.UserRole;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.services.SchedulerService;
import com.pirasalbe.utils.DateUtils;
import com.pirasalbe.utils.TelegramUtils;

/**
 * Service to manage requests from users
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramAcceptRequestHandlerService extends AbstractTelegramRequestHandlerService {

	public static final String COMMAND = "/accept_request";

	public static final UserRole ROLE = UserRole.CONTRIBUTOR;

	@Autowired
	private SchedulerService schedulerService;

	@Override
	protected Message getMessage(Update update) {
		return update.message().replyToMessage();
	}

	@Override
	public void handle(TelegramBot bot, Update update) {
		Message message = getRequestMessage(update);

		Long chatId = message.chat().id();

		deleteMessage(bot, update.message());

		LocalDateTime requestTime = DateUtils.integerToLocalDateTime(message.date());

		// manage only requests from active groups
		Optional<Group> optional = groupService.findById(chatId);
		if (optional.isPresent()) {
			sendAcceptNotification(bot, message, chatId);

			newRequest(bot, message, chatId, message.messageId(), requestTime, optional.get(), true);
		}
	}

	/**
	 * Delete a message
	 *
	 * @param bot     Bot to delete the message
	 * @param message Message to delete
	 */
	private void deleteMessage(TelegramBot bot, Message message) {
		bot.execute(new DeleteMessage(message.chat().id(), message.messageId()));
	}

	private void sendAcceptNotification(TelegramBot bot, Message message, Long chatId) {
		StringBuilder acceptBuilder = new StringBuilder();
		acceptBuilder.append(TelegramUtils.tagUser(message));
		acceptBuilder.append("Your <a href='");
		acceptBuilder.append(TelegramUtils.getLink(message));
		acceptBuilder.append("'>request</a> has been accepted.");

		SendMessage sendMessage = new SendMessage(chatId, acceptBuilder.toString());
		sendMessage.replyToMessageId(message.messageId());
		sendMessage.parseMode(ParseMode.HTML);

		SendResponse sendResponse = bot.execute(sendMessage);
		if (sendResponse.isOk()) {
			schedulerService.schedule(
					(b, r) -> b.execute(new DeleteMessage(r.message().chat().id(), r.message().messageId())),
					sendResponse, 10, TimeUnit.SECONDS);
		}
	}

}
