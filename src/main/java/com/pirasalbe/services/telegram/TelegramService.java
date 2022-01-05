package com.pirasalbe.services.telegram;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pirasalbe.configurations.TelegramConfiguration;
import com.pirasalbe.models.telegram.TelegramHandlerResult;
import com.pirasalbe.services.telegram.handler.TelegramHandlerServiceFactory;

/**
 * Service to manage the telegram bot
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramService {

	private static final Logger LOGGER = LoggerFactory.getLogger(TelegramService.class);

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

	private void manageUpdate(Update update) {
		try {
			TelegramHandlerResult handlerResult = handlerServiceFactory.getTelegramHandlerService(update)
					.handleUpdate(update);

			for (BaseRequest<?, ?> response : handlerResult.getResponses()) {
				bot.execute(response);
			}
		} catch (Exception e) {
			LOGGER.error("Unexpected error", e);
		}
	}

}
