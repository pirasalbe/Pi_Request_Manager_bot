package com.pirasalbe.services.telegram;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pirasalbe.models.telegram.handlers.TelegramCondition;
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
			if (checkConditions(handlerService.getConditions(), update)) {
				found = true;
				handlerService.getHandler().handle(bot, update);
			}
		}
	}

	private boolean checkConditions(Collection<TelegramCondition> conditions, Update update) {
		boolean result = true;

		Iterator<TelegramCondition> iterator = conditions.iterator();
		while (iterator.hasNext() && result) {
			TelegramCondition telegramCondition = iterator.next();
			result = telegramCondition.check(update);
		}

		return result;
	}

}
