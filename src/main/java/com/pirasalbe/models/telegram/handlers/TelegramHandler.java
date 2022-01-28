package com.pirasalbe.models.telegram.handlers;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;

/**
 *
 * @author pirasalbe
 *
 */
@FunctionalInterface
public interface TelegramHandler {

	/**
	 * Handle the update
	 *
	 * @param bot    Bot to send replies
	 * @param update Update to handle
	 *
	 */
	void handle(TelegramBot bot, Update update);

}
