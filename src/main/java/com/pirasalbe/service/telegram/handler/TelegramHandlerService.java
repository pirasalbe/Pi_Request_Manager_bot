package com.pirasalbe.service.telegram.handler;

import com.pengrad.telegrambot.model.Update;

/**
 * Interface for the Telegram handlers
 *
 * @author pirasalbe
 *
 */
public interface TelegramHandlerService {

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
	void handleUpdate(Update update);

}
