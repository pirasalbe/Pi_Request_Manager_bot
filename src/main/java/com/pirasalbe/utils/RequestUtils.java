package com.pirasalbe.utils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.pengrad.telegrambot.model.MessageEntity;
import com.pengrad.telegrambot.model.MessageEntity.Type;
import com.pirasalbe.models.request.Source;

/**
 * Utility methods to manage requests
 *
 * @author pirasalbe
 *
 */
public class RequestUtils {

	private RequestUtils() {
		super();
	}

	public static List<Source> getNoRepeatSources(String noRepeat) {
		List<Source> noRepeatForSources = new ArrayList<>();

		if (noRepeat != null) {
			String[] sources = noRepeat.toUpperCase().split(",");
			for (String sourceString : sources) {
				Source source = Source.valueOf(sourceString.trim());
				noRepeatForSources.add(source);
			}
		}

		return noRepeatForSources;
	}

	public static String getNoRepeatSources(List<Source> sources) {
		String noRepeat = null;

		if (!sources.isEmpty()) {
			StringBuilder builder = new StringBuilder();
			for (int i = 0; i < sources.size(); i++) {
				Source source = sources.get(i);
				builder.append(source);
				if (i < sources.size() - 1) {
					builder.append(",");
				}
			}
			noRepeat = builder.toString();
		}

		return noRepeat;
	}

	public static String getLink(String content, MessageEntity[] entities) {
		String link = null;

		if (entities != null) {
			for (MessageEntity entity : entities) {
				if (entity.type().equals(Type.text_link)) {
					link = entity.url();
				} else if (entity.type().equals(Type.url)) {
					link = content.substring(entity.offset(), entity.offset() + entity.length());
				}
			}
		}

		return link;
	}

	public static String getTimeBetweenDates(LocalDateTime from, LocalDateTime to) {
		StringBuilder stringBuilder = new StringBuilder();

		// get numbers
		long days = DateUtils.getDays(from, to);
		long hours = DateUtils.getHours(from, to, days);
		long minutes = DateUtils.getMinutes(from, to, days, hours);

		// aggregate them
		if (days > 0) {
			stringBuilder.append(days).append(" day").append(StringUtils.getPlural(days));
		}

		// get hours
		if (days > 0 && hours > 0) {
			stringBuilder.append(minutes > 0 ? ", " : " and ");
		}

		if (hours > 0) {
			stringBuilder.append(hours).append(" hour").append(StringUtils.getPlural(hours));
		}

		// get minutes
		if (hours > 0 && minutes > 0 || days > 0 && minutes > 0) {
			stringBuilder.append(" and ");
		}

		if (minutes > 0 || stringBuilder.length() == 0) {
			stringBuilder.append(minutes).append(" minute").append(StringUtils.getPlural(minutes));
		}

		return stringBuilder.toString();
	}

	public static String getComeBackAgain(LocalDateTime requestTime, LocalDateTime nextValidRequest) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Come back again in ");

		stringBuilder.append(getTimeBetweenDates(requestTime, nextValidRequest));

		stringBuilder.append(".");

		return stringBuilder.toString();
	}

}
