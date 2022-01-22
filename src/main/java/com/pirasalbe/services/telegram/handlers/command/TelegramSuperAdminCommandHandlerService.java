package com.pirasalbe.services.telegram.handlers.command;

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
import com.pirasalbe.models.Pagination;
import com.pirasalbe.models.UserRole;
import com.pirasalbe.models.database.Admin;
import com.pirasalbe.models.telegram.handlers.TelegramHandler;
import com.pirasalbe.services.AdminService;
import com.pirasalbe.utils.TelegramUtils;

/**
 * Service to manage SuperAdmin commands
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramSuperAdminCommandHandlerService {

	private static final String SOMETHING_WENT_WRONG = "Something went wrong";

	public static final String COMMAND = "/admins";
	public static final String COMMAND_LIST = COMMAND + "_list";
	public static final String COMMAND_COPY = COMMAND + "_copy";
	public static final String COMMAND_ADD = COMMAND + "_add";
	public static final String COMMAND_REMOVE = COMMAND + "_remove";

	public static final UserRole ROLE = UserRole.SUPERADMIN;

	@Autowired
	private AdminService adminService;

	/**
	 * Show a keyboard with available commands
	 *
	 * @param message Message received
	 * @return Response
	 */
	public TelegramHandler showActions() {
		return (bot, update) -> {
			SendMessage sendMessage = new SendMessage(TelegramUtils.getChatId(update), "What do you want to do?");
			sendMessage.replyToMessageId(TelegramUtils.getMessageId(update));

			sendMessage.replyMarkup(getAdminsKeyboard());

			bot.execute(sendMessage);
		};
	}

	private Keyboard getAdminsKeyboard() {
		InlineKeyboardButton listButton = new InlineKeyboardButton("List").callbackData(COMMAND_LIST);
		InlineKeyboardButton addButton = new InlineKeyboardButton("Add").callbackData(COMMAND_ADD);
		InlineKeyboardButton removeButton = new InlineKeyboardButton("Remove").callbackData(COMMAND_REMOVE);

		return new InlineKeyboardMarkup(new InlineKeyboardButton[] { listButton },
				new InlineKeyboardButton[] { addButton, removeButton });
	}

	public TelegramHandler listUsers() {
		return (bot, update) -> {
			int page = 0;
			int size = 10;

			// get limit and offset from string
			String[] parts = update.callbackQuery().data().substring(COMMAND_LIST.length()).trim().split(" ");
			if (parts.length == 2) {
				page = Integer.parseInt(parts[0]);
				size = Integer.parseInt(parts[1]);
			}

			Pagination<Admin> pagination = adminService.list(page, size);
			SendMessage sendMessage = getResponseFromPagination(update, page, size, pagination);

			bot.execute(sendMessage);
		};
	}

	public TelegramHandler copyUser() {
		return (bot, update) -> {
			// get id from message
			String id = update.callbackQuery().data().substring(COMMAND_COPY.length()).trim();

			SendMessage sendMessage = new SendMessage(TelegramUtils.getChatId(update), "<code>" + id + "</code>");
			sendMessage.parseMode(ParseMode.HTML);

			bot.execute(sendMessage);
		};
	}

	private SendMessage getResponseFromPagination(Update update, int page, int size, Pagination<Admin> pagination) {
		// message text
		StringBuilder builder = new StringBuilder("<b>Admins</b>").append("\n");
		builder.append("Page ").append(page + 1).append(" of ").append(pagination.getTotalPages());

		// create message
		SendMessage sendMessage = new SendMessage(TelegramUtils.getChatId(update), builder.toString());
		sendMessage.parseMode(ParseMode.HTML);

		// prepare keyboard
		InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
		sendMessage.replyMarkup(keyboard);
		pagination.getElements().forEach(
				admin -> keyboard.addRow(new InlineKeyboardButton(admin.getId() + " - " + admin.getRole().name())
						.callbackData(COMMAND_COPY + " " + admin.getId())));

		// add navigation buttons
		List<InlineKeyboardButton> navigationButtons = new ArrayList<>();
		if (page > 0) {
			InlineKeyboardButton previous = new InlineKeyboardButton("Previous page");
			int newOffset = page - size >= 0 ? page - size : 0;
			previous.callbackData(COMMAND_LIST + " " + newOffset + " " + size);
			navigationButtons.add(previous);
		}
		if (page < pagination.getTotalPages() - 1) {
			InlineKeyboardButton next = new InlineKeyboardButton("Next page");
			int newOffset = page + size;
			next.callbackData(COMMAND_LIST + " " + newOffset + " " + size);
			navigationButtons.add(next);
		}
		if (!navigationButtons.isEmpty()) {
			keyboard.addRow(navigationButtons.toArray(new InlineKeyboardButton[0]));
		}

		return sendMessage;
	}

	public TelegramHandler addUserHelp() {
		return (bot, update) -> {
			// ask the id
			StringBuilder builder = new StringBuilder(COMMAND_ADD).append("\n");
			builder.append("Send the ID of the user and the role").append("\n");
			String placeholder = "id " + UserRole.getRoles();
			builder.append("Format: <code>").append(placeholder).append("</code>");
			SendMessage sendMessage = new SendMessage(TelegramUtils.getChatId(update), builder.toString());
			sendMessage.parseMode(ParseMode.HTML);
			sendMessage.replyMarkup(new ForceReply().inputFieldPlaceholder(placeholder));

			bot.execute(sendMessage);
		};
	}

	public TelegramHandler addUser() {
		return (bot, update) -> {
			// add user
			SendMessage sendMessage = addUser(TelegramUtils.getChatId(update), update.message().text(),
					update.message().messageId());

			bot.execute(sendMessage);
		};
	}

	private SendMessage addUser(Long chatId, String text, Integer messageId) {
		SendMessage sendMessage = null;

		String[] parts = text.trim().split(" ");
		if (parts.length == 2) {
			// add user
			long adminId = Long.parseLong(parts[0]);
			UserRole adminRole = UserRole.valueOf(parts[1].toUpperCase());
			adminService.insertUpdate(adminId, adminRole);
			sendMessage = new SendMessage(chatId, "Admin " + adminId + " with role " + adminRole.name() + " added");
		} else {
			sendMessage = new SendMessage(chatId, "There is something wrong with your message. Try again.");
		}

		sendMessage.replyToMessageId(messageId);

		return sendMessage;
	}

	public TelegramHandler removeUserHelp() {
		return (bot, update) -> {
			// ask the id
			StringBuilder builder = new StringBuilder(COMMAND_REMOVE).append("\n");
			builder.append("Send the ID of the user to remove").append("\n");
			String placeholder = "id";
			builder.append("Format: <code>").append(placeholder).append("</code>");
			SendMessage sendMessage = new SendMessage(TelegramUtils.getChatId(update), builder.toString());
			sendMessage.parseMode(ParseMode.HTML);
			sendMessage.replyMarkup(new ForceReply().inputFieldPlaceholder(placeholder));

			bot.execute(sendMessage);
		};
	}

	public TelegramHandler removeUser() {
		return (bot, update) -> {
			// remove user
			SendMessage sendMessage = removeUser(TelegramUtils.getChatId(update), update.message().text(),
					update.message().messageId());

			bot.execute(sendMessage);
		};
	}

	private SendMessage removeUser(Long chatId, String text, Integer messageId) {
		SendMessage sendMessage = null;

		// add user
		long adminId = Long.parseLong(text.trim());
		adminService.deleteIfExists(adminId);
		sendMessage = new SendMessage(chatId, "Admin " + adminId + " removed");

		sendMessage.replyToMessageId(messageId);

		return sendMessage;
	}

}
