package com.pirasalbe.services.telegram.handlers.request;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pirasalbe.models.UpdateRequestAction;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.request.Format;
import com.pirasalbe.utils.DateUtils;
import com.pirasalbe.utils.RequestUtils;

/**
 * Service to manage requests from users
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramUpdateRequestHandlerService extends AbstractTelegramRequestHandlerService {

	@Override
	protected Message getMessage(Update update) {
		return update.editedMessage();
	}

	@Override
	public void handle(TelegramBot bot, Update update) {
		Message message = getRequestMessage(update);

		Long chatId = message.chat().id();

		// manage only requests from active groups
		Optional<Group> optional = groupService.findById(chatId);
		if (optional.isPresent()) {

			Long userId = message.from().id();
			String content = RequestUtils.getContent(message.text(), message.entities());
			String link = RequestUtils.getLink(message.text(), message.entities());

			if (link != null) {
				Group group = optional.get();

				// check association
				UpdateRequestAction requestAssociationInfo = requestManagementService
						.getUpdateRequestAction(message.messageId().longValue(), group.getId(), userId, link);

				if (requestAssociationInfo == UpdateRequestAction.UPDATE_REQUEST) {
					// request exists and user is creator
					updateRequest(message, group, content, link);
				} else if (requestAssociationInfo == UpdateRequestAction.NEW_REQUEST) {
					// request may or may not exists
					LocalDateTime requestTime = DateUtils.integerToLocalDateTime(message.editDate());

					newRequest(bot, message, chatId, message.messageId(), requestTime, group, content, link);
				}
			} else {
				manageIncompleteRequest(bot, message, chatId);
			}
		}
	}

	private void updateRequest(Message message, Group group, String content, String link) {
		Format format = getFormat(content);

		requestManagementService.updateRequest(message.messageId().longValue(), group, link, content, format,
				getSource(content, format), getOtherTags(content));
	}

}
