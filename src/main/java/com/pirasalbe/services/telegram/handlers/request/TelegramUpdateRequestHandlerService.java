package com.pirasalbe.services.telegram.handlers.request;

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
public class TelegramUpdateRequestHandlerService extends AbstractTelegramRequestHandlerService {

	@Override
	public boolean shouldHandle(Update update) {
		// edit request
		return update.editedMessage() != null && hasRequestTag(update.editedMessage().text());
		// TODO manage updates and deletes
		// TODO manage responses from contributors
	}

	@Override
	public TelegramHandlerResult handleUpdate(Update update) {
		Message message = update.editedMessage();
		TelegramHandlerResult result = TelegramHandlerResult.noResponse();

		Long chatId = message.chat().id();

		// manage only requests from active groups
		Optional<Group> optional = groupService.findById(chatId);
		if (optional.isPresent()) {

			Long userId = message.from().id();
			String content = message.text();
			String link = getLink(content, message.entities());

			Group group = optional.get();

			// check if exists
			if (userRequestService.exists(message.messageId().longValue(), group.getId(), userId, link)) {
				// updateRequest
				result = updateRequest(message, chatId, group, content, link);
			} else {
				// create new request
				result = newRequest(message, chatId, group, content, link);
			}
		}

		return result;
	}

	private TelegramHandlerResult updateRequest(Message message, Long chatId, Group group, String content,
			String link) {
		TelegramHandlerResult result = TelegramHandlerResult.noResponse();

		// TODO check if the user is the creator
		// TODO True -> update request

		return result;
	}

}
