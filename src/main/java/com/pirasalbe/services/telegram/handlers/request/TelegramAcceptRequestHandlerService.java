package com.pirasalbe.services.telegram.handlers.request;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pirasalbe.models.UserRole;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.utils.DateUtils;

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

	@Override
	protected Message getMessage(Update update) {
		return update.message().replyToMessage();
	}

	@Override
	public void handle(TelegramBot bot, Update update) {
		Message message = getRequestMessage(update);

		deleteMessage(bot, update.message());

		Long chatId = message.chat().id();

		LocalDateTime requestTime = DateUtils.integerToLocalDateTime(message.date());

		// manage only requests from active groups
		Optional<Group> optional = groupService.findById(chatId);
		if (optional.isPresent()) {
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

}
