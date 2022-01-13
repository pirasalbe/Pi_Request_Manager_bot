package com.pirasalbe.services.telegram;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pirasalbe.models.telegram.handlers.TelegramUpdateHandler;

/**
 * Service that gives the right handler of a message
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramUpdateHandlerRegistry {

	private Set<TelegramUpdateHandler> updateHandlers;

	public TelegramUpdateHandlerRegistry() {
		updateHandlers = new LinkedHashSet<>();
	}

	/**
	 * Register a new update handler
	 *
	 * @param updateHandler Update handler to register
	 */
	public void register(TelegramUpdateHandler updateHandler) {
		updateHandlers.add(updateHandler);
	}

	/**
	 * Get the handler for the update
	 *
	 * @param bot    Telegram Bot to send replies
	 * @param update Update to check
	 *
	 * @return TelegramHandlerService
	 */
	public void handle(TelegramBot bot, Update update) {
		boolean found = false;

		Iterator<TelegramUpdateHandler> iterator = updateHandlers.iterator();
		while (iterator.hasNext() && !found) {
			TelegramUpdateHandler handlerService = iterator.next();
			if (handlerService.getCondition().check(update)) {
				found = true;
				handlerService.getHandler().handle(bot, update);
			}
		}
	}

}
