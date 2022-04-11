package com.pirasalbe.services.telegram.handlers.command;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.models.database.Request;
import com.pirasalbe.models.request.RequestStatus;
import com.pirasalbe.models.telegram.handlers.TelegramHandler;
import com.pirasalbe.services.RequestService;
import com.pirasalbe.services.telegram.handlers.AbstractTelegramHandlerService;
import com.pirasalbe.utils.TelegramUtils;

/**
 * Service to manage /me
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramMyRequestsCommandHandlerService extends AbstractTelegramHandlerService implements TelegramHandler {

	public static final String COMMAND = "/my_requests";

	@Autowired
	private RequestService requestService;

	@Override
	public void handle(TelegramBot bot, Update update) {
		Long chatId = TelegramUtils.getChatId(update);

		// check if the context is valid, either enabled group or PM
		Optional<Long> user = Optional.of(chatId);

		sendRequests(bot, chatId, user, RequestStatus.PENDING);
		sendRequests(bot, chatId, user, RequestStatus.RESOLVED);
	}

	private void sendRequests(TelegramBot bot, Long chatId, Optional<Long> user, RequestStatus requestStatus) {
		List<Request> requests = requestService.findRequests(Optional.empty(), requestStatus, user, Optional.empty(),
				Optional.empty(), Optional.empty(), true);

		StringBuilder titleBuilder = new StringBuilder();
		titleBuilder.append("<b>Requests ").append(requestStatus.getDescription()).append("</b>\n\n");
		String title = titleBuilder.toString();

		if (requests.isEmpty()) {
			SendMessage sendMessage = new SendMessage(chatId, title + "No requests found");
			sendMessage.parseMode(ParseMode.HTML);
			bot.execute(sendMessage);
		} else {
			sendRequestList(chatId, Optional.empty(), title, requests, false);
		}
	}

}
