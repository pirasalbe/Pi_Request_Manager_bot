package com.pirasalbe.services.telegram.handlers.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat.Type;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.models.UserRole;
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

		String message = null;

		switch (userRole) {
		case SUPERADMIN:
			message = getSuperAdminHelp(chatType);
			break;
		case MANAGER:
		case CONTRIBUTOR:
			message = getAdminHelp(chatType);
			break;
		case USER:
		default:
			message = getUserHelp(chatType);
			break;
		}

		SendMessage sendMessage = new SendMessage(update.message().chat().id(), message);
		sendMessage.parseMode(ParseMode.HTML);
		sendMessage.replyToMessageId(update.message().messageId());
		sendMessage.disableWebPagePreview(true);

		bot.execute(sendMessage);
	}

	private String getUserHelp(Type chatType) {
		StringBuilder message = new StringBuilder("<b>User help:</b>").append("\n");

		message.append(TelegramMeCommandHandlerService.COMMAND).append(" - ").append("Show user's info").append("\n");
		message.append("<code>Reply to a another user's message with</code> ")
				.append(TelegramMeCommandHandlerService.COMMAND).append(" - ").append("Show other user's info");

		return message.toString();
	}

	private String getAdminHelp(Type chatType) {
		StringBuilder message = new StringBuilder("<b>Contributor help:</b>").append("\n");

		if (chatType == Type.Private) {
			message.append("<a href='https://telegra.ph/PiRequestManager-02-16'>Contributors commands</a>");
		} else {
			message.append("Go in PM to see your available commands.");
		}

		message.append("\n\n");

		message.append(getUserHelp(chatType));

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

		message.append(getAdminHelp(chatType));

		return message.toString();
	}

}
