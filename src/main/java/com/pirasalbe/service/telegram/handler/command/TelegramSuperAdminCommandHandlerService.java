package com.pirasalbe.service.telegram.handler.command;

import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.model.UserRole;
import com.pirasalbe.model.telegram.TelegramHandlerResult;

/**
 * Service to manage SuperAdmin commands
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramSuperAdminCommandHandlerService implements TelegramCommandHandler {

	private static final String COMMAND = "/admins";

	private static final UserRole ROLE = UserRole.SUPERADMIN;

	@Override
	public boolean shouldHandle(Message message) {
		// allow only in PM
		return message.text().equals(COMMAND) && message.chat().id().equals(message.from().id());
	}

	@Override
	public UserRole getRequiredRole() {
		return ROLE;
	}

	@Override
	public TelegramHandlerResult<SendMessage> handleCommand(Message message) {
		SendMessage sendMessage = new SendMessage(message.chat().id(), "TODO");
		sendMessage.replyToMessageId(message.messageId());

		return TelegramHandlerResult.reply(sendMessage);
	}

}
