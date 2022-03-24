package com.pirasalbe.services.telegram.conditions;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.MessageEntity;
import com.pengrad.telegrambot.model.MessageEntity.Type;
import com.pengrad.telegrambot.model.Update;
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
	 * Generates a condition on the specified command
	 *
	 * @param command   Command for the condition
	 * @param allowText If false, checks that there is no text other than the
	 *                  command
	 * @return TelegramCondition
	 */
	public TelegramCondition onCommand(String command, boolean allowText) {
		return onCommands(Arrays.asList(command), allowText);
	}

	/**
	 * Generates a condition on the specified commands
	 *
	 * @param command Command for the condition
	 * @return TelegramCondition
	 */
	public TelegramCondition onCommands(Collection<String> commands) {
		return onCommands(commands, true);
	}

	/**
	 * Generates a condition on the specified commands
	 *
	 * @param command   Command for the condition
	 * @param allowText If false, checks that there is no text other than the
	 *                  command
	 * @return TelegramCondition
	 */
	public TelegramCondition onCommands(Collection<String> commands, boolean allowText) {
		return update -> {
			boolean asserted = false;

			// commands only handles messages
			Message message = getMessage(update);
			if (message != null) {
				String messageCommand = getCommand(message.text(), message.entities());
				asserted = messageCommand != null && commands.contains(messageCommand);

				if (asserted && !allowText) {
					// checks that the command is the whole word
					asserted = messageCommand.equals(message.text().trim());
				}
			}

			return asserted;
		};
	}

	/**
	 * Get message from update
	 *
	 * @param update Update
	 * @return Message
	 */
	private Message getMessage(Update update) {
		Message message = null;
		if (update.message() != null) {
			message = update.message();
		} else if (update.channelPost() != null) {
			message = update.channelPost();
		}

		return message;
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
						textCommand = textCommand.substring(0, index);
					}
				}
			}
		}

		return textCommand;
	}

}
