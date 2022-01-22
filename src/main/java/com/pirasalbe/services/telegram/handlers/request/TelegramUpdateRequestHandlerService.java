package com.pirasalbe.services.telegram.handlers.request;

import java.time.LocalDateTime;
import java.util.Optional;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pirasalbe.models.RequestAssociationInfo;
import com.pirasalbe.models.RequestAssociationInfo.Association;
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
public class TelegramUpdateRequestHandlerService extends AbstractTelegramRequestHandlerService {

	public TelegramCondition geCondition() {
		// edit request
		return update -> update.editedMessage() != null && hasRequestTag(update.editedMessage().text());
	}

	@Override
	public void handle(TelegramBot bot, Update update) {
		Message message = update.editedMessage();

		Long chatId = message.chat().id();
		LocalDateTime requestTime = DateUtils.integerToLocalDateTime(message.editDate());

		// manage only requests from active groups
		Optional<Group> optional = groupService.findById(chatId);
		if (optional.isPresent()) {

			Long userId = message.from().id();
			String content = message.text();
			String link = getLink(content, message.entities());

			Group group = optional.get();

			// check association
			RequestAssociationInfo requestAssociationInfo = requestManagementService
					.getRequestAssociationInfo(message.messageId().longValue(), group.getId(), userId, link);
			if (requestAssociationInfo.requestExists()
					&& requestAssociationInfo.getAssociation() == Association.CREATOR) {
				// request exists and user is creator
				// updateRequest
				updateRequest(bot, message, chatId, requestTime, group, content, link);
			} else if (requestAssociationInfo.getAssociation() == Association.NONE) {
				// request may or may not exists, but the association doesn't
				// create new request
				newRequest(bot, message, chatId, requestTime, group, content, link);
			}
		}

	}

	private void updateRequest(TelegramBot bot, Message message, Long chatId, LocalDateTime requestTime, Group group,
			String content, String link) {

		// TODO check if the user is the creator
		// TODO True -> update request

	}

}
