package com.pirasalbe.service.telegram.handler.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ForceReply;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.model.UserRole;
import com.pirasalbe.model.telegram.TelegramHandlerResult;
import com.pirasalbe.service.telegram.AdminService;
import com.pirasalbe.utils.TelegramUtils;

/**
 * Service to manage SuperAdmin commands
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramSuperAdminCommandHandlerService implements TelegramCommandHandler {

	private static final String COMMAND = "/admins";
	private static final String COMMAND_LIST = COMMAND + " list";
	private static final String COMMAND_ADD = COMMAND + " add";
	private static final String COMMAND_REMOVE = COMMAND + " remove";

	private static final UserRole ROLE = UserRole.SUPERADMIN;

	@Autowired
	private AdminService adminService;

	@Override
	public boolean shouldHandle(Update update) {
		return TelegramUtils.getText(update).startsWith(COMMAND)
				// allow only in PM
				&& TelegramUtils.getChatId(update).equals(TelegramUtils.getUserId(update));
	}

	@Override
	public UserRole getRequiredRole() {
		return ROLE;
	}

	@Override
	public TelegramHandlerResult<SendMessage> handleCommand(Update update) {
		SendMessage sendMessage = null;

		String text = TelegramUtils.getText(update);
		if (text.startsWith(COMMAND_LIST)) {

		} else if (text.startsWith(COMMAND_ADD)) {
			sendMessage = addUser(update);
		} else if (text.startsWith(COMMAND_REMOVE)) {

		} else {
			// show keyboard
			sendMessage = showActions(update);
		}

		return TelegramHandlerResult.reply(sendMessage);
	}

	private SendMessage addUser(Update update) {
		SendMessage sendMessage = null;

		if (TelegramUtils.getText(update).equals(COMMAND_ADD)) {
			// ask the id
			StringBuilder builder = new StringBuilder(COMMAND_ADD).append("\n");
			builder.append("Send the ID of the user and the role").append("\n");
			String placeholder = "id " + UserRole.getRoles();
			builder.append("Format: <code>").append(placeholder).append("</code>");
			sendMessage = new SendMessage(TelegramUtils.getChatId(update), builder.toString());
			sendMessage.parseMode(ParseMode.HTML);
			sendMessage.replyMarkup(new ForceReply().inputFieldPlaceholder(placeholder));
		} else if (update.message() != null && update.message().replyToMessage() != null) {
			// add user
			sendMessage = addUser(TelegramUtils.getChatId(update), update.message().text(),
					update.message().messageId());
		} else {
			sendMessage = new SendMessage(TelegramUtils.getChatId(update), "Something went wrong");
		}

		return sendMessage;
	}

	private SendMessage addUser(Long chatId, String text, Integer messageId) {
		SendMessage sendMessage = null;

		String[] parts = text.trim().split(" ");
		if (parts.length == 2) {
			// add user
			long adminId = Long.parseLong(parts[0]);
			UserRole adminRole = UserRole.valueOf(parts[1].toUpperCase());
			adminService.insertAdmin(adminId, adminRole);
			sendMessage = new SendMessage(chatId, "Admin " + adminId + " with role " + adminRole.name() + " added");
		} else {
			sendMessage = new SendMessage(chatId, "There is something wrong with your message. Try again.");
		}

		sendMessage.replyToMessageId(messageId);

		return sendMessage;
	}

	/**
	 * Show a keyboard with available commands
	 *
	 * @param message Message received
	 * @return Response
	 */
	private SendMessage showActions(Update update) {
		SendMessage sendMessage = new SendMessage(TelegramUtils.getChatId(update), "What do you want to do?");
		sendMessage.replyToMessageId(TelegramUtils.getMessageId(update));

		sendMessage.replyMarkup(getAdminsKeyboard());

		return sendMessage;
	}

	private Keyboard getAdminsKeyboard() {
		InlineKeyboardButton listButton = new InlineKeyboardButton("List").callbackData(COMMAND_LIST + " 0 10");
		InlineKeyboardButton addButton = new InlineKeyboardButton("Add").callbackData(COMMAND_ADD);
		InlineKeyboardButton removeButton = new InlineKeyboardButton("Remove").callbackData(COMMAND_REMOVE);

		return new InlineKeyboardMarkup(new InlineKeyboardButton[] { listButton },
				new InlineKeyboardButton[] { addButton, removeButton });
	}

}
