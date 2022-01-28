package com.pirasalbe.models.telegram.handlers;

import java.util.Collection;

public class TelegramUpdateHandler {

	private Collection<TelegramCondition> conditions;

	private TelegramHandler handler;

	public TelegramUpdateHandler(Collection<TelegramCondition> conditions, TelegramHandler handler) {
		super();
		this.conditions = conditions;
		this.handler = handler;
	}

	public Collection<TelegramCondition> getConditions() {
		return conditions;
	}

	public TelegramHandler getHandler() {
		return handler;
	}

}
