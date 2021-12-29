package com.pirasalbe.service.telegram.handler.command;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.model.UserRole;
import com.pirasalbe.model.telegram.TelegramHandlerResult;

/**
 * Interface for the Telegram command handlers
 *
 * @author pirasalbe
 *
 */
public interface TelegramCommandHandler {

	/**
	 * Checks if it should manage the message
	 *
	 * @param update Command to check
	 * @return True if it should manage it
	 */
	boolean shouldHandle(Update update);

	/**
	 * Returns the minimum role to user the command
	 *
	 * @return Role
	 */
	UserRole getRequiredRole();

	/**
	 * Manage the message
	 *
	 * @param update Message arrived
	 */
	TelegramHandlerResult<SendMessage> handleCommand(Update update);

}
