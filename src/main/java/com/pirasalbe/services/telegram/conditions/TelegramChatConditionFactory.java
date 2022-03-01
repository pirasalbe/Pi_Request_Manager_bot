package com.pirasalbe.services.telegram.conditions;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Chat.Type;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pirasalbe.models.telegram.handlers.TelegramCondition;

/**
 * Command conditions factory
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramChatConditionFactory {

	/**
	 * Generates a condition on the specified chat type
	 *
	 * @param type Type of the chat
	 *
	 * @return TelegramCondition
	 */
	public TelegramCondition onChatType(Type type) {
		return onChatTypes(Arrays.asList(type));
	}

	/**
	 * Generates a condition on the specified chat type
	 *
	 * @param types Types of the chat
	 *
	 * @return TelegramCondition
	 */
	public TelegramCondition onChatTypes(Collection<Type> types) {
		return update -> {
			boolean asserted = false;

			// commands only handles messages
			Message message = getMessage(update);
			if (message != null) {
				asserted = types.contains(message.chat().type());
			}

			return asserted;
		};
	}

	private Message getMessage(Update update) {
		Message message = null;
		if (update.message() != null) {
			message = update.message();
		} else if (update.callbackQuery() != null) {
			message = update.callbackQuery().message();
		}

		return message;
	}

}
