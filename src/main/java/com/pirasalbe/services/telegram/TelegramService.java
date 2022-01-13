package com.pirasalbe.services.telegram;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

	@PostConstruct
	public void initialize() {

		// TODO add handlers
		// bot.register

		bot.launch();

	}

}
