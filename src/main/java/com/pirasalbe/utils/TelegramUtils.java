package com.pirasalbe.utils;

import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.MessageEntity;
import com.pengrad.telegrambot.model.MessageEntity.Type;
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
	 * Get an HTML string that tags the user
	 *
	 * @param message Message of the user
	 * @return String
	 */
	public static String tagUser(Message message) {
		User user = message.from();

		return tagUser(user);
	}

	/**
	 * Get an HTML string that tags the user
	 *
	 * @param user User
	 * @return String
	 */
	public static String tagUser(User user) {
		return "<a href=\"tg://user?id=" + user.id() + "\">" + TelegramUtils.getUserName(user) + "</a>. ";
	}

	/**
	 * Get an HTML string that tags the user
	 *
	 * @param userId User id
	 * @return String
	 */
	public static String tagUser(Long userId) {
		return "<a href=\"tg://user?id=" + userId + "\">" + userId + "</a>. ";
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

	/**
	 * Get text without command
	 *
	 * @param text     Message text
	 * @param entities Message entities
	 * @return Text of the message without commands
	 */
	public static String removeCommand(String text, MessageEntity[] entities) {
		StringBuilder builder = new StringBuilder();

		if (entities != null) {
			for (int i = 0; i < entities.length && builder.length() == 0; i++) {
				MessageEntity entity = entities[i];
				if (entity.type() == Type.bot_command) {
					Integer offset = entity.offset();
					builder.append(text.substring(0, offset));
					builder.append(text.substring(offset + entity.length()));
				}
			}
		}

		return builder.toString();
	}

	/**
	 * Get a link to a message
	 *
	 * @param message Message to link
	 * @return link
	 */
	public static String getLink(Message message) {
		String chatId = message.chat().id().toString();
		String messageId = message.messageId().toString();
		return getLink(chatId, messageId);
	}

	/**
	 * Get a link to a message
	 *
	 * @param chatId    Chat of the message
	 * @param messageId Message Id
	 * @return link
	 */
	public static String getLink(String chatId, String messageId) {
		if (chatId.startsWith("-100")) {
			chatId = chatId.substring(4);
		}

		return "https://t.me/c/" + chatId + "/" + messageId;
	}

}
