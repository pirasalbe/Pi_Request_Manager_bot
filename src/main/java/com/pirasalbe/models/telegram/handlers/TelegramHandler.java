package com.pirasalbe.models.telegram.handlers;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pirasalbe.models.telegram.TelegramHandlerResult;

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
	 * @return Result for the bot
	 */
	TelegramHandlerResult handle(TelegramBot bot, Update update);

}
