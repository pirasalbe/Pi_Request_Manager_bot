package com.pirasalbe.service.telegram;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pirasalbe.configuration.TelegramConfiguration;
import com.pirasalbe.service.telegram.handler.TelegramCommandHandlerService;
import com.pirasalbe.service.telegram.handler.TelegramHandlerService;
import com.pirasalbe.service.telegram.handler.TelegramUnknownHandlerService;

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
	private TelegramCommandHandlerService commandHandlerService;

	@Autowired
	private TelegramUnknownHandlerService unknownHandlerService;

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

	private void manageUpdate(Update update) {
		TelegramHandlerService handlerService = unknownHandlerService;

		if (commandHandlerService.shouldHandle(update)) {
			handlerService = commandHandlerService;
		}

		handlerService.handleUpdate(update);
	}

}
