package com.pirasalbe.services.telegram.handlers;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import com.pirasalbe.services.SchedulerService;
import com.pirasalbe.utils.TelegramConditionUtils;

/**
 * Service with common methods
 *
 * @author pirasalbe
 *
 */
@Component
public class AbstractTelegramHandlerService {

	@Autowired
	protected SchedulerService schedulerService;

	/**
	 * Delete a message
	 *
	 * @param bot     Bot to delete the message
	 * @param message Message to delete
	 * @param delete  True to delete the message
	 */
	protected void deleteMessage(TelegramBot bot, Message message, boolean delete) {
		if (delete) {
			deleteMessage(bot, message);
		}
	}

	/**
	 * Delete a message
	 *
	 * @param bot     Bot to delete the message
	 * @param message Message to delete
	 */
	protected void deleteMessage(TelegramBot bot, Message message) {
		bot.execute(new DeleteMessage(message.chat().id(), message.messageId()));
	}

	/**
	 * Send a message and delete it after a timeout
	 *
	 * @param bot         Bot to send the message
	 * @param sendMessage Message to send
	 * @param timeout     Timeout before deleting the request
	 * @param timeUnit    Unit of the timeout
	 */
	protected void sendMessageAndDelete(TelegramBot bot, SendMessage sendMessage, long timeout, TimeUnit timeUnit) {
		sendMessageAndDelete(bot, sendMessage, timeout, timeUnit, true);
	}

	/**
	 * Send a message and optionally delete it after a timeout
	 *
	 * @param bot         Bot to send the message
	 * @param sendMessage Message to send
	 * @param timeout     Timeout before deleting the request
	 * @param timeUnit    Unit of the timeout
	 * @param delete      True to delete the message
	 */
	protected void sendMessageAndDelete(TelegramBot bot, SendMessage sendMessage, long timeout, TimeUnit timeUnit,
			boolean delete) {

		SendResponse response = bot.execute(sendMessage);

		// schedule delete
		if (delete) {
			schedulerService.schedule(
					(b, r) -> b.execute(new DeleteMessage(r.message().chat().id(), r.message().messageId())), response,
					timeout, timeUnit);
		}
	}

	protected Optional<Long> getGroup(Long chatId, String text, boolean isPrivate) {
		Optional<Long> group;

		if (isPrivate) {
			// get requests in PM
			group = TelegramConditionUtils.getGroupId(text);
		} else {
			// get requests of the group
			group = Optional.of(chatId);
		}

		return group;
	}

}
