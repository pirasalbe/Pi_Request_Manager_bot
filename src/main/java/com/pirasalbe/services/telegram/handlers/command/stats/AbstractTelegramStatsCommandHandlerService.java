package com.pirasalbe.services.telegram.handlers.command.stats;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat.Type;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.models.UserRole;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.database.Request;
import com.pirasalbe.models.request.Format;
import com.pirasalbe.models.request.Source;
import com.pirasalbe.models.telegram.handlers.TelegramHandler;
import com.pirasalbe.services.AdminService;
import com.pirasalbe.services.RequestManagementService;
import com.pirasalbe.services.telegram.handlers.AbstractTelegramHandlerService;

/**
 * Service to manage stats
 *
 * @author pirasalbe
 *
 */
@Component
public abstract class AbstractTelegramStatsCommandHandlerService extends AbstractTelegramHandlerService
		implements TelegramHandler {

	public static final UserRole ROLE = UserRole.CONTRIBUTOR;

	@Autowired
	protected AdminService adminService;

	@Autowired
	protected RequestManagementService requestManagementService;

	@Override
	public void handle(TelegramBot bot, Update update) {
		// delete command
		deleteMessage(bot, update.message(), update.message().chat().type() != Type.Private);

		Long chatId = update.message().chat().id();

		// filters
		String text = update.message().text();

		boolean isPrivate = update.message().chat().type() == Type.Private;
		Optional<Long> group = getGroup(chatId, text, isPrivate);

		// check if the context is valid, either enabled group or PM
		if (groupService.existsById(chatId) || isPrivate) {
			SendMessage sendMessage = new SendMessage(chatId, "Preparing stats..");
			sendMessageAndDelete(bot, sendMessage, 10, TimeUnit.SECONDS);

			getAndSendStats(chatId, group, text);
		}
	}

	protected abstract void getAndSendStats(Long chatId, Optional<Long> group, String text);

	protected boolean checkFilters(Request request, Optional<Long> group, Optional<Long> user, Optional<Format> format,
			Optional<Source> source, Optional<String> otherTags) {
		boolean valid = true;

		if (group.isPresent()) {
			valid = request.getId().getGroupId().equals(group.get());
		}
		if (valid && user.isPresent()) {
			valid = request.getUserId().equals(user.get());
		}
		if (valid && format.isPresent()) {
			valid = request.getFormat().equals(format.get());
		}
		if (valid && source.isPresent()) {
			valid = request.getSource().equals(source.get());
		}
		if (valid && otherTags.isPresent()) {
			valid = request.getOtherTags().equals(otherTags.get());
		}

		return valid;
	}

	protected String getFilters(Optional<Long> group, Optional<Long> user, Optional<Format> format,
			Optional<Source> source, Optional<String> otherTags) {
		StringBuilder filters = new StringBuilder();
		if (group.isPresent()) {
			Long groupId = group.get();
			Optional<Group> groupOptional = groupService.findById(groupId);
			filters.append("\nGroup [").append(groupOptional.orElseThrow().getName()).append(" (<code>").append(groupId)
					.append("</code>)]");
		}
		if (user.isPresent()) {
			filters.append("\nUser [<code>").append(user.get()).append("</code>]");
		}
		if (format.isPresent()) {
			filters.append("\nFormat [").append(format.get()).append("]");
		}
		if (source.isPresent()) {
			filters.append("\nSource [").append(source.get()).append("]");
		}
		if (otherTags.isPresent()) {
			filters.append("\nOther [").append(otherTags.get()).append("]");
		}

		return filters.toString();
	}

	protected void sendMessage(Long chatId, String message) {
		SendMessage sendMessage = new SendMessage(chatId, message);
		sendMessage.parseMode(ParseMode.HTML);
		sendMessage.disableWebPagePreview(true);

		botQueue.add(bot -> bot.execute(sendMessage));
	}

}
