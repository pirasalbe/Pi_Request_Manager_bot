package com.pirasalbe.services.telegram.conditions;

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
		return update -> {
			boolean asserted = false;

			// commands only handles messages
			Message message = update.message();
			if (message != null) {
				asserted = message.chat().type() == type;
			}

			return asserted;
		};
	}

}
