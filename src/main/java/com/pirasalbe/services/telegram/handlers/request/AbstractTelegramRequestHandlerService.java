package com.pirasalbe.services.telegram.handlers.request;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.ChatPermissions;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.RestrictChatMember;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pirasalbe.configurations.ErrorConfiguration;
import com.pirasalbe.models.NextValidRequest;
import com.pirasalbe.models.RequestResult;
import com.pirasalbe.models.Validation;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.request.Format;
import com.pirasalbe.models.request.Source;
import com.pirasalbe.models.telegram.handlers.TelegramCondition;
import com.pirasalbe.models.telegram.handlers.TelegramHandler;
import com.pirasalbe.services.GroupService;
import com.pirasalbe.services.RequestManagementService;
import com.pirasalbe.utils.DateUtils;
import com.pirasalbe.utils.RequestUtils;
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
	protected static final String STORYTEL_TAG = "#storytel";
	protected static final String SCRIBD_TAG = "#scribd";

	protected static final List<String> KNOWN_TAGS = Arrays.asList(REQUEST_TAG, EBOOK_TAG, AUDIOBOOK_TAG, KU_TAG,
			ARCHIVE_TAG, STORYTEL_TAG, SCRIBD_TAG);

	@Autowired
	protected ErrorConfiguration errorConfiguration;

	@Autowired
	protected RequestManagementService requestManagementService;

	@Autowired
	protected GroupService groupService;

	/**
	 * Get the message from the update
	 *
	 * @return Message
	 */
	protected abstract Message getMessage(Update update);

	public TelegramCondition getCondition() {
		// messages with request
		return update -> getRequestMessage(update) != null;
	}

	protected Message getRequestMessage(Update update) {
		Message message = null;

		Message updateMessage = getMessage(update);

		// direct request
		if (updateMessage != null && hasRequestTag(updateMessage.text())) {
			message = updateMessage;
		}

		return message;
	}

	protected boolean hasRequestTag(String text) {
		// messages with request tag
		return text != null && text.toLowerCase().contains(REQUEST_TAG);
	}

	protected void newRequest(TelegramBot bot, Message message, Long chatId, Integer messageId,
			LocalDateTime requestTime, Group group) {
		String content = RequestUtils.getContent(message.text(), message.entities());
		String link = RequestUtils.getLink(message.text(), message.entities());

		if (link != null) {
			newRequest(bot, message, chatId, messageId, requestTime, group, content, link);
		} else {
			manageIncompleteRequest(bot, message, chatId);
		}
	}

	protected void manageIncompleteRequest(TelegramBot bot, Message message, Long chatId) {
		// notify user of the error
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(TelegramUtils.tagUser(message));
		stringBuilder.append(errorConfiguration.getIncompleteRequest());
		SendMessage sendMessage = new SendMessage(chatId, stringBuilder.toString());
		sendMessage.replyToMessageId(message.messageId());
		sendMessage.parseMode(ParseMode.HTML);

		bot.execute(sendMessage);
	}

	protected void newRequest(TelegramBot bot, Message message, Long chatId, Integer messageId,
			LocalDateTime requestTime, Group group, String content, String link) {
		Long userId = message.from().id();

		Format format = getFormat(content);

		// check if user can request
		Validation<NextValidRequest> validation = requestManagementService.canRequest(group, userId, format,
				requestTime);
		if (validation.isValid()) {
			// create request
			manageRequest(bot, message, chatId, messageId, requestTime, group, content, link, format);
		} else {
			NextValidRequest nextValidRequest = validation.getReason();

			// mute user
			boolean muted = muteUser(bot, chatId, userId, nextValidRequest);

			// notify user of the error
			DeleteMessage deleteMessage = new DeleteMessage(chatId, messageId);
			bot.execute(deleteMessage);

			StringBuilder builder = new StringBuilder(TelegramUtils.tagUser(message));
			builder.append(nextValidRequest.getMessage());
			if (muted) {
				builder.append("\n").append("You have been muted until then.");
			}

			SendMessage sendMessage = new SendMessage(chatId, builder.toString());
			sendMessage.parseMode(ParseMode.HTML);
			bot.execute(sendMessage);

		}
	}

	private boolean muteUser(TelegramBot bot, Long chatId, Long userId, NextValidRequest nextValidRequest) {
		boolean restricted = false;

		LocalDateTime nextRequestDate = nextValidRequest.getNextRequest();

		// mute if a date has been provided and it's more than 30 seconds
		if (nextRequestDate != null && DateUtils.getSeconds(DateUtils.getNow(), nextRequestDate) > 60) {
			Integer muteUntil = DateUtils.localDateTimeToInteger(nextRequestDate);

			ChatPermissions chatPermissions = new ChatPermissions();
			chatPermissions.canSendMessages(false);
			RestrictChatMember restrictChatMember = new RestrictChatMember(chatId, userId, chatPermissions);
			restrictChatMember.untilDate(muteUntil);

			BaseResponse response = bot.execute(restrictChatMember);
			restricted = response.isOk();
		}

		return restricted;
	}

	private void manageRequest(TelegramBot bot, Message message, Long chatId, Integer messageId,
			LocalDateTime requestTime, Group group, String content, String link, Format format) {
		Long userId = message.from().id();

		Source source = getSource(content, format);
		String otherTags = getOtherTags(content);

		RequestResult requestResult = requestManagementService.manageRequest(messageId.longValue(), content, link,
				format, source, otherTags, userId, group, requestTime);

		manageRequestResult(bot, message, chatId, messageId, requestResult);
	}

	private void manageRequestResult(TelegramBot bot, Message message, Long chatId, Integer messageId,
			RequestResult requestResult) {

		switch (requestResult.getResult()) {
		case CANNOT_REPEAT_REQUEST:
		case REQUEST_REPEATED_TOO_EARLY:
			// notify user of the error
			DeleteMessage deleteMessage = new DeleteMessage(chatId, messageId);
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(TelegramUtils.tagUser(message));
			stringBuilder.append(requestResult.getReason());
			SendMessage sendMessage = new SendMessage(chatId, stringBuilder.toString());
			sendMessage.parseMode(ParseMode.HTML);

			bot.execute(deleteMessage);
			bot.execute(sendMessage);
			break;
		default:
			break;
		}
	}

	protected String getOtherTags(String content) {
		String otherTags = RequestUtils.OTHER_TAGS_ENGLISH;

		// split every word
		boolean found = false;
		String[] parts = content.replace("\n", " ").split(" ");
		for (int i = 0; i < parts.length && !found; i++) {
			String part = parts[i].toLowerCase();

			// if the word is a tag and is unknown
			if (Pattern.matches("^#[a-zA-Z]*", part) && !KNOWN_TAGS.contains(part)) {
				otherTags = part.substring(1);
				found = true;
			}
		}

		return otherTags.toLowerCase();
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
