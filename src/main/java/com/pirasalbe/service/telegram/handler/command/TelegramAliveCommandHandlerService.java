package com.pirasalbe.service.telegram.handler.command;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.model.UserRole;
import com.pirasalbe.model.telegram.TelegramHandlerResult;

/**
 * Service to manage commands from the telegram bot
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramAliveCommandHandlerService implements TelegramCommandHandler {

	private static final Set<String> COMMANDS = new HashSet<>(Arrays.asList("/start", "/alive"));

	private static final UserRole ROLE = UserRole.USER;

	@Override
	public boolean shouldHandle(String command) {
		return COMMANDS.contains(command);
	}

	@Override
	public UserRole getRequiredRole() {
		return ROLE;
	}

	@Override
	public TelegramHandlerResult<SendMessage> handleCommand(Message message) {
		SendMessage sendMessage = new SendMessage(message.chat().id(), "Bot up!");
		sendMessage.replyToMessageId(message.messageId());

		return TelegramHandlerResult.reply(sendMessage);
	}

}
