package com.pirasalbe.services.telegram.handlers.request;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;

import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.MessageEntity;
import com.pengrad.telegrambot.model.MessageEntity.Type;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.models.Format;
import com.pirasalbe.models.Source;
import com.pirasalbe.models.Validation;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.telegram.TelegramHandlerResult;
import com.pirasalbe.services.GroupService;
import com.pirasalbe.services.UserRequestService;
import com.pirasalbe.services.telegram.handlers.TelegramHandlerService;
import com.pirasalbe.utils.DateUtils;

/**
 * Service to manage requests from users
 *
 * @author pirasalbe
 *
 */
public abstract class AbstractTelegramRequestHandlerService implements TelegramHandlerService {

	protected static final String REQUEST_TAG = "#request";
	protected static final String EBOOK_TAG = "#ebook";
	protected static final String AUDIOBOOK_TAG = "#audiobook";
	protected static final String KU_TAG = "#ku";
	protected static final String ARCHIVE_TAG = "#archive";
	protected static final String STORYTEL_TAG = "#stortytel";
	protected static final String SCRIBD_TAG = "#scribd";

	protected static final List<String> KNOWN_TAGS = Arrays.asList(REQUEST_TAG, EBOOK_TAG, AUDIOBOOK_TAG, KU_TAG,
			ARCHIVE_TAG, STORYTEL_TAG, SCRIBD_TAG);

	@Autowired
	protected UserRequestService userRequestService;

	@Autowired
	protected GroupService groupService;

	protected boolean hasRequestTag(String text) {
		// messages with request tag
		return text.toLowerCase().contains(REQUEST_TAG);
	}

	protected TelegramHandlerResult newRequest(Message message, Long chatId, Group group) {
		String content = message.text();
		String link = getLink(content, message.entities());

		return newRequest(message, chatId, group, content, link);
	}

	protected TelegramHandlerResult newRequest(Message message, Long chatId, Group group, String content, String link) {
		TelegramHandlerResult result = TelegramHandlerResult.noResponse();

		Long userId = message.from().id();
		Integer timestampUnix = message.date();
		LocalDateTime timestamp = DateUtils.longToLocalDateTime(timestampUnix);

		Format format = getFormat(content);

		// check if user can request
		Validation validation = userRequestService.canRequest(group, userId, format);
		if (validation.isValid()) {
			// create request
			Source source = getSource(content, format);
			String otherTags = getOtherTags(content);

			userRequestService.insert(message.messageId().longValue(), content, link, format, source, otherTags, userId,
					group.getId(), timestamp);
		} else {
			DeleteMessage deleteMessage = new DeleteMessage(chatId, message.messageId());
			SendMessage sendMessage = new SendMessage(chatId,
					"<a href=\"tg://user?id=" + userId + "\">" + userId + "</a>. " + validation.getReason());
			sendMessage.parseMode(ParseMode.HTML);

			result = TelegramHandlerResult.withResponses(deleteMessage, sendMessage);
		}

		return result;
	}

	protected String getLink(String content, MessageEntity[] entities) {
		String link = null;

		for (MessageEntity entity : entities) {
			if (entity.type().equals(Type.text_link)) {
				link = entity.url();
			} else if (entity.type().equals(Type.url)) {
				link = content.substring(entity.offset(), entity.offset() + entity.length());
			}
		}

		return link;
	}

	protected String getOtherTags(String content) {
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

	protected Format getFormat(String content) {
		Format format = Format.EBOOK;

		if (content.toLowerCase().contains(AUDIOBOOK_TAG)) {
			format = Format.AUDIOBOOK;
		}

		return format;
	}

	protected Source getSource(String content, Format format) {
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
