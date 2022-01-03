package com.pirasalbe.services.telegram.handlers.command;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Chat.Type;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.models.UserRole;
import com.pirasalbe.models.telegram.TelegramHandlerResult;
import com.pirasalbe.services.telegram.GroupService;
import com.pirasalbe.utils.TelegramUtils;

/**
 * Service to manage groups related commands
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramGroupsCommandHandlerService implements TelegramCommandHandler {

	private static final String ENABLE_COMMAND = "/enable_group";
	private static final String DISABLE_COMMAND = "/disable_group";

	private static final List<String> COMMANDS = Arrays.asList(ENABLE_COMMAND, DISABLE_COMMAND);

	private static final UserRole ROLE = UserRole.MANAGER;

	@Autowired
	private GroupService groupService;

	@Override
	public boolean shouldHandle(Update update) {
		return update.message() != null && COMMANDS.contains(update.message().text())
		// command vaild only on groups
				&& (update.message().chat().type() == Type.group || update.message().chat().type() == Type.supergroup);
	}

	@Override
	public UserRole getRequiredRole() {
		return ROLE;
	}

	@Override
	public TelegramHandlerResult<SendMessage> handleCommand(Update update) {
		SendMessage sendMessage = null;

		Long chatId = TelegramUtils.getChatId(update);
		if (update.message().text().equals(ENABLE_COMMAND)) {
			groupService.insertIfNotExists(chatId);
			sendMessage = new SendMessage(chatId, "Group enabled");
		} else if (update.message().text().equals(DISABLE_COMMAND)) {
			groupService.deleteIfExists(chatId);
			sendMessage = new SendMessage(chatId, "Group disabled");
		} else {
			sendMessage = new SendMessage(chatId, SOMETHING_WENT_WRONG);
		}

		sendMessage.replyToMessageId(update.message().messageId());

		return TelegramHandlerResult.reply(sendMessage);
	}

}
