package com.pirasalbe.services.telegram.handlers.request;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.services.SchedulerService;
import com.pirasalbe.utils.DateUtils;
import com.pirasalbe.utils.TelegramUtils;

/**
 * Service to manage requests from users
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramBumpRequestHandlerService extends AbstractTelegramRequestHandlerService {

	protected static final List<String> BUMPS = Arrays.asList("bump", "update", "can i get", "can i have", "need",
			"please help", "send", "thank", "repost", "news", "anyone", "hope", "this one", "not received", "status",
			"any luck", "available", "do you", "please anybody", "check");

	@Autowired
	private SchedulerService schedulerService;

	@Override
	protected Message getMessage(Update update) {
		Message message = null;

		if (update.message() != null) {
			message = update.message();
		}

		return message;
	}

	@Override
	protected Message getRequestMessage(Update update) {
		Message message = null;

		Message updateMessage = getMessage(update);

		// reply to the same user request with a bump keyword
		if (updateMessage != null && updateMessage.replyToMessage() != null
				&& updateMessage.from().id().equals(updateMessage.replyToMessage().from().id())
				&& hasRequestTag(updateMessage.replyToMessage().text()) && isBump(updateMessage.text())) {
			message = updateMessage.replyToMessage();
		}

		return message;
	}

	protected boolean isBump(String text) {
		boolean result = false;

		if (text != null) {
			text = text.toLowerCase();

			for (int i = 0; i < BUMPS.size() && !result; i++) {
				String bump = BUMPS.get(i);

				// message has bump keyword
				result = text.contains(bump);
			}
		}

		return result;
	}

	@Override
	public void handle(TelegramBot bot, Update update) {
		Message requestMessage = getRequestMessage(update);
		Message message = getMessage(update);

		Long chatId = requestMessage.chat().id();

		LocalDateTime requestTime = DateUtils.integerToLocalDateTime(message.date());

		sendBumpNotification(bot, requestMessage, message, chatId);

		// manage only requests from active groups
		Optional<Group> optional = groupService.findById(chatId);
		if (optional.isPresent()) {
			newRequest(bot, requestMessage, chatId, message.messageId(), requestTime, optional.get());
		}
	}

	private void sendBumpNotification(TelegramBot bot, Message requestMessage, Message message, Long chatId) {
		StringBuilder bumpBuilder = new StringBuilder();
		bumpBuilder.append(TelegramUtils.tagUser(message));
		bumpBuilder.append("You asked for an update for <a href='");
		bumpBuilder.append(TelegramUtils.getLink(requestMessage));
		bumpBuilder.append("'>your request</a>.");

		SendMessage sendMessage = new SendMessage(chatId, bumpBuilder.toString());
		sendMessage.replyToMessageId(message.messageId());
		sendMessage.parseMode(ParseMode.HTML);

		SendResponse sendResponse = bot.execute(sendMessage);
		if (sendResponse.isOk()) {
			schedulerService.schedule(
					(b, r) -> b.execute(new DeleteMessage(r.message().chat().id(), r.message().messageId())),
					sendResponse, 10, TimeUnit.SECONDS);
		}
	}

}
