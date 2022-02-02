package com.pirasalbe.services.telegram.handlers.command;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat.Type;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.models.UserRole;
import com.pirasalbe.models.request.Source;
import com.pirasalbe.models.telegram.handlers.TelegramHandler;
import com.pirasalbe.services.AdminService;

/**
 * Service to manage /help
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramHelpCommandHandlerService implements TelegramHandler {

	public static final String COMMAND = "/help";

	@Autowired
	private AdminService adminService;

	@Override
	public void handle(TelegramBot bot, Update update) {
		UserRole userRole = adminService.getAuthority(update.message().from().id());
		Type chatType = update.message().chat().type();

		// TODO
		String message = null;

		switch (userRole) {
		case SUPERADMIN:
			message = getSuperAdminHelp(chatType);
			break;
		case MANAGER:
			message = getManagerHelp(chatType);
			break;
		case CONTRIBUTOR:
			message = getContributorHelp(chatType);
			break;
		case USER:
		default:
			message = getUserHelp(chatType);
			break;
		}

		SendMessage sendMessage = new SendMessage(update.message().chat().id(), message);
		sendMessage.parseMode(ParseMode.HTML);
		sendMessage.replyToMessageId(update.message().messageId());

		bot.execute(sendMessage);
	}

	private String getUserHelp(Type chatType) {
		StringBuilder message = new StringBuilder("<b>User help:</b>").append("\n");

		message.append("TODO");

		return message.toString();
	}

	private String getContributorHelp(Type chatType) {
		StringBuilder message = new StringBuilder("<b>Contributor help:</b>").append("\n");

		message.append("TODO").append("\n\n");

		message.append(getUserHelp(chatType));

		return message.toString();
	}

	private String getManagerHelp(Type chatType) {
		StringBuilder message = new StringBuilder("<b>Manager help:</b>").append("\n");

		message.append("<i>Group commands:</i>").append("\n");
		if (chatType != Type.channel) {
			message.append(TelegramGroupsCommandHandlerService.COMMAND_INFO).append(" - ").append("Show group settings")
					.append("\n\n");

			message.append(TelegramGroupsCommandHandlerService.COMMAND_ENABLE).append(" - ")
					.append("Enable group management").append("\n");
			message.append(TelegramGroupsCommandHandlerService.COMMAND_DISABLE).append(" - ")
					.append("Disable group management").append("\n\n");

			message.append(TelegramGroupsCommandHandlerService.COMMAND_REQUEST_LIMIT).append(" [number of request]")
					.append(" - ").append("Define the limit of requests per day to <i>[number of request]</i>")
					.append("\n");
			message.append(TelegramGroupsCommandHandlerService.COMMAND_AUDIOBOOK_DAYS_WAIT)
					.append(" [number of days to wait]").append(" - ")
					.append("Define the days to wait before requesting a new audiobook to <i>[number of days to wait]</i>")
					.append("\n");
			message.append(TelegramGroupsCommandHandlerService.COMMAND_ENGLISH_AUDIOBOOK_DAYS_WAIT)
					.append(" [number of days to wait]").append(" - ")
					.append("Define the days to wait before requesting a new English audiobook to <i>[number of days to wait]</i>")
					.append("\n");
			message.append(TelegramGroupsCommandHandlerService.COMMAND_ALLOW).append(" [ebooks/audiobooks/both]")
					.append(" - ").append("Define what can be requested <i>[ebooks/audiobooks/both]</i>").append("\n");
			message.append(TelegramGroupsCommandHandlerService.COMMAND_NO_REPEAT).append(" ")
					.append(Arrays.asList(Source.values())).append(" - ")
					.append("Define the tags whose requests can't be repeated. Send a list of sources, for example <i>KU, AUDIBLE, SCRIBD, ARCHIVE</i>");
		} else {
			message.append("Go in PM or a group to see the available commands.");
		}

		message.append("\n\n");

		message.append(getContributorHelp(chatType));

		return message.toString();
	}

	private String getSuperAdminHelp(Type chatType) {
		StringBuilder message = new StringBuilder("<b>Admin help:</b>").append("\n");

		if (chatType == Type.Private) {
			message.append(TelegramSuperAdminCommandHandlerService.COMMAND).append(" - ")
					.append("Show commands to manage admins");
		} else {
			message.append("Go in PM to see your available commands.");
		}

		message.append("\n\n");

		message.append(getManagerHelp(chatType));

		return message.toString();
	}

}
