package com.pirasalbe.services.telegram.handlers.command;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.request.Format;
import com.pirasalbe.models.request.Source;
import com.pirasalbe.models.telegram.handlers.TelegramCondition;
import com.pirasalbe.models.telegram.handlers.TelegramHandler;
import com.pirasalbe.services.GroupService;
import com.pirasalbe.services.RequestService;
import com.pirasalbe.services.telegram.handlers.AbstractTelegramHandlerService;
import com.pirasalbe.utils.DateUtils;
import com.pirasalbe.utils.RequestUtils;
import com.pirasalbe.utils.TelegramUtils;

/**
 * Service to manage /help
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramNextAudiobookCommandHandlerService extends AbstractTelegramHandlerService
		implements TelegramHandler, TelegramCondition {

	public static final String COMMAND = "/next_audiobook";

	@Autowired
	private RequestService requestService;

	@Autowired
	private GroupService groupService;

	@Override
	public boolean check(Update update) {
		return update.message() != null && update.message().replyToMessage() != null;
	}

	@Override
	public void handle(TelegramBot bot, Update update) {
		Long groupId = TelegramUtils.getChatId(update);
		User user = update.message().replyToMessage().from();
		Long userId = user.id();

		deleteMessage(bot, update.message());

		Optional<Group> optional = groupService.findById(groupId);
		if (optional.isPresent()) {

			String removeCommand = TelegramUtils.removeCommand(update.message().text(), update.message().entities())
					.trim();

			if (!removeCommand.isEmpty()) {
				// determine request date
				int days = Integer.parseInt(removeCommand);
				LocalDateTime commandDate = DateUtils.integerToLocalDateTime(update.message().date());

				LocalDateTime nextValidRequest = commandDate.plusDays(days);
				LocalDateTime requestDate = nextValidRequest.minusDays(optional.get().getEnglishAudiobooksDaysWait());

				// notify user
				String text = TelegramUtils.tagUser(user) + ""
						+ RequestUtils.getComeBackAgain(commandDate, nextValidRequest);

				SendMessage sendMessage = new SendMessage(groupId, text);
				sendMessage.parseMode(ParseMode.HTML);

				SendResponse response = bot.execute(sendMessage);

				Integer messageId = null;

				if (response.isOk()) {
					messageId = response.message().messageId();
				} else {
					messageId = update.message().replyToMessage().messageId();
				}

				// add request
				requestService.insert(messageId.longValue(), groupId,
						TelegramUtils.getLink(groupId, messageId.longValue()), text, Format.AUDIOBOOK, Source.AUDIBLE,
						RequestUtils.OTHER_TAGS_ENGLISH, userId, requestDate);
			}
		}

	}

}
