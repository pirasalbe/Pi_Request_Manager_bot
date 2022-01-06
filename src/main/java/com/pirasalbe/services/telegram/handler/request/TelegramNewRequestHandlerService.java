package com.pirasalbe.services.telegram.handler.request;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.telegram.TelegramHandlerResult;

/**
 * Service to manage requests from users
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramNewRequestHandlerService extends AbstractTelegramRequestHandlerService {

	@Override
	public boolean shouldHandle(Update update) {
		// messages with request
		return update.message() != null && hasRequestTag(update.message().text());
	}

	@Override
	public TelegramHandlerResult handleUpdate(Update update) {
		Message message = update.message();
		TelegramHandlerResult result = TelegramHandlerResult.noResponse();

		Long chatId = message.chat().id();

		// manage only requests from active groups
		Optional<Group> optional = groupService.findById(chatId);
		if (optional.isPresent()) {
			result = newRequest(message, chatId, optional.get());
		}

		return result;
	}

}
