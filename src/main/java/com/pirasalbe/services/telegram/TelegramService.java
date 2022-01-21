package com.pirasalbe.services.telegram;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pirasalbe.configurations.TelegramConfiguration;
import com.pirasalbe.models.telegram.handlers.conditions.TelegramCommand;
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
	private TelegramConfiguration configuration;

	@Autowired
	private TelegramBotService bot;

	/*
	 * HANDLERS
	 */
	@Autowired
	private TelegramHelpCommandHandlerService helpCommandHandlerService;

	@PostConstruct
	public void initialize() {

		String username = configuration.getUsername();

		// TODO add handlers
		bot.register(new TelegramCommand(username, TelegramHelpCommandHandlerService.COMMAND),
				helpCommandHandlerService);

		bot.launch();

	}

}
