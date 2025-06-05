package com.pirasalbe.services.telegram.conditions;

import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Update;
import com.pirasalbe.models.telegram.handlers.TelegramCondition;
import com.pirasalbe.utils.TelegramUtils;

@Component
public class TelegramReplyToMessageCondition implements TelegramCondition {

	@Override
	public boolean check(Update update) {
		return update.message() != null && update.message().replyToMessage() != null
				&& !TelegramUtils.isThreadMessage(update.message().replyToMessage());
	}

}
