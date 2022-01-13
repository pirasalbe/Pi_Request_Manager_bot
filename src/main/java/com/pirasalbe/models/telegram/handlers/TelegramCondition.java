package com.pirasalbe.models.telegram.handlers;

import com.pengrad.telegrambot.model.Update;

/**
 * Define a condition
 *
 * @author pirasalbe
 *
 */
@FunctionalInterface
public interface TelegramCondition {

	/**
	 * Checks the update for a condition
	 *
	 * @param update Update to check
	 * @return True if the condition is asserted
	 */
	boolean check(Update update);

}
