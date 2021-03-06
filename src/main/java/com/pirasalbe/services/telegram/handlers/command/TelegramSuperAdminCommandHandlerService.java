package com.pirasalbe.services.telegram.handlers.command;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ForceReply;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.DeleteMessage;
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

	private void deleteMessage(TelegramBot bot, Long chatId, Integer messageId) {
		DeleteMessage deleteMessage = new DeleteMessage(chatId, messageId);
		bot.execute(deleteMessage);
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

				deleteMessage(bot, update.callbackQuery().from().id(), update.callbackQuery().message().messageId());
			}

			emptyAnswerCallbackQuery(bot, update.callbackQuery().id());

			Pagination<Admin> pagination = adminService.list(page, size);
			SendMessage sendMessage = getResponseFromPagination(update, page, size, pagination);

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
		pagination.getElements().forEach(admin -> keyboard.addRow(
				new InlineKeyboardButton(admin.getName() + " - (" + admin.getId() + ") - " + admin.getRole().name())
						.callbackData(COMMAND_COPY + " " + admin.getId())));

		// add navigation buttons
		List<InlineKeyboardButton> navigationButtons = new ArrayList<>();
		if (page > 0) {
			InlineKeyboardButton previous = new InlineKeyboardButton("Previous page");
			int newPage = page - 1;
			previous.callbackData(COMMAND_LIST + " " + newPage + " " + size);
			navigationButtons.add(previous);
		}
		if (page < pagination.getTotalPages() - 1) {
			InlineKeyboardButton next = new InlineKeyboardButton("Next page");
			int newPage = page + 1;
			next.callbackData(COMMAND_LIST + " " + newPage + " " + size);
			navigationButtons.add(next);
		}
		if (!navigationButtons.isEmpty()) {
			keyboard.addRow(navigationButtons.toArray(new InlineKeyboardButton[0]));
		}

		return sendMessage;
	}

	private void emptyAnswerCallbackQuery(TelegramBot bot, String id) {
		AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery(id);
		bot.execute(answerCallbackQuery);
	}

	public TelegramHandler copyUser() {
		return (bot, update) -> {
			// get id from message
			String id = update.callbackQuery().data().substring(COMMAND_COPY.length()).trim();

			emptyAnswerCallbackQuery(bot, update.callbackQuery().id());

			SendMessage sendMessage = new SendMessage(TelegramUtils.getChatId(update), "<code>" + id + "</code>");
			sendMessage.parseMode(ParseMode.HTML);

			bot.execute(sendMessage);
		};
	}

	public TelegramHandler addUserHelp() {
		return (bot, update) -> {
			// ask the id
			StringBuilder builder = new StringBuilder(COMMAND_ADD).append("\n");
			builder.append("Send the ID of the user and the role").append("\n");
			builder.append("Format: id name <code>").append(UserRole.getRoles()).append("</code>");
			SendMessage sendMessage = new SendMessage(TelegramUtils.getChatId(update), builder.toString());
			sendMessage.parseMode(ParseMode.HTML);
			sendMessage.replyMarkup(new ForceReply().inputFieldPlaceholder("id name " + UserRole.getRoles()));

			emptyAnswerCallbackQuery(bot, update.callbackQuery().id());

			bot.execute(sendMessage);
		};
	}

	public TelegramHandler addUser() {
		return (bot, update) -> {
			String text = TelegramUtils.removeCommand(update.message().text(), update.message().entities());
			if (text.isEmpty()) {
				text = update.message().text();
			}

			// add user
			SendMessage sendMessage = addUser(TelegramUtils.getChatId(update), text);

			if (update.message().replyToMessage() != null) {
				deleteMessage(bot, update.message().chat().id(), update.message().replyToMessage().messageId());
			}
			deleteMessage(bot, update.message().chat().id(), update.message().messageId());

			bot.execute(sendMessage);
		};
	}

	private SendMessage addUser(Long chatId, String text) {
		SendMessage sendMessage = null;

		String[] parts = text.trim().split(" ");
		if (parts.length == 3) {
			// add user
			long adminId = Long.parseLong(parts[0]);
			if (chatId != adminId) {
				String name = parts[1];
				UserRole adminRole = UserRole.valueOf(parts[2].toUpperCase());
				adminService.insertUpdate(adminId, name, adminRole);
				sendMessage = new SendMessage(chatId,
						"Admin " + name + " (" + adminId + ") with role " + adminRole.name() + " added");
			} else {
				sendMessage = new SendMessage(chatId, "You can't add/edit your user");
			}
		} else {
			sendMessage = new SendMessage(chatId, "There is something wrong with your message. Try again.");
		}

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

			emptyAnswerCallbackQuery(bot, update.callbackQuery().id());

			bot.execute(sendMessage);
		};
	}

	public TelegramHandler removeUser() {
		return (bot, update) -> {
			String text = TelegramUtils.removeCommand(update.message().text(), update.message().entities());
			if (text.isEmpty()) {
				text = update.message().text();
			}

			// remove user
			SendMessage sendMessage = removeUser(TelegramUtils.getChatId(update), text);

			if (update.message().replyToMessage() != null) {
				deleteMessage(bot, update.message().chat().id(), update.message().replyToMessage().messageId());
			}
			deleteMessage(bot, update.message().chat().id(), update.message().messageId());

			bot.execute(sendMessage);
		};
	}

	private SendMessage removeUser(Long chatId, String text) {
		SendMessage sendMessage = null;

		// add user
		long adminId = Long.parseLong(text.trim());
		if (chatId != adminId) {
			adminService.deleteIfExists(adminId);
			sendMessage = new SendMessage(chatId, "Admin " + adminId + " removed");
		} else {
			sendMessage = new SendMessage(chatId, "You can't remove your user");
		}

		return sendMessage;
	}

}
