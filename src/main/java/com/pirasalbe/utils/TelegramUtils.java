package com.pirasalbe.utils;

import com.pengrad.telegrambot.model.Update;

/**
 * Utility methods for Telegram
 *
 * @author pirasalbe
 *
 */
public class TelegramUtils {

	private TelegramUtils() {
		super();
	}

	/**
	 * Get chat id from message
	 *
	 * @param update Message received
	 * @return Chat id
	 */
	public static Long getChatId(Update update) {
		Long result = null;

		if (update.message() != null) {
			// get from message
			result = update.message().chat().id();
		} else if (update.callbackQuery() != null) {
			// get the text from keyboard response
			result = update.callbackQuery().from().id();
		}

		return result;
	}

	/**
	 * Get user id from message
	 *
	 * @param update Message received
	 * @return User id
	 */
	public static Long getUserId(Update update) {
		Long result = null;

		if (update.message() != null) {
			// get from message
			result = update.message().from().id();
		} else if (update.callbackQuery() != null) {
			// get the text from keyboard response
			result = update.callbackQuery().from().id();
		}

		return result;
	}

	/**
	 * Get message id from message
	 *
	 * @param update Message received
	 * @return Message id
	 */
	public static Integer getMessageId(Update update) {
		Integer result = null;

		if (update.message() != null) {
			// get from message
			result = update.message().messageId();
		}

		return result;
	}

}
