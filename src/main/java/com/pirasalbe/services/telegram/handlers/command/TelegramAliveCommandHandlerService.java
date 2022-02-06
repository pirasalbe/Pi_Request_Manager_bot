package com.pirasalbe.services.telegram.handlers.command;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import com.pirasalbe.models.telegram.handlers.TelegramHandler;
import com.pirasalbe.services.SchedulerService;

/**
 * Service to manage /alive and /start
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramAliveCommandHandlerService implements TelegramHandler {

	public static final Set<String> COMMANDS = new HashSet<>(Arrays.asList("/start", "/alive"));

	@Autowired
	private SchedulerService schedulerService;

	@Override
	public void handle(TelegramBot bot, Update update) {
		Long chatId = update.message().chat().id();

		SendMessage sendMessage = new SendMessage(chatId, "Bot up!");
		sendMessage.replyToMessageId(update.message().messageId());

		SendResponse response = bot.execute(sendMessage);

		// schedule delete
		schedulerService.schedule((b, r) -> b.execute(new DeleteMessage(chatId, r.message().messageId())), response, 10,
				TimeUnit.SECONDS);
	}

}
