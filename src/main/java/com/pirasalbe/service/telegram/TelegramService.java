package com.pirasalbe.service.telegram;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pirasalbe.configuration.TelegramConfiguration;
import com.pirasalbe.model.telegram.TelegramHandlerResult;
import com.pirasalbe.service.telegram.handler.TelegramHandlerServiceFactory;

/**
 * Service to manage the telegram bot
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramService {

	private TelegramBot bot;

	@Autowired
	private TelegramHandlerServiceFactory handlerServiceFactory;

	public TelegramService(TelegramConfiguration configuration) {
		this.bot = new TelegramBot(configuration.getToken());
	}

	@PostConstruct
	public void initializeHandlers() {
		// Register for updates
		bot.setUpdatesListener(updates -> {
			int lastProcessed = UpdatesListener.CONFIRMED_UPDATES_NONE;

			// process updates
			for (Update update : updates) {
				lastProcessed = update.updateId();

				// update
				manageUpdate(update);
			}

			// return id of last processed update or confirm them all
			return lastProcessed;
		});
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void manageUpdate(Update update) {
		TelegramHandlerResult handlerResult = handlerServiceFactory.getTelegramHandlerService(update)
				.handleUpdate(update);

		if (handlerResult.shouldReply()) {
			bot.execute(handlerResult.getResponse());
		}
	}

}
