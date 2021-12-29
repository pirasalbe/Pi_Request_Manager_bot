package com.pirasalbe.service.telegram.handler.command;

import com.pengrad.telegrambot.model.Message;
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
	 * @param text Command to check
	 * @return True if it should manage it
	 */
	boolean shouldHandle(String command);

	/**
	 * Returns the minimum role to user the command
	 *
	 * @return Role
	 */
	UserRole getRequiredRole();

	/**
	 * Manage the message
	 *
	 * @param message Message arrived
	 */
	TelegramHandlerResult<SendMessage> handleCommand(Message message);

}
