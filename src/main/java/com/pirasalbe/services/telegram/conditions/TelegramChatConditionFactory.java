package com.pirasalbe.services.telegram.conditions;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Chat.Type;
import com.pengrad.telegrambot.model.Message;
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
			Message message = update.message();
			if (message != null) {
				asserted = types.contains(message.chat().type());
			}

			return asserted;
		};
	}

}
