package com.pirasalbe.service.telegram.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Update;
import com.pirasalbe.service.telegram.AdminService;

/**
 * Service to manage commands from the telegram bot
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramCommandHandlerService implements TelegramHandlerService {

	@Autowired
	private AdminService adminService;

	@Override
	public boolean shouldHandle(Update update) {
		return update.message() != null && update.message().text() != null && update.message().text().startsWith("/");
	}

	@Override
	public void handleUpdate(Update update) {
		// only admins can send commands
		if (adminService.isAdmin(update.message().from().id())) {
			// TODO create command handlers
		}
	}

}
