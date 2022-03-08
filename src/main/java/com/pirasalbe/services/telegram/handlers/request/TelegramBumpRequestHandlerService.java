package com.pirasalbe.services.telegram.handlers.request;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.utils.DateUtils;

/**
 * Service to manage requests from users
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramBumpRequestHandlerService extends AbstractTelegramRequestHandlerService {

	@Override
	protected Message getMessage(Update update) {
		Message message = null;

		if (update.message() != null) {
			message = update.message();
		} else if (update.editedMessage() != null) {
			message = update.editedMessage();
		}

		return message;
	}

	@Override
	protected Message getRequestMessage(Update update) {
		Message message = null;

		Message updateMessage = getMessage(update);

		// reply to the same user request with a bump keyword
		if (updateMessage != null && updateMessage.replyToMessage() != null
				&& updateMessage.from().id().equals(updateMessage.replyToMessage().from().id())
				&& hasRequestTag(updateMessage.replyToMessage().text()) && isBump(updateMessage.text())) {
			message = updateMessage.replyToMessage();
		}

		return message;
	}

	@Override
	public void handle(TelegramBot bot, Update update) {
		Message requestMessage = getRequestMessage(update);
		Message message = getMessage(update);

		Long chatId = requestMessage.chat().id();

		LocalDateTime requestTime = DateUtils.integerToLocalDateTime(message.date());

		// manage only requests from active groups
		Optional<Group> optional = groupService.findById(chatId);
		if (optional.isPresent()) {
			newRequest(bot, requestMessage, chatId, message.messageId(), requestTime, optional.get());
		}
	}

}
