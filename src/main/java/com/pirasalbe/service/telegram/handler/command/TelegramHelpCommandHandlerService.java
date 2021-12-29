package com.pirasalbe.service.telegram.handler.command;

import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.model.UserRole;
import com.pirasalbe.model.telegram.TelegramHandlerResult;

/**
 * Service to manage /help
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramHelpCommandHandlerService implements TelegramCommandHandler {

	private static final String COMMAND = "/help";

	private static final UserRole ROLE = UserRole.USER;

	@Override
	public boolean shouldHandle(Message message) {
		return message.text().startsWith(COMMAND);
	}

	@Override
	public UserRole getRequiredRole() {
		return ROLE;
	}

	@Override
	public TelegramHandlerResult<SendMessage> handleCommand(Message message) {
		// TODO implement help
		SendMessage sendMessage = new SendMessage(message.chat().id(), "TODO");
		sendMessage.replyToMessageId(message.messageId());

		return TelegramHandlerResult.reply(sendMessage);
	}

}
