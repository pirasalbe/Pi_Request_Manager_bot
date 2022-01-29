package com.pirasalbe.utils;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;

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
	 * Get user from message
	 *
	 * @param update Message received
	 * @return User
	 */
	public static User getUserFrom(Update update) {
		User result = null;

		if (update.message() != null) {
			// get from message
			result = update.message().from();
		} else if (update.editedMessage() != null) {
			// get from message
			result = update.editedMessage().from();
		} else if (update.callbackQuery() != null) {
			// get the text from keyboard response
			result = update.callbackQuery().from();
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
		User user = getUserFrom(update);

		if (user != null) {
			result = user.id();
		}

		return result;
	}

	/**
	 * Get user name from message
	 *
	 * @param update Message received
	 * @return User name
	 */
	public static String getUserName(Update update) {
		User user = getUserFrom(update);
		return getUserName(user);
	}

	/**
	 * Get user name from user
	 *
	 * @param update Message received
	 * @return User name
	 */
	public static String getUserName(User user) {
		String result = null;

		if (user != null) {
			if (user.username() != null) {
				result = "@" + user.username();
			} else if (user.firstName() != null && user.lastName() != null) {
				result = user.firstName() + " " + user.lastName();
			} else if (user.firstName() != null) {
				result = user.firstName();
			} else if (user.lastName() != null) {
				result = user.lastName();
			} else {
				result = user.id().toString();
			}
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
