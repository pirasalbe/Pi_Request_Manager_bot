package com.pirasalbe.services.telegram.conditions;

import java.util.Collection;

import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Message;
import com.pirasalbe.configurations.TelegramConfiguration;
import com.pirasalbe.models.telegram.handlers.TelegramCondition;

/**
 * Command conditions factory
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramReplyToCommandConditionFactory extends TelegramCommandConditionFactory {

	public TelegramReplyToCommandConditionFactory(TelegramConfiguration configuration) {
		super(configuration);
	}

	/**
	 * Generates a condition on the specified commands
	 *
	 * @param command Command for the condition
	 * @return TelegramCondition
	 */
	@Override
	public TelegramCondition onCommands(Collection<String> commands) {
		return update -> {
			boolean asserted = false;

			// commands only handles messages
			Message message = update.message();
			if (message != null && message.replyToMessage() != null) {
				Message replyToMessage = message.replyToMessage();
				String messageCommand = getCommand(replyToMessage.text(), replyToMessage.entities());
				asserted = messageCommand != null && commands.contains(messageCommand);
			}

			return asserted;
		};
	}

}
