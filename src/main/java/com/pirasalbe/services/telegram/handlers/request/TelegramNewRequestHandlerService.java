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
public class TelegramNewRequestHandlerService extends AbstractTelegramRequestHandlerService {

	@Override
	protected Message getMessage(Update update) {
		return update.message();
	}

	@Override
	public void handle(TelegramBot bot, Update update) {
		Message message = getRequestMessage(update);

		Long chatId = message.chat().id();

		LocalDateTime requestTime = DateUtils.integerToLocalDateTime(message.date());

		// manage only requests from active groups
		Optional<Group> optional = groupService.findById(chatId);
		if (optional.isPresent()) {
			newRequest(bot, message, chatId, message.messageId(), requestTime, optional.get());
		}
	}

}
