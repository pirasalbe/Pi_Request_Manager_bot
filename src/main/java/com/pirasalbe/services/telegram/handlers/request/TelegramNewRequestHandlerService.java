package com.pirasalbe.services.telegram.handlers.request;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.telegram.handlers.TelegramCondition;
import com.pirasalbe.utils.DateUtils;

/**
 * Service to manage requests from users
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramNewRequestHandlerService extends AbstractTelegramRequestHandlerService {

	public TelegramCondition geCondition() {
		// messages with request
		return update -> update.message() != null && hasRequestTag(update.message().text());
	}

	@Override
	public void handle(TelegramBot bot, Update update) {
		Message message = update.message();
		Long chatId = message.chat().id();

		LocalDateTime requestTime = DateUtils.integerToLocalDateTime(message.date());

		// manage only requests from active groups
		Optional<Group> optional = groupService.findById(chatId);
		if (optional.isPresent()) {
			newRequest(bot, message, chatId, requestTime, optional.get());
		}
	}

}
