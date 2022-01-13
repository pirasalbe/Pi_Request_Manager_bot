package com.pirasalbe.services.telegram.handlers.command;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Chat.Type;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.models.FormatAllowed;
import com.pirasalbe.models.UserRole;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.telegram.TelegramHandlerResult;
import com.pirasalbe.services.GroupService;
import com.pirasalbe.utils.TelegramUtils;

/**
 * Service to manage groups related commands
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramGroupsCommandHandlerService implements TelegramCommandHandler {

	static final String INFO_COMMAND = "/group_info";
	static final String ENABLE_COMMAND = "/enable_group";
	static final String DISABLE_COMMAND = "/disable_group";
	static final List<String> COMMANDS = Arrays.asList(INFO_COMMAND, ENABLE_COMMAND, DISABLE_COMMAND);

	static final String REQUEST_LIMIT_COMMAND = "/request_limit";
	static final String AUDIOBOOK_DAYS_WAIT_COMMAND = "/audiobooks_days_wait";
	static final String ENGLISH_AUDIOBOOK_DAYS_WAIT_COMMAND = "/english_audiobooks_days_wait";
	static final String ALLOW_COMMAND = "/allow";

	private static final String ENABLE_THE_GROUP_FIRST = "Enable the group first with <code>" + ENABLE_COMMAND
			+ "</code>";

	private static final UserRole ROLE = UserRole.MANAGER;

	@Autowired
	private GroupService groupService;

	@Override
	public boolean shouldHandle(Update update) {
		String text = update.message() != null ? TelegramUtils.getTextCommand(update) : null;

		return text != null && (COMMANDS.contains(text) || startsWith(text, REQUEST_LIMIT_COMMAND,
				AUDIOBOOK_DAYS_WAIT_COMMAND, ENGLISH_AUDIOBOOK_DAYS_WAIT_COMMAND, ALLOW_COMMAND))
		// command vaild only on groups
				&& (update.message().chat().type() == Type.group || update.message().chat().type() == Type.supergroup);
	}

	private boolean startsWith(String text, String... commands) {
		boolean startsWith = false;

		for (int i = 0; i < commands.length && !startsWith; i++) {
			String command = commands[i];

			startsWith = text.startsWith(command);
		}

		return startsWith;
	}

	@Override
	public UserRole getRequiredRole() {
		return ROLE;
	}

	@Override
	public TelegramHandlerResult handleCommand(Update update) {
		SendMessage sendMessage = null;

		Long chatId = TelegramUtils.getChatId(update);
		String text = TelegramUtils.getTextCommand(update);

		if (text.equals(INFO_COMMAND)) {
			Optional<Group> optional = groupService.findById(chatId);
			sendMessage = new SendMessage(chatId,
					optional.isPresent() ? optional.get().toString() : ENABLE_THE_GROUP_FIRST);
			sendMessage.parseMode(ParseMode.HTML);
		} else if (text.equals(ENABLE_COMMAND)) {
			groupService.insertIfNotExists(chatId);
			sendMessage = new SendMessage(chatId, "Group enabled");
		} else if (text.equals(DISABLE_COMMAND)) {
			groupService.deleteIfExists(chatId);
			sendMessage = new SendMessage(chatId, "Group disabled");
		} else if (text.startsWith(REQUEST_LIMIT_COMMAND)) {
			sendMessage = updateRequestLimit(chatId, update.message().text());
		} else if (text.startsWith(AUDIOBOOK_DAYS_WAIT_COMMAND)) {
			sendMessage = updateAudiobooksDaysWait(chatId, update.message().text());
		} else if (text.startsWith(ENGLISH_AUDIOBOOK_DAYS_WAIT_COMMAND)) {
			sendMessage = updateEnglishAudiobooksDaysWait(chatId, update.message().text());
		} else if (text.startsWith(ALLOW_COMMAND)) {
			sendMessage = updateAllow(chatId, update.message().text());
		} else {
			sendMessage = new SendMessage(chatId, SOMETHING_WENT_WRONG);
		}

		sendMessage.replyToMessageId(update.message().messageId());

		return TelegramHandlerResult.withResponses(sendMessage);
	}

	private SendMessage updateRequestLimit(Long chatId, String text) {
		String message = null;

		String[] parts = text.split(" ");
		if (parts.length == 2) {
			int requestLimit = Integer.parseInt(parts[1]);
			boolean update = groupService.updateRequestLimit(chatId, requestLimit);

			if (update) {
				message = "Updated request limit to <b>" + requestLimit + "</b>";
			} else {
				message = ENABLE_THE_GROUP_FIRST;
			}
		} else {
			message = "The right format is: <code>" + REQUEST_LIMIT_COMMAND + "</code> [number of request per day]";
		}

		SendMessage sendMessage = new SendMessage(chatId, message);
		sendMessage.parseMode(ParseMode.HTML);

		return sendMessage;
	}

	private SendMessage updateAudiobooksDaysWait(Long chatId, String text) {
		String message = null;

		String[] parts = text.split(" ");
		if (parts.length == 2) {
			int daysWait = Integer.parseInt(parts[1]);
			boolean update = groupService.updateAudiobooksDaysWait(chatId, daysWait);

			if (update) {
				message = "Updated audiobooks days wait to <b>" + daysWait + "</b>";
			} else {
				message = ENABLE_THE_GROUP_FIRST;
			}
		} else {
			message = "The right format is: <code>" + AUDIOBOOK_DAYS_WAIT_COMMAND + "</code> [number of days to wait]";
		}

		SendMessage sendMessage = new SendMessage(chatId, message);
		sendMessage.parseMode(ParseMode.HTML);

		return sendMessage;
	}

	private SendMessage updateEnglishAudiobooksDaysWait(Long chatId, String text) {
		String message = null;

		String[] parts = text.split(" ");
		if (parts.length == 2) {
			int daysWait = Integer.parseInt(parts[1]);
			boolean update = groupService.updateEnglishAudiobooksDaysWait(chatId, daysWait);

			if (update) {
				message = "Updated English audiobooks days wait to <b>" + daysWait + "</b>";
			} else {
				message = ENABLE_THE_GROUP_FIRST;
			}
		} else {
			message = "The right format is: <code>" + ENGLISH_AUDIOBOOK_DAYS_WAIT_COMMAND
					+ "</code> [number of days to wait]";
		}

		SendMessage sendMessage = new SendMessage(chatId, message);
		sendMessage.parseMode(ParseMode.HTML);

		return sendMessage;
	}

	private SendMessage updateAllow(Long chatId, String text) {
		String message = null;

		String[] parts = text.split(" ");
		if (parts.length == 2) {
			String allowed = parts[1].toUpperCase();
			FormatAllowed formatAllowed = FormatAllowed.valueOf(allowed);
			boolean update = groupService.updateAllow(chatId, formatAllowed);

			if (update) {
				message = "Updated allowed to <b>" + allowed + "</b>";
			} else {
				message = ENABLE_THE_GROUP_FIRST;
			}
		} else {
			message = "The right format is: <code>" + ALLOW_COMMAND + "</code> [ebooks/audiobooks/both]";
		}

		SendMessage sendMessage = new SendMessage(chatId, message);
		sendMessage.parseMode(ParseMode.HTML);

		return sendMessage;
	}

}
