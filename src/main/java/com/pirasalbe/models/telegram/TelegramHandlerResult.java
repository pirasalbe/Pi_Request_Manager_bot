package com.pirasalbe.models.telegram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.pengrad.telegrambot.request.BaseRequest;

/**
 * Result of the handling
 *
 * @author pirasalbe
 *
 */
public class TelegramHandlerResult {

	private List<BaseRequest<?, ?>> responses;

	private TelegramHandlerResult(List<BaseRequest<?, ?>> responses) {
		super();
		this.responses = responses;
	}

	/**
	 * Generate a result with no response
	 *
	 * @return TelegramHandlerResult without response
	 */
	public static TelegramHandlerResult noResponse() {
		return new TelegramHandlerResult(new ArrayList<>());
	}

	/**
	 * Generate a reply
	 *
	 * @param <R>      Response type
	 * @param response Response to send
	 * @return TelegramHandlerResult with response
	 */
	public static TelegramHandlerResult withResponses(BaseRequest<?, ?>... response) {
		return new TelegramHandlerResult(Arrays.asList(response));
	}

	public List<BaseRequest<?, ?>> getResponses() {
		return responses;
	}

}
