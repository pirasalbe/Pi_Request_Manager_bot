package com.pirasalbe.services.telegram.handlers.command;

import com.pengrad.telegrambot.model.Update;
import com.pirasalbe.models.UserRole;
import com.pirasalbe.models.telegram.TelegramHandlerResult;

/**
 * Interface for the Telegram command handlers
 *
 * @author pirasalbe
 *
 */
public interface TelegramCommandHandler {

	static final String SOMETHING_WENT_WRONG = "Something went wrong";

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
	TelegramHandlerResult handleCommand(Update update);

}
