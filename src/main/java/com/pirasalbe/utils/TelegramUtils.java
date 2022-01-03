package com.pirasalbe.utils;

import com.pengrad.telegrambot.model.MessageEntity;
import com.pengrad.telegrambot.model.MessageEntity.Type;
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
	 * Get text of a command
	 *
	 * @param update Message received
	 * @return Text from message
	 */
	public static String getText(Update update) {
		String result = null;

		if (update.message() != null && update.message().replyToMessage() != null) {
			// get the text from the reply to message
			result = update.message().replyToMessage().text();
		} else if (update.message() != null) {
			// get the text from the message
			result = update.message().text();
		} else if (update.callbackQuery() != null) {
			// get the text from the button data
			result = update.callbackQuery().data();
		}

		return result;
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

	/**
	 * Get text command without username
	 *
	 * @param update Update received
	 * @return Command without username
	 */
	public static String getTextCommand(Update update) {
		String text = update.message().text();

		boolean found = false;
		MessageEntity[] entities = update.message().entities();
		for (int i = 0; i < entities.length && !found; i++) {
			MessageEntity entity = entities[i];
			if (entity.type() == Type.bot_command) {
				Integer offset = entity.offset();
				text = text.substring(offset, offset + entity.length());
				found = true;
			}
		}

		int index = text.indexOf('@');
		if (index > 0) {
			text = text.substring(0, index);
		}

		return text;
	}

}
