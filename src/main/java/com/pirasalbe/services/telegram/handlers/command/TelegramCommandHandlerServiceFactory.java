package com.pirasalbe.services.telegram.handlers.command;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Update;

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
	private TelegramSuperAdminCommandHandlerService superAdminCommandHandlerService;

	@Autowired
	private TelegramGroupsCommandHandlerService groupsCommandHandlerService;

	@PostConstruct
	public void initializeHandlers() {
		commandHandlers = new LinkedHashSet<>();
		commandHandlers.add(groupsCommandHandlerService);
	}

	public TelegramCommandHandler getTelegramCommandHandler(Update update) {
		TelegramCommandHandler result = null;

		boolean found = false;
		Iterator<TelegramCommandHandler> iterator = commandHandlers.iterator();
		while (iterator.hasNext() && !found) {
			TelegramCommandHandler handler = iterator.next();
			if (handler.shouldHandle(update)) {
				found = true;
				result = handler;
			}
		}

		return result;
	}

}
