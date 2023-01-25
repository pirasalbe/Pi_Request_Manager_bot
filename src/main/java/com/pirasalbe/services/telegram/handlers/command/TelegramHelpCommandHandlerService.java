package com.pirasalbe.services.telegram.handlers.command;

import java.util.concurrent.TimeUnit;

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
import com.pirasalbe.services.telegram.handlers.AbstractTelegramHandlerService;
import com.pirasalbe.utils.TelegramUtils;

/**
 * Service to manage /help
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramHelpCommandHandlerService extends AbstractTelegramHandlerService implements TelegramHandler {

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
			message = getUserHelp();
			break;
		}

		SendMessage sendMessage = new SendMessage(update.message().chat().id(), message);
		TelegramUtils.setMessageThreadId(sendMessage, update.message());
		sendMessage.parseMode(ParseMode.HTML);
		sendMessage.disableWebPagePreview(true);

		// keep message only in private
		boolean delete = chatType != Type.Private;
		if (!delete) {
			sendMessage.replyToMessageId(update.message().messageId());
		}

		sendMessageAndDelete(bot, sendMessage, 10, TimeUnit.SECONDS, delete);
		deleteMessage(bot, update.message(), delete);
	}

	private String getUserHelp() {
		StringBuilder message = new StringBuilder("<b>User help:</b>").append("\n");

		message.append(TelegramMeCommandHandlerService.COMMAND).append(" - ").append("Show user's info").append("\n");

		return message.toString();
	}

	private String getAdminHelp(Type chatType) {
		StringBuilder message = new StringBuilder("<b>Admin help:</b>").append("\n");

		if (chatType == Type.Private) {
			message.append("<a href='https://telegra.ph/PiRequestManager-02-16'>Admin commands</a>");
		} else {
			message.append("Go in PM to see your available commands.");
		}

		message.append("\n\n");

		message.append(getUserHelp());

		return message.toString();
	}

	private String getSuperAdminHelp(Type chatType) {
		StringBuilder message = new StringBuilder("<b>Super Admin help:</b>").append("\n");

		if (chatType == Type.Private) {
			message.append(TelegramSuperAdminCommandHandlerService.COMMAND).append(" - ")
					.append("Show commands to manage admins\n\n");
			message.append(TelegramSuperAdminCommandHandlerService.COMMAND_ADD).append(" [id name <code>")
					.append(UserRole.getRoles()).append("</code>]").append(" - ").append("Add admin\n");
			message.append(TelegramSuperAdminCommandHandlerService.COMMAND_REMOVE).append(" [id]").append(" - ")
					.append("Remove admin\n\n");

			message.append(TelegramInfoCommandHandlerService.COMMAND).append(" - ").append("Show bot info");
		} else {
			message.append("Go in PM to see your available commands.");
		}

		message.append("\n\n");

		message.append(getAdminHelp(chatType));

		return message.toString();
	}

}
