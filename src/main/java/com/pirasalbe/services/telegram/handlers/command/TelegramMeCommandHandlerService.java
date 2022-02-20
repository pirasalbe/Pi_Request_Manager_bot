package com.pirasalbe.services.telegram.handlers.command;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat.Type;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.models.telegram.handlers.TelegramHandler;
import com.pirasalbe.services.AdminService;
import com.pirasalbe.services.telegram.handlers.AbstractTelegramHandlerService;

/**
 * Service to manage /me
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramMeCommandHandlerService extends AbstractTelegramHandlerService implements TelegramHandler {

	public static final String COMMAND = "/me";

	@Autowired
	private AdminService adminService;

	@Override
	public void handle(TelegramBot bot, Update update) {
		// get sender id
		Long userId = update.message().from().id();
		Integer messageId = update.message().messageId();
		if (update.message().replyToMessage() != null) {
			userId = update.message().replyToMessage().from().id();
			messageId = update.message().replyToMessage().messageId();
		}

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("<b>User info</b>:\n");
		stringBuilder.append("Id: <code>").append(userId).append("</code>\n");
		stringBuilder.append("Role: <code>").append(adminService.getAuthority(userId)).append("</code>");
		SendMessage sendMessage = new SendMessage(update.message().chat().id(), stringBuilder.toString());
		sendMessage.replyToMessageId(messageId);
		sendMessage.parseMode(ParseMode.HTML);

		boolean delete = update.message().chat().type() != Type.Private;

		sendMessageAndDelete(bot, sendMessage, 10, TimeUnit.SECONDS, delete);
		deleteMessage(bot, update.message(), delete);
	}

}
