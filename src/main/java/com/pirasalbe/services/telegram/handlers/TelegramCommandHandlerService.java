package com.pirasalbe.services.telegram.handlers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.models.UserRole;
import com.pirasalbe.models.telegram.TelegramHandlerResult;
import com.pirasalbe.services.AdminService;
import com.pirasalbe.services.telegram.handlers.command.TelegramCommandHandler;
import com.pirasalbe.services.telegram.handlers.command.TelegramCommandHandlerServiceFactory;
import com.pirasalbe.utils.TelegramUtils;

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

	@Autowired
	private TelegramCommandHandlerServiceFactory handlerServiceFactory;

	@Override
	public boolean shouldHandle(Update update) {
		String text = TelegramUtils.getText(update);
		return text != null && text.startsWith("/");
	}

	@Override
	public TelegramHandlerResult handleUpdate(Update update) {
		TelegramHandlerResult result = TelegramHandlerResult.noResponse();

		TelegramCommandHandler commandHandler = handlerServiceFactory.getTelegramCommandHandler(update);
		if (commandHandler != null) {
			result = manageMessage(commandHandler, update);
		}

		return result;
	}

	private TelegramHandlerResult manageMessage(TelegramCommandHandler commandHandler, Update update) {
		TelegramHandlerResult result = null;

		// execute command if authorized
		UserRole authority = adminService.getAuthority(TelegramUtils.getUserId(update));
		if (commandHandler.getRequiredRole().getAuthorityLevel() <= authority.getAuthorityLevel()) {
			result = commandHandler.handleCommand(update);
		} else {
			// reply with unauthorized error
			result = TelegramHandlerResult.withResponses(getUnauthorized(update));
		}

		return result;
	}

	private SendMessage getUnauthorized(Update update) {
		SendMessage sendMessage = new SendMessage(TelegramUtils.getChatId(update), "You are not authorized.");

		Integer messageId = TelegramUtils.getMessageId(update);
		if (messageId != null) {
			sendMessage.replyToMessageId(messageId);
		}

		return sendMessage;
	}

}
