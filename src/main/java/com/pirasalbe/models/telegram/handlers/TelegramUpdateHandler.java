package com.pirasalbe.models.telegram.handlers;

public class TelegramUpdateHandler {

	private TelegramCondition condition;

	private TelegramHandler handler;

	public TelegramUpdateHandler(TelegramCondition condition, TelegramHandler handler) {
		super();
		this.condition = condition;
		this.handler = handler;
	}

	public TelegramCondition getCondition() {
		return condition;
	}

	public TelegramHandler getHandler() {
		return handler;
	}

}
