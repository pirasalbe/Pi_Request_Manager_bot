package com.pirasalbe.services.telegram.handlers.request;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.MessageEntity;
import com.pengrad.telegrambot.model.MessageEntity.Type;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.models.RequestResult;
import com.pirasalbe.models.Validation;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.request.Format;
import com.pirasalbe.models.request.Source;
import com.pirasalbe.models.telegram.handlers.TelegramHandler;
import com.pirasalbe.services.GroupService;
import com.pirasalbe.services.RequestManagementService;
import com.pirasalbe.services.RequestService;
import com.pirasalbe.services.UserRequestService;
import com.pirasalbe.utils.TelegramUtils;

/**
 * Service to manage requests from users
 *
 * @author pirasalbe
 *
 */
public abstract class AbstractTelegramRequestHandlerService implements TelegramHandler {

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
	protected RequestManagementService requestManagementService;

	@Autowired
	protected UserRequestService userRequestService;

	@Autowired
	protected RequestService requestService;

	@Autowired
	protected GroupService groupService;

	protected boolean hasRequestTag(String text) {
		// messages with request tag
		return text != null && text.toLowerCase().contains(REQUEST_TAG);
	}

	protected void newRequest(TelegramBot bot, Message message, Long chatId, LocalDateTime requestTime, Group group) {
		String content = message.text();
		String link = getLink(content, message.entities());

		newRequest(bot, message, chatId, requestTime, group, content, link);
	}

	protected void newRequest(TelegramBot bot, Message message, Long chatId, LocalDateTime requestTime, Group group,
			String content, String link) {
		Long userId = message.from().id();

		Format format = getFormat(content);

		// check if user can request
		Validation validation = userRequestService.canRequest(group, userId, format, requestTime);
		if (validation.isValid()) {
			// create request
			manageRequest(bot, message, chatId, message.messageId(), requestTime, group.getId(), content, link, format);
		} else {
			// notify user of the error
			DeleteMessage deleteMessage = new DeleteMessage(chatId, message.messageId());
			SendMessage sendMessage = new SendMessage(chatId, tagUser(message) + validation.getReason());
			sendMessage.parseMode(ParseMode.HTML);

			bot.execute(deleteMessage);
			bot.execute(sendMessage);
		}
	}

	private String tagUser(Message message) {
		User user = message.from();

		return "<a href=\"tg://user?id=" + user.id() + "\">" + TelegramUtils.getUserName(user) + "</a>. ";
	}

	private void manageRequest(TelegramBot bot, Message message, Long chatId, Integer messageId,
			LocalDateTime requestTime, Long groupId, String content, String link, Format format) {
		Long userId = message.from().id();

		Source source = getSource(content, format);
		String otherTags = getOtherTags(content);

		RequestResult requestResult = requestManagementService.manageRequest(messageId.longValue(), content, link,
				format, source, otherTags, userId, groupId, requestTime);

		if (requestResult.getResult() == RequestResult.Result.REQUEST_REPEATED_TOO_EARLY) {
			// notify user of the error
			DeleteMessage deleteMessage = new DeleteMessage(chatId, messageId);
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(tagUser(message));
			stringBuilder.append(requestResult.getReason());
			SendMessage sendMessage = new SendMessage(chatId, stringBuilder.toString());
			sendMessage.parseMode(ParseMode.HTML);

			bot.execute(deleteMessage);
			bot.execute(sendMessage);
		}
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
			String part = parts[i].toLowerCase();

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
