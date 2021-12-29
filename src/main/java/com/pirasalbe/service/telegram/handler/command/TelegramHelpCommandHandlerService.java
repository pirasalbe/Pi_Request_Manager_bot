package com.pirasalbe.service.telegram.handler.command;

import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Update;
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
