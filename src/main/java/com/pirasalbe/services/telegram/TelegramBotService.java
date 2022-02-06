package com.pirasalbe.services.telegram;

import java.util.Arrays;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pirasalbe.configurations.TelegramConfiguration;
import com.pirasalbe.models.telegram.handlers.TelegramCondition;
import com.pirasalbe.models.telegram.handlers.TelegramHandler;
import com.pirasalbe.models.telegram.handlers.TelegramUpdateHandler;

/**
 * Service to manage the telegram bot
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramBotService {

	private static final Logger LOGGER = LoggerFactory.getLogger(TelegramBotService.class);

	private TelegramBot bot;

	@Autowired
	private TelegramUpdateHandlerRegistry registry;

	public TelegramBotService(TelegramConfiguration configuration) {
		this.bot = new TelegramBot(configuration.getToken());
	}

	public TelegramBot getBot() {
		return bot;
	}

	public void launch() {
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
			registry.handle(bot, update);
		} catch (Exception e) {
			LOGGER.error("Unexpected error for message [{}]", update, e);
		}
	}

	/**
	 * Handle an update
	 *
	 * @param condition Condition to determine if it should handle the update
	 * @param handler   Handling logic
	 */
	public void register(TelegramCondition condition, TelegramHandler handler) {
		register(Arrays.asList(condition), handler);
	}

	/**
	 * Handle an update
	 *
	 * @param conditions Conditions to determine if it should handle the update
	 * @param handler    Handling logic
	 */
	public void register(Collection<TelegramCondition> condition, TelegramHandler handler) {
		registry.register(new TelegramUpdateHandler(condition, handler));
	}

}
