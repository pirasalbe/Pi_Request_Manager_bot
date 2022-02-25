package com.pirasalbe.services.telegram.handlers.command;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat.Type;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.models.telegram.handlers.TelegramHandler;
import com.pirasalbe.services.telegram.handlers.AbstractTelegramHandlerService;

/**
 * Service to manage /alive and /start
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramAliveCommandHandlerService extends AbstractTelegramHandlerService implements TelegramHandler {

	public static final Set<String> COMMANDS = new HashSet<>(Arrays.asList("/start", "/alive"));

	@Override
	public void handle(TelegramBot bot, Update update) {
		Long chatId = update.message().chat().id();

		SendMessage sendMessage = new SendMessage(chatId, "Bot up!");

		boolean delete = update.message().chat().type() != Type.Private;
		if (!delete) {
			sendMessage.replyToMessageId(update.message().messageId());
		}

		sendMessageAndDelete(bot, sendMessage, 10, TimeUnit.SECONDS, delete);
		deleteMessage(bot, update.message(), delete);
	}

}
