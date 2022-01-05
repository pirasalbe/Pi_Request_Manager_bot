package com.pirasalbe.services.telegram.handler;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.models.Format;
import com.pirasalbe.models.Source;
import com.pirasalbe.models.Validation;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.telegram.TelegramHandlerResult;
import com.pirasalbe.services.telegram.GroupService;
import com.pirasalbe.services.telegram.UserRequestService;
import com.pirasalbe.utils.DateUtils;
import com.pirasalbe.utils.TelegramUtils;

/**
 * Service to manage requests from users
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramRequestHandlerService implements TelegramHandlerService {

	private static final String REQUEST_TAG = "#request";
	private static final String AUDIOBOOK_TAG = "#audiobook";
	private static final String KU_TAG = "#ku";
	private static final String ARCHIVE_TAG = "#archive";
	private static final String STORYTEL_TAG = "#stortytel";
	private static final String SCRIBD_TAG = "#scribd";

	private static final List<String> KNOWN_TAGS = Arrays.asList(REQUEST_TAG, AUDIOBOOK_TAG, KU_TAG, ARCHIVE_TAG,
			STORYTEL_TAG, SCRIBD_TAG);

	@Autowired
	protected UserRequestService userRequestService;

	@Autowired
	protected GroupService groupService;

	@Override
	public boolean shouldHandle(Update update) {
		// messages with request
		return update.message() != null && update.message().text().toLowerCase().contains(REQUEST_TAG);
		// TODO manage updates and deletes
		// TODO manage responses
	}

	@Override
	public TelegramHandlerResult handleUpdate(Update update) {
		TelegramHandlerResult result = TelegramHandlerResult.noResponse();

		String content = TelegramUtils.getText(update);
		Long chatId = TelegramUtils.getChatId(update);
		Long userId = TelegramUtils.getUserId(update);
		Integer timestampUnix = update.message().date();
		LocalDateTime timestamp = DateUtils.longToLocalDateTime(timestampUnix);

		// manage only requests from active groups
		Optional<Group> optional = groupService.findById(chatId);
		if (optional.isPresent()) {
			Group group = optional.get();
			Format format = getFormat(content);

			// check if user can request
			Validation validation = userRequestService.canRequest(group, userId, format);
			if (validation.isValid()) {
				// create request
				Source source = getSource(content, format);
				String otherTags = getOtherTags(content);

				userRequestService.insertRequest(content, format, source, otherTags, userId, group.getId(), timestamp);
			} else {
				DeleteMessage deleteMessage = new DeleteMessage(chatId, update.message().messageId());
				SendMessage sendMessage = new SendMessage(chatId,
						"<a href=\"tg://user?id=" + userId + "\">" + userId + "</a>. " + validation.getReason());
				sendMessage.parseMode(ParseMode.HTML);

				result = TelegramHandlerResult.withResponses(deleteMessage, sendMessage);
			}
		}

		return result;
	}

	private String getOtherTags(String content) {
		String otherTags = null;

		// split every word
		String[] parts = content.replace("\n", " ").split(" ");
		for (int i = 0; i < parts.length && otherTags == null; i++) {
			String part = parts[i];

			// if the word is a tag and is unknown
			if (Pattern.matches("^#.*", part) && !KNOWN_TAGS.contains(part)) {
				otherTags = part.substring(1);
			}
		}

		return otherTags;
	}

	private Format getFormat(String content) {
		Format format = Format.EBOOK;

		if (content.toLowerCase().contains(AUDIOBOOK_TAG)) {
			format = Format.AUDIOBOOK;
		}

		return format;
	}

	private Source getSource(String content, Format format) {
		Source source = format.equals(Format.EBOOK) ? Source.AMAZON : Source.AUDIBLE;

		String lowerContent = content.toLowerCase();
		if (format.equals(Format.EBOOK) && lowerContent.contains(KU_TAG)) {
			source = Source.KU;
		} else if (format.equals(Format.EBOOK) && lowerContent.contains(ARCHIVE_TAG)) {
			source = Source.ARCHIVE;
		} else if (lowerContent.contains(STORYTEL_TAG)) {
			source = Source.STORYTEL;
		} else if (lowerContent.contains(SCRIBD_TAG)) {
			source = Source.SCRIBD;
		}

		return source;
	}

}
