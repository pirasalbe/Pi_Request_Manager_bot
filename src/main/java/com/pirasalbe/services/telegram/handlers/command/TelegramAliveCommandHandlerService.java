package com.pirasalbe.services.telegram.handlers.command;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.models.telegram.handlers.TelegramHandler;

/**
 * Service to manage /alive and /start
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramAliveCommandHandlerService implements TelegramHandler {

	public static final Set<String> COMMANDS = new HashSet<>(Arrays.asList("/start", "/alive"));

	@Override
	public void handle(TelegramBot bot, Update update) {
		SendMessage sendMessage = new SendMessage(update.message().chat().id(), "Bot up!");
		sendMessage.replyToMessageId(update.message().messageId());

		bot.execute(sendMessage);
	}

}
