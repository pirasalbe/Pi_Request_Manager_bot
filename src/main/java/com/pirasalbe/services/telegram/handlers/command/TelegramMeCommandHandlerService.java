package com.pirasalbe.services.telegram.handlers.command;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat.Type;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.models.NextValidRequest;
import com.pirasalbe.models.Validation;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.database.Request;
import com.pirasalbe.models.database.RequestPK;
import com.pirasalbe.models.request.Format;
import com.pirasalbe.models.telegram.handlers.TelegramHandler;
import com.pirasalbe.services.AdminService;
import com.pirasalbe.services.RequestManagementService;
import com.pirasalbe.services.RequestService;
import com.pirasalbe.services.telegram.handlers.AbstractTelegramHandlerService;
import com.pirasalbe.utils.DateUtils;
import com.pirasalbe.utils.RequestUtils;
import com.pirasalbe.utils.TelegramUtils;

/**
 * Service to manage /me
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramMeCommandHandlerService extends AbstractTelegramHandlerService implements TelegramHandler {

	public static final String COMMAND = "/me";

	@Autowired
	private AdminService adminService;

	@Autowired
	private RequestService requestService;

	@Autowired
	private RequestManagementService requestManagementService;

	@Override
	public void handle(TelegramBot bot, Update update) {
		// get sender id
		Long userId = update.message().from().id();
		Integer messageId = update.message().messageId();
		if (update.message().replyToMessage() != null) {
			userId = update.message().replyToMessage().from().id();
			messageId = update.message().replyToMessage().messageId();
		}

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("<b>User info</b>:\n");
		stringBuilder.append("Id: <code>").append(userId).append("</code>\n");
		stringBuilder.append("Role: <code>").append(adminService.getAuthority(userId)).append("</code>\n");

		checkRequests(stringBuilder, userId, update.message().chat().id());

		SendMessage sendMessage = new SendMessage(update.message().chat().id(), stringBuilder.toString());
		sendMessage.replyToMessageId(messageId);
		sendMessage.parseMode(ParseMode.HTML);

		boolean delete = update.message().chat().type() != Type.Private;

		sendMessageAndDelete(bot, sendMessage, 10, TimeUnit.SECONDS, delete);
		deleteMessage(bot, update.message(), delete);
	}

	private void checkRequests(StringBuilder stringBuilder, Long userId, Long chatId) {
		Request ebookRequest = requestService.getLastEbookRequestOfUser(userId);
		if (ebookRequest != null) {
			stringBuilder.append("\n")
					.append(getRequestInfo(ebookRequest.getId(), ebookRequest.getRequestDate(), "Last ebook request"));

		}

		Request audiobookRequest = requestService.getLastAudiobookRequestOfUser(userId);
		if (audiobookRequest != null) {
			stringBuilder.append("\n").append(getRequestInfo(audiobookRequest.getId(),
					audiobookRequest.getRequestDate(), "Last audiobook request"));
		}

		Request audiobookResolved = requestService.getLastAudiobookResolvedOfUser(userId);
		if (audiobookResolved != null) {
			stringBuilder.append("\n").append(getRequestInfo(audiobookResolved.getId(),
					audiobookResolved.getResolvedDate(), "Last audiobook received"));
		}

		Optional<Group> group = groupService.findById(chatId);
		if (group.isPresent()) {
			LocalDateTime now = DateUtils.getNow();

			stringBuilder.append("\n\n");
			stringBuilder.append("<b>Limits:</b>\n");

			stringBuilder.append(checkRequestLimit(userId, group.get(), Format.EBOOK, now)).append("\n\n");
			stringBuilder.append(checkRequestLimit(userId, group.get(), Format.AUDIOBOOK, now));

		}
	}

	private String checkRequestLimit(Long userId, Group group, Format format, LocalDateTime requestDate) {
		Validation<NextValidRequest> validation = requestManagementService.canRequest(group, userId, format,
				requestDate);

		String result = null;

		if (validation.isValid()) {
			result = "You are allowed to request an " + format.name().toLowerCase();
		} else {
			result = validation.getReason().getMessage();
		}

		return result;
	}

	private String getRequestInfo(RequestPK id, LocalDateTime date, String description) {
		StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append("<a href='");
		stringBuilder.append(TelegramUtils.getLink(id.getGroupId().toString(), id.getMessageId().toString()));
		stringBuilder.append("'>").append(description).append("</a> ");
		stringBuilder.append(RequestUtils.getTimeBetweenDates(date, DateUtils.getNow())).append(" ago.");

		return stringBuilder.toString();
	}

}
