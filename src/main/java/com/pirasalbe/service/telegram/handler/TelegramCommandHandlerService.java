package com.pirasalbe.service.telegram.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.model.UserRole;
import com.pirasalbe.model.telegram.TelegramHandlerResult;
import com.pirasalbe.service.telegram.AdminService;
import com.pirasalbe.service.telegram.handler.command.TelegramCommandHandler;
import com.pirasalbe.service.telegram.handler.command.TelegramCommandHandlerServiceFactory;

/**
 * Service to manage commands from the telegram bot
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramCommandHandlerService implements TelegramHandlerService<SendMessage> {

	@Autowired
	private AdminService adminService;

	@Autowired
	private TelegramCommandHandlerServiceFactory handlerServiceFactory;

	@Override
	public boolean shouldHandle(Update update) {
		return update.message() != null && update.message().text() != null && update.message().text().startsWith("/");
	}

	@Override
	public TelegramHandlerResult<SendMessage> handleUpdate(Update update) {
		TelegramHandlerResult<SendMessage> result = TelegramHandlerResult.noReply();

		Message message = update.message();

		TelegramCommandHandler commandHandler = handlerServiceFactory.getTelegramCommandHandler(message);
		if (commandHandler != null) {
			result = manageMessage(commandHandler, message);
		}

		return result;
	}

	private TelegramHandlerResult<SendMessage> manageMessage(TelegramCommandHandler commandHandler, Message message) {
		TelegramHandlerResult<SendMessage> result = null;

		// execute command if authorized
		UserRole authority = adminService.getAuthority(message.from().id());
		if (commandHandler.getRequiredRole().getAuthorityLevel() <= authority.getAuthorityLevel()) {
			result = commandHandler.handleCommand(message);
		} else {
			// reply with unauthorized error
			result = TelegramHandlerResult.reply(getUnauthorized(message));
		}

		return result;
	}

	private SendMessage getUnauthorized(Message message) {
		return new SendMessage(message.chat().id(), "You are not authorized.").replyToMessageId(message.messageId());
	}

}
