package com.pirasalbe.models.telegram.handlers.conditions;

import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.MessageEntity;
import com.pengrad.telegrambot.model.MessageEntity.Type;
import com.pengrad.telegrambot.model.Update;
import com.pirasalbe.models.telegram.handlers.TelegramCondition;

/**
 * Define a command condition
 *
 * @author pirasalbe
 *
 */
public class TelegramCommand implements TelegramCondition {

	protected String username;

	private String command;

	public TelegramCommand(String username, String command) {
		this.username = username;
		setCommand(command);
	}

	private void setCommand(String command) {
		StringBuilder builder = new StringBuilder();
		if (!command.startsWith("/")) {
			builder.append("/");
		}
		builder.append(command);

		this.command = builder.toString();
	}

	@Override
	public boolean check(Update update) {
		boolean asserted = false;

		// commands only handles messages
		Message message = update.message();
		if (message != null) {
			String messageCommand = getCommand(message.text(), message.entities());
			asserted = messageCommand != null && command.equals(messageCommand);
		}

		return asserted;
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

		return textCommand;
	}

}
