package com.pirasalbe.service.telegram.handler.command;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ForceReply;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.model.Pagination;
import com.pirasalbe.model.UserRole;
import com.pirasalbe.model.database.Admin;
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
			// navigate users
			sendMessage = listUsers(update);
		} else if (text.startsWith(COMMAND_ADD)) {
			// add user
			sendMessage = addUser(update);
		} else if (text.startsWith(COMMAND_REMOVE)) {
			// remove user
			sendMessage = removeUser(update);
		} else {
			// show keyboard
			sendMessage = showActions(update);
		}

		return TelegramHandlerResult.reply(sendMessage);
	}

	private SendMessage listUsers(Update update) {
		SendMessage sendMessage = null;

		if (update.callbackQuery() != null) {
			int offset = 0;
			int limit = 10;

			// get limit and offset from string
			String[] parts = update.callbackQuery().data().substring(COMMAND_LIST.length()).trim().split(" ");
			if (parts.length == 2) {
				offset = Integer.parseInt(parts[0]);
				limit = Integer.parseInt(parts[1]);
			}

			Pagination<Admin> pagination = adminService.list(offset, limit);
			sendMessage = getResponseFromPagination(update, offset, limit, pagination);
		} else {
			sendMessage = new SendMessage(TelegramUtils.getChatId(update), "Something went wrong");
		}

		return sendMessage;
	}

	private SendMessage getResponseFromPagination(Update update, int offset, int limit, Pagination<Admin> pagination) {
		// message text
		StringBuilder builder = new StringBuilder("<b>Admins</b>").append("\n");
		long totalPages = pagination.getTotalItems() / limit;
		if (pagination.getTotalItems() % limit != 0) {
			totalPages++;
		}
		builder.append("Page ").append(offset / limit + 1).append(" of ").append(totalPages);

		// create message
		SendMessage sendMessage = new SendMessage(TelegramUtils.getChatId(update), builder.toString());
		sendMessage.parseMode(ParseMode.HTML);

		// prepare keyboard
		InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
		sendMessage.replyMarkup(keyboard);
		pagination.getElements().forEach(
				admin -> keyboard.addRow(new InlineKeyboardButton(admin.getId() + " - " + admin.getRole().name())));

		// add navigation buttons
		List<InlineKeyboardButton> navigationButtons = new ArrayList<>();
		if (offset > 0) {
			InlineKeyboardButton previous = new InlineKeyboardButton("Previous page");
			int newOffset = offset - limit >= 0 ? offset - limit : 0;
			previous.callbackData(COMMAND_LIST + " " + newOffset + " " + limit);
			navigationButtons.add(previous);
		}
		if (offset + limit < pagination.getTotalItems()) {
			InlineKeyboardButton next = new InlineKeyboardButton("Next page");
			int newOffset = offset + limit;
			next.callbackData(COMMAND_LIST + " " + newOffset + " " + limit);
			navigationButtons.add(next);
		}
		if (!navigationButtons.isEmpty()) {
			keyboard.addRow(navigationButtons.toArray(new InlineKeyboardButton[0]));
		}

		return sendMessage;
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
			adminService.insert(adminId, adminRole);
			sendMessage = new SendMessage(chatId, "Admin " + adminId + " with role " + adminRole.name() + " added");
		} else {
			sendMessage = new SendMessage(chatId, "There is something wrong with your message. Try again.");
		}

		sendMessage.replyToMessageId(messageId);

		return sendMessage;
	}

	private SendMessage removeUser(Update update) {
		SendMessage sendMessage = null;

		if (TelegramUtils.getText(update).equals(COMMAND_REMOVE)) {
			// ask the id
			StringBuilder builder = new StringBuilder(COMMAND_REMOVE).append("\n");
			builder.append("Send the ID of the user to remove").append("\n");
			String placeholder = "id";
			builder.append("Format: <code>").append(placeholder).append("</code>");
			sendMessage = new SendMessage(TelegramUtils.getChatId(update), builder.toString());
			sendMessage.parseMode(ParseMode.HTML);
			sendMessage.replyMarkup(new ForceReply().inputFieldPlaceholder(placeholder));
		} else if (update.message() != null && update.message().replyToMessage() != null) {
			// remove user
			sendMessage = removeUser(TelegramUtils.getChatId(update), update.message().text(),
					update.message().messageId());
		} else {
			sendMessage = new SendMessage(TelegramUtils.getChatId(update), "Something went wrong");
		}

		return sendMessage;
	}

	private SendMessage removeUser(Long chatId, String text, Integer messageId) {
		SendMessage sendMessage = null;

		// add user
		long adminId = Long.parseLong(text.trim());
		adminService.delete(adminId);
		sendMessage = new SendMessage(chatId, "Admin " + adminId + " removed");

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
		InlineKeyboardButton listButton = new InlineKeyboardButton("List").callbackData(COMMAND_LIST);
		InlineKeyboardButton addButton = new InlineKeyboardButton("Add").callbackData(COMMAND_ADD);
		InlineKeyboardButton removeButton = new InlineKeyboardButton("Remove").callbackData(COMMAND_REMOVE);

		return new InlineKeyboardMarkup(new InlineKeyboardButton[] { listButton },
				new InlineKeyboardButton[] { addButton, removeButton });
	}

}
