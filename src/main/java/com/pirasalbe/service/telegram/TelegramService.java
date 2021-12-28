package com.pirasalbe.service.telegram;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pirasalbe.configuration.TelegramConfiguration;

/**
 * Service to manage the telegram bot
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramService {

	private TelegramBot bot;

	public TelegramService(TelegramConfiguration configuration) {
		this.bot = new TelegramBot(configuration.getToken());
	}

	@PostConstruct
	public void initializeHandlers() {
		// Register for updates
		bot.setUpdatesListener(updates -> {
			// ... process updates
			// return id of last processed update or confirm them all
			return UpdatesListener.CONFIRMED_UPDATES_ALL;
		});
	}

}
