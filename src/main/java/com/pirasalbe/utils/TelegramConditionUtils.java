package com.pirasalbe.utils;

import java.util.Optional;
import java.util.function.Function;

import com.pirasalbe.models.ContributorAction;
import com.pirasalbe.models.request.Format;
import com.pirasalbe.models.request.RequestStatus;
import com.pirasalbe.models.request.Source;

/**
 * Utility methods for Telegram conditions
 *
 * @author pirasalbe
 *
 */
public class TelegramConditionUtils {

	public static final String MESSAGE_CONDITION = "message=";
	public static final String GROUP_CONDITION = "group=";
	public static final String ACTION_CONDITION = "action=";
	public static final String STATUS_CONDITION = "status=";
	public static final String FORMAT_CONDITION = "format=";
	public static final String SOURCE_CONDITION = "source=";
	public static final String ORDER_CONDITION = "order=";
	public static final String REFRESH_SHOW_CONDITION = "refresh_show=";

	public static final String ORDER_CONDITION_OLD = "OLD";
	public static final String ORDER_CONDITION_NEW = "NEW";

	private TelegramConditionUtils() {
		super();
	}

	private static <T> Optional<T> getCondition(String text, String condition, Function<String, T> function) {
		return getCondition(text, condition, function, false);
	}

	private static <T> Optional<T> getCondition(String text, String condition, Function<String, T> function,
			boolean allowEmpty) {
		T result = null;

		int indexOf = text.toLowerCase().indexOf(condition);
		int begin = indexOf + condition.length();
		int end = text.indexOf(' ', indexOf);

		if (end < indexOf) {
			end = text.length();
		}

		if (indexOf > -1 && (allowEmpty || end > begin)) {
			String conditionString = text.toUpperCase().substring(begin, end);
			result = function.apply(conditionString);
		}

		return Optional.ofNullable(result);
	}

	public static Optional<Long> getMessageId(String text) {
		return getCondition(text, MESSAGE_CONDITION, Long::parseLong);
	}

	public static Optional<Long> getGroupId(String text) {
		return getCondition(text, GROUP_CONDITION, Long::parseLong);
	}

	public static Optional<ContributorAction> getAction(String text) {
		return getCondition(text, ACTION_CONDITION, ContributorAction::valueOf);
	}

	public static Optional<Integer> getRefreshShow(String text) {
		return getCondition(text, REFRESH_SHOW_CONDITION, Integer::parseInt);
	}

	public static Optional<RequestStatus> getStatus(String text) {
		return getCondition(text, STATUS_CONDITION, RequestStatus::valueOf);
	}

	public static Optional<Format> getFormat(String text) {
		return getCondition(text, FORMAT_CONDITION, Format::valueOf);
	}

	public static Optional<Source> getSource(String text) {
		return getCondition(text, SOURCE_CONDITION, Source::valueOf);
	}

	public static Optional<Boolean> getDescendent(String text) {
		return getCondition(text, ORDER_CONDITION, s -> s.equals(ORDER_CONDITION_NEW), true);
	}

}
