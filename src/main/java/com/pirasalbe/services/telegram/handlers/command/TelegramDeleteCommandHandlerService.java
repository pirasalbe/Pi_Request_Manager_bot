package com.pirasalbe.services.telegram.handlers.command;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.MessageEntity;
import com.pengrad.telegrambot.model.MessageEntity.Type;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pirasalbe.models.telegram.handlers.TelegramCondition;
import com.pirasalbe.models.telegram.handlers.TelegramHandler;
import com.pirasalbe.services.telegram.handlers.AbstractTelegramHandlerService;

/**
 * Service to delete commands
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramDeleteCommandHandlerService extends AbstractTelegramHandlerService
		implements TelegramHandler, TelegramCondition {

	@Override
	public boolean check(Update update) {
		boolean asserted = false;

		Message message = update.message();

		// only when not PM
		if (message != null && message.chat().type() != com.pengrad.telegrambot.model.Chat.Type.Private
				&& message.entities() != null) {

			MessageEntity[] entities = message.entities();
			for (int i = 0; i < entities.length && !asserted; i++) {
				MessageEntity entity = entities[i];

				// true if it is a command
				asserted = entity.type() == Type.bot_command;
			}
		}

		return asserted;
	}

	@Override
	public void handle(TelegramBot bot, Update update) {
		schedulerService.schedule((b, r) -> b.execute(new DeleteMessage(r.chat().id(), r.messageId())),
				update.message(), 5, TimeUnit.SECONDS);
	}

}
