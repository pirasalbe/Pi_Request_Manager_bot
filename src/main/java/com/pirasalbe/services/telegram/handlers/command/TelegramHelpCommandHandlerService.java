package com.pirasalbe.services.telegram.handlers.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Chat.Type;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.models.UserRole;
import com.pirasalbe.models.telegram.TelegramHandlerResult;
import com.pirasalbe.services.telegram.AdminService;

/**
 * Service to manage /help
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramHelpCommandHandlerService implements TelegramCommandHandler {

	private static final String COMMAND = "/help";

	private static final UserRole ROLE = UserRole.USER;

	@Autowired
	private AdminService adminService;

	@Override
	public boolean shouldHandle(Update update) {
		return update.message() != null && update.message().text().startsWith(COMMAND);
	}

	@Override
	public UserRole getRequiredRole() {
		return ROLE;
	}

	@Override
	public TelegramHandlerResult<SendMessage> handleCommand(Update update) {
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

		return TelegramHandlerResult.reply(sendMessage);
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
			message.append(TelegramGroupsCommandHandlerService.INFO_COMMAND).append(" - ").append("Show group settings")
					.append("\n\n");

			message.append(TelegramGroupsCommandHandlerService.ENABLE_COMMAND).append(" - ")
					.append("Enable group management").append("\n");
			message.append(TelegramGroupsCommandHandlerService.DISABLE_COMMAND).append(" - ")
					.append("Disable group management").append("\n\n");

			message.append(TelegramGroupsCommandHandlerService.REQUEST_LIMIT_COMMAND).append(" [number of request]")
					.append(" - ").append("Define the limit of requests per day to [number of request]");
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
