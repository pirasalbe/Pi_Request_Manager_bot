package com.pirasalbe.service.telegram.handler.command;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.model.UserRole;
import com.pirasalbe.model.telegram.TelegramHandlerResult;

/**
 * Service to manage /alive and /start
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramAliveCommandHandlerService implements TelegramCommandHandler {

	private static final Set<String> COMMANDS = new HashSet<>(Arrays.asList("/start", "/alive"));

	private static final UserRole ROLE = UserRole.USER;

	@Override
	public boolean shouldHandle(Update update) {
		return update.message() != null && COMMANDS.contains(update.message().text());
	}

	@Override
	public UserRole getRequiredRole() {
		return ROLE;
	}

	@Override
	public TelegramHandlerResult<SendMessage> handleCommand(Update update) {
		SendMessage sendMessage = new SendMessage(update.message().chat().id(), "Bot up!");
		sendMessage.replyToMessageId(update.message().messageId());

		return TelegramHandlerResult.reply(sendMessage);
	}

}
