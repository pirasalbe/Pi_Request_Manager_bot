package com.pirasalbe.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger LOGGER = LoggerFactory.getLogger(TelegramUtils.class);

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
		return tagUser(user.id(), TelegramUtils.getUserName(user));
	}

	/**
	 * Get an HTML string that tags the user
	 *
	 * @param userId User id
	 * @return String
	 */
	public static String tagUser(Long userId) {
		return tagUser(userId, userId.toString());
	}

	/**
	 * Get an HTML string that tags the user
	 *
	 * @param userId User id
	 * @param text   Text to show
	 * @return String
	 */
	private static String tagUser(Long userId, String text) {
		return "<a href=\"tg://user?id=" + userId + "\">" + text + "</a>. ";
	}

	public static String getStartLink(String username, String payload) {
		return "https://t.me/" + username + "?start=" + payload;
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
		} else if (update.channelPost() != null) {
			// get the text from keyboard response
			result = update.channelPost().chat().id();
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
		} else if (update.channelPost() != null) {
			// get the text from channel post
			result = update.channelPost().from();
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
	public static String getLink(Long groupId, Long messageId) {
		return getLink(groupId.toString(), messageId.toString());
	}

	/**
	 * Get a link to a message
	 *
	 * @param groupId   Chat of the message
	 * @param messageId Message Id
	 * @return link
	 */
	public static String getLink(String groupId, String messageId) {
		if (groupId.startsWith("-100")) {
			groupId = groupId.substring(4);
		}

		return "https://t.me/c/" + groupId + "/" + messageId;
	}

	/**
	 * Get message from update
	 *
	 * @param update Update
	 * @return Message
	 */
	public static Message getMessage(Update update) {
		Message message = null;
		if (update.message() != null) {
			message = update.message();
		} else if (update.channelPost() != null) {
			message = update.channelPost();
		} else if (update.callbackQuery() != null) {
			message = update.callbackQuery().message();
		}

		return message;
	}

	/**
	 * Checks the request limit and sleep if needed
	 *
	 * @param requestCount Request done till now
	 * @param newRequest   If the new request was successful
	 * @return The new request count
	 */
	public static int checkRequestLimitSameGroup(int requestCount, boolean newRequest) {
		int result = requestCount;

		// if tr
		if (newRequest) {
			result = requestCount + 1;
		}

		// if request count greater then the limit, sleep and reset count
		if (result >= 10) {
			LOGGER.info("Cooldown due to the Telegram limits");
			try {
				Thread.sleep(120000);
				result = 0;
			} catch (InterruptedException e) {
				LOGGER.warn("Could not sleep to prevent request limit", e);
			}
			LOGGER.info("End cooldown period");
		}

		return result;
	}

}
