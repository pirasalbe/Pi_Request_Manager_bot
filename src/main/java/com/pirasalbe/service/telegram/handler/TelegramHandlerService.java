package com.pirasalbe.service.telegram.handler;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.AbstractSendRequest;
import com.pirasalbe.model.telegram.TelegramHandlerResult;

/**
 * Interface for the Telegram handlers
 *
 * @author pirasalbe
 *
 */
public interface TelegramHandlerService<T extends AbstractSendRequest<?>> {

	/**
	 * Checks if it should manage the message
	 *
	 * @param update Message received
	 * @return True if it should manage it
	 */
	boolean shouldHandle(Update update);

	/**
	 * Manage the message
	 *
	 * @param update Message arrived
	 */
	TelegramHandlerResult<T> handleUpdate(Update update);

}
