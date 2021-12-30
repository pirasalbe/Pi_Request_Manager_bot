package com.pirasalbe.services.telegram.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.models.telegram.TelegramHandlerResult;

/**
 * Service to manage commands from the telegram bot
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramUnknownHandlerService implements TelegramHandlerService<SendMessage> {

	private static final Logger LOGGER = LoggerFactory.getLogger(TelegramUnknownHandlerService.class);

	@Override
	public boolean shouldHandle(Update update) {
		return true;
	}

	@Override
	public TelegramHandlerResult<SendMessage> handleUpdate(Update update) {
		LOGGER.debug("Unknown request: [{}]", update);

		return TelegramHandlerResult.noReply();
	}

}
