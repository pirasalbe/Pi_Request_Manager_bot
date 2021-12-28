package com.pirasalbe.service.telegram.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Update;

/**
 * Service to manage commands from the telegram bot
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramUnknownHandlerService implements TelegramHandlerService {

	private static final Logger LOGGER = LoggerFactory.getLogger(TelegramUnknownHandlerService.class);

	@Override
	public boolean shouldHandle(Update update) {
		return true;
	}

	@Override
	public void handleUpdate(Update update) {
		LOGGER.debug("Unknown request: [{}]", update);
	}

}
