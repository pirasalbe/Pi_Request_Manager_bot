package com.pirasalbe.services.telegram.handlers.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.models.FormatAllowed;
import com.pirasalbe.models.UserRole;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.request.Source;
import com.pirasalbe.models.telegram.handlers.TelegramHandler;
import com.pirasalbe.services.GroupService;
import com.pirasalbe.services.telegram.handlers.AbstractTelegramHandlerService;
import com.pirasalbe.utils.RequestUtils;
import com.pirasalbe.utils.TelegramUtils;

/**
 * Service to manage groups related commands
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramGroupsCommandHandlerService extends AbstractTelegramHandlerService {

	public static final String COMMAND_INFO = "/group_info";
	public static final String COMMAND_ENABLE = "/enable_group";
	public static final String COMMAND_DISABLE = "/disable_group";
	public static final String COMMAND_REQUEST_LIMIT = "/request_limit";
	public static final String COMMAND_NONENGLISH_AUDIOBOOK_DAYS_WAIT = "/nonenglish_audiobooks_days_wait";
	public static final String COMMAND_ENGLISH_AUDIOBOOK_DAYS_WAIT = "/english_audiobooks_days_wait";
	public static final String COMMAND_ALLOW = "/allow";
	public static final String COMMAND_NO_REPEAT = "/no_repeat";

	private static final String ENABLE_THE_GROUP_FIRST = "Enable the group first with <code>" + COMMAND_ENABLE
			+ "</code>";

	public static final UserRole ROLE = UserRole.MANAGER;

	@Autowired
	private GroupService groupService;

	private void sendMessage(TelegramBot bot, Update update, SendMessage sendMessage) {
		sendMessageAndDelete(bot, sendMessage, 15, TimeUnit.SECONDS);
		deleteMessage(bot, update.message());
	}

	public TelegramHandler showInfo() {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);

			Optional<Group> optional = groupService.findById(chatId);

			SendMessage sendMessage = new SendMessage(chatId,
					optional.isPresent() ? optional.get().toString() : ENABLE_THE_GROUP_FIRST);
			sendMessage.parseMode(ParseMode.HTML);

			sendMessage(bot, update, sendMessage);
		};
	}

	public TelegramHandler enableGroup() {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);

			groupService.insertIfNotExists(chatId, update.message().chat().title());
			SendMessage sendMessage = new SendMessage(chatId, "Group enabled");

			sendMessage(bot, update, sendMessage);
		};
	}

	public TelegramHandler disableGroup() {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);

			groupService.deleteIfExists(chatId);
			SendMessage sendMessage = new SendMessage(chatId, "Group disabled");

			sendMessage(bot, update, sendMessage);
		};
	}

	private String rightFormatMessage(String command, String element) {
		StringBuilder builder = new StringBuilder();

		builder.append("The right format is: <code>");
		builder.append(command);
		builder.append("</code> [");
		builder.append(element);
		builder.append("]");

		return builder.toString();
	}

	public TelegramHandler updateRequestLimit() {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);
			String text = update.message().text();

			String message = null;

			String[] parts = text.split(" ");
			if (parts.length == 2) {
				int requestLimit = Integer.parseInt(parts[1]);
				boolean updateSuccess = groupService.updateRequestLimit(chatId, requestLimit);

				if (updateSuccess) {
					message = "Updated request limit to <b>" + requestLimit + "</b>";
				} else {
					message = ENABLE_THE_GROUP_FIRST;
				}
			} else {
				message = rightFormatMessage(COMMAND_REQUEST_LIMIT, "number of request per day");
			}

			SendMessage sendMessage = new SendMessage(chatId, message);
			sendMessage.parseMode(ParseMode.HTML);

			sendMessage(bot, update, sendMessage);
		};
	}

	public TelegramHandler updateAudiobooksDaysWait() {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);
			String text = update.message().text();

			String message = null;

			String[] parts = text.split(" ");
			if (parts.length == 2) {
				int daysWait = Integer.parseInt(parts[1]);
				boolean updateSuccess = groupService.updateAudiobooksDaysWait(chatId, daysWait);

				if (updateSuccess) {
					message = "Updated non-English audiobooks days wait to <b>" + daysWait + "</b>";
				} else {
					message = ENABLE_THE_GROUP_FIRST;
				}
			} else {
				message = rightFormatMessage(COMMAND_NONENGLISH_AUDIOBOOK_DAYS_WAIT, "number of days to wait");
			}

			SendMessage sendMessage = new SendMessage(chatId, message);
			sendMessage.parseMode(ParseMode.HTML);

			sendMessage(bot, update, sendMessage);
		};
	}

	public TelegramHandler updateEnglishAudiobooksDaysWait() {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);
			String text = update.message().text();

			String message = null;

			String[] parts = text.split(" ");
			if (parts.length == 2) {
				int daysWait = Integer.parseInt(parts[1]);
				boolean updateSuccess = groupService.updateEnglishAudiobooksDaysWait(chatId, daysWait);

				if (updateSuccess) {
					message = "Updated English audiobooks days wait to <b>" + daysWait + "</b>";
				} else {
					message = ENABLE_THE_GROUP_FIRST;
				}
			} else {
				message = rightFormatMessage(COMMAND_ENGLISH_AUDIOBOOK_DAYS_WAIT, "number of days to wait");
			}

			SendMessage sendMessage = new SendMessage(chatId, message);
			sendMessage.parseMode(ParseMode.HTML);

			sendMessage(bot, update, sendMessage);
		};
	}

	public TelegramHandler updateAllow() {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);
			String text = update.message().text();

			String message = null;

			String[] parts = text.split(" ");
			if (parts.length == 2) {
				String allowed = parts[1].toUpperCase();
				FormatAllowed formatAllowed = FormatAllowed.valueOf(allowed);
				boolean updateSuccess = groupService.updateAllow(chatId, formatAllowed);

				if (updateSuccess) {
					message = "Updated allowed to <b>" + allowed + "</b>";
				} else {
					message = ENABLE_THE_GROUP_FIRST;
				}
			} else {
				message = rightFormatMessage(COMMAND_ALLOW, "ebooks/audiobooks/both");
			}

			SendMessage sendMessage = new SendMessage(chatId, message);
			sendMessage.parseMode(ParseMode.HTML);

			sendMessage(bot, update, sendMessage);
		};
	}

	public TelegramHandler updateNoRepeat() {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);
			String text = update.message().text();

			String message = null;

			String[] parts = text.replace(", ", ",").split(" ");
			if (parts.length == 2) {
				String sources = parts[1];

				List<Source> noRepeatSources = getSources(sources);

				boolean updateSuccess = groupService.updateNoRepeat(chatId, noRepeatSources);

				if (updateSuccess) {
					message = "Not allowed to repeat <b>" + noRepeatSources + "</b>";
				} else {
					message = ENABLE_THE_GROUP_FIRST;
				}
			} else {
				message = rightFormatMessage(COMMAND_NO_REPEAT, Arrays.asList(Source.values()).toString());
			}

			SendMessage sendMessage = new SendMessage(chatId, message);
			sendMessage.parseMode(ParseMode.HTML);

			sendMessage(bot, update, sendMessage);
		};
	}

	private List<Source> getSources(String sources) {
		List<Source> noRepeatSources = null;

		try {
			noRepeatSources = RequestUtils.getNoRepeatSources(sources);
		} catch (Exception e) {
			noRepeatSources = new ArrayList<>();
		}

		return noRepeatSources;
	}

}
