package com.pirasalbe.services.telegram;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pirasalbe.services.telegram.handlers.TelegramCommandConditionFactory;
import com.pirasalbe.services.telegram.handlers.command.TelegramAliveCommandHandlerService;
import com.pirasalbe.services.telegram.handlers.command.TelegramHelpCommandHandlerService;

/**
 * Service to manage the telegram logic
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramService {

	@Autowired
	private TelegramBotService bot;

	/*
	 * HANDLERS
	 */

	@Autowired
	private TelegramCommandConditionFactory commandConditionFactory;

	@Autowired
	private TelegramHelpCommandHandlerService helpCommandHandlerService;

	@Autowired
	private TelegramAliveCommandHandlerService aliveCommandHandlerService;

	@PostConstruct
	public void initialize() {

		// help
		bot.register(commandConditionFactory.onCommand(TelegramHelpCommandHandlerService.COMMAND),
				helpCommandHandlerService);

		// alive
		bot.register(commandConditionFactory.onCommands(TelegramAliveCommandHandlerService.COMMANDS),
				aliveCommandHandlerService);

		// TODO add handlers

		bot.launch();

	}

}
