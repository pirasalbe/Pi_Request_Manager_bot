package com.pirasalbe.models.telegram;

import com.pengrad.telegrambot.request.AbstractSendRequest;
import com.pengrad.telegrambot.request.SendMessage;

/**
 * Result of the handling
 *
 * @author pirasalbe
 *
 * @param <T> Response type
 */
public class TelegramHandlerResult<T extends AbstractSendRequest<?>> {

	private boolean shouldReply;

	private T response;

	private TelegramHandlerResult(boolean shouldReply, T response) {
		super();
		this.shouldReply = shouldReply;
		this.response = response;
	}

	/**
	 * Generate a result with no response
	 *
	 * @return TelegramHandlerResult without response
	 */
	public static TelegramHandlerResult<SendMessage> noReply() {
		return new TelegramHandlerResult<>(false, null);
	}

	/**
	 * Generate a reply
	 *
	 * @param <R>      Response type
	 * @param response Response to send
	 * @return TelegramHandlerResult with response
	 */
	public static <R extends AbstractSendRequest<R>> TelegramHandlerResult<R> reply(R response) {
		return new TelegramHandlerResult<>(true, response);
	}

	public boolean shouldReply() {
		return shouldReply;
	}

	public T getResponse() {
		return response;
	}

}
