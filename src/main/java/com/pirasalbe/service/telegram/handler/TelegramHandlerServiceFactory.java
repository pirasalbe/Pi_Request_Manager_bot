package com.pirasalbe.service.telegram.handler;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Update;

/**
 * Service that gives the right handler of a message
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramHandlerServiceFactory {

	private Set<TelegramHandlerService<?>> handlerServices;

	@Autowired
	private TelegramUnknownHandlerService unknownHandlerService;

	@Autowired
	private TelegramCommandHandlerService commandHandlerService;

	@PostConstruct
	public void initializeHandlers() {
		handlerServices = new LinkedHashSet<>();
		handlerServices.add(commandHandlerService);
	}

	/**
	 * Get the handler for the update
	 *
	 * @param update Update to check
	 * @return TelegramHandlerService
	 */
	public TelegramHandlerService<?> getTelegramHandlerService(Update update) {
		TelegramHandlerService<?> result = unknownHandlerService;

		boolean found = false;
		Iterator<TelegramHandlerService<?>> iterator = handlerServices.iterator();
		while (iterator.hasNext() && !found) {
			TelegramHandlerService<?> handlerService = iterator.next();
			if (handlerService.shouldHandle(update)) {
				found = true;
				result = handlerService;
			}
		}

		return result;
	}

}
