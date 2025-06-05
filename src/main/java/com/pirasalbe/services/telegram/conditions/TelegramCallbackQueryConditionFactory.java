package com.pirasalbe.services.telegram.conditions;

import java.util.function.BiPredicate;

import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.CallbackQuery;
import com.pirasalbe.models.telegram.handlers.TelegramCondition;

/**
 * Command conditions factory
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramCallbackQueryConditionFactory {

	public enum Condition {
		EQUALS((callbackQuery, data) -> callbackQuery.equals(data)),

		STARTS_WITH((callbackQuery, data) -> data.startsWith(callbackQuery)),

		MATCHES((callbackQuery, data) -> data.matches(callbackQuery));

		private BiPredicate<String, String> predicate;

		private Condition(BiPredicate<String, String> function) {
			this.predicate = function;
		}

		boolean assertCondition(String callbackQuery, String data) {
			return this.predicate.test(callbackQuery, data);
		}
	}

	/**
	 * Generates a condition on the specified callbackQuery data
	 *
	 * @param callbackQuery Callback query expected
	 * @param condition     Condition to check
	 * @return TelegramCondition
	 */
	public TelegramCondition onCallbackQuery(String callbackQuery, Condition condition) {
		return update -> {
			boolean asserted = false;

			// commands only handles messages
			CallbackQuery message = update.callbackQuery();
			if (message != null && message.data() != null) {
				String data = message.data();
				asserted = condition.assertCondition(callbackQuery, data);
			}

			return asserted;
		};
	}

}
