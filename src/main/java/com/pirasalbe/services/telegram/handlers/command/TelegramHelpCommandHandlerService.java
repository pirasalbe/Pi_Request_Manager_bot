package com.pirasalbe.services.telegram.handlers.command;

import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.models.UserRole;
import com.pirasalbe.models.telegram.TelegramHandlerResult;

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
	public boolean shouldHandle(Update update) {
		return update.message() != null && update.message().text().startsWith(COMMAND);
	}

	@Override
	public UserRole getRequiredRole() {
		return ROLE;
	}

	@Override
	public TelegramHandlerResult<SendMessage> handleCommand(Update update) {
		// TODO implement help
		SendMessage sendMessage = new SendMessage(update.message().chat().id(), "TODO");
		sendMessage.replyToMessageId(update.message().messageId());

		return TelegramHandlerResult.reply(sendMessage);
	}

}
