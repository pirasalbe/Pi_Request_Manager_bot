package com.pirasalbe.services.telegram.conditions;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.MessageEntity;
import com.pengrad.telegrambot.model.MessageEntity.Type;
import com.pirasalbe.configurations.TelegramConfiguration;
import com.pirasalbe.models.telegram.handlers.TelegramCondition;

/**
 * Command conditions factory
 *
 * @author pirasalbe
 *
 */
@Component("telegramCommandConditionFactory")
public class TelegramCommandConditionFactory {

	protected String username;

	public TelegramCommandConditionFactory(TelegramConfiguration configuration) {
		this.username = configuration.getUsername();
	}

	/**
	 * Generates a condition on the specified command
	 *
	 * @param command Command for the condition
	 * @return TelegramCondition
	 */
	public TelegramCondition onCommand(String command) {
		return onCommands(Arrays.asList(command));
	}

	/**
	 * Generates a condition on the specified commands
	 *
	 * @param command Command for the condition
	 * @return TelegramCondition
	 */
	public TelegramCondition onCommands(Collection<String> commands) {
		return update -> {
			boolean asserted = false;

			// commands only handles messages
			Message message = update.message();
			if (message != null) {
				String messageCommand = getCommand(message.text(), message.entities());
				asserted = messageCommand != null && commands.contains(messageCommand);
			}

			return asserted;
		};
	}

	/**
	 * Get the command from the text<br>
	 * It removes the username of the bot if it's the bot username
	 *
	 * @param text     Text of the message
	 * @param entities Entities in the message
	 * @return String of the command. Null otherwise
	 */
	protected String getCommand(String text, MessageEntity[] entities) {
		String textCommand = null;

		// look for a command entity
		if (entities != null) {
			for (int i = 0; i < entities.length && textCommand == null; i++) {
				MessageEntity entity = entities[i];
				if (entity.type() == Type.bot_command) {
					Integer offset = entity.offset();
					textCommand = text.substring(offset, offset + entity.length());

					// remove username
					int index = textCommand.indexOf('@' + username);
					if (index > 0) {
						textCommand = textCommand.substring(1, index);
					}
				}
			}
		}

		return textCommand;
	}

}
