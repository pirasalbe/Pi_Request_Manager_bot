package com.pirasalbe.service.telegram.handler.command;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Message;

/**
 * Service that gives the right handler of a command message
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramCommandHandlerServiceFactory {

	private Set<TelegramCommandHandler> commandHandlers;

	@Autowired
	private TelegramAliveCommandHandlerService aliveCommandHandlerService;

	@Autowired
	private TelegramHelpCommandHandlerService helpCommandHandlerService;

	@Autowired
	private TelegramSuperAdminCommandHandlerService superAdminCommandHandlerService;

	@PostConstruct
	public void initializeHandlers() {
		commandHandlers = new LinkedHashSet<>();
		commandHandlers.add(aliveCommandHandlerService);
		commandHandlers.add(helpCommandHandlerService);
		commandHandlers.add(superAdminCommandHandlerService);
	}

	public TelegramCommandHandler getTelegramCommandHandler(Message message) {
		TelegramCommandHandler result = null;

		boolean found = false;
		Iterator<TelegramCommandHandler> iterator = commandHandlers.iterator();
		while (iterator.hasNext() && !found) {
			TelegramCommandHandler handler = iterator.next();
			if (handler.shouldHandle(message)) {
				found = true;
				result = handler;
			}
		}

		return result;
	}

}
