package com.pirasalbe.utils;

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

		for (MessageEntity entity : entities) {
			if (entity.type().equals(Type.text_link)) {
				link = entity.url();
			} else if (entity.type().equals(Type.url)) {
				link = content.substring(entity.offset(), entity.offset() + entity.length());
			}
		}

		return link;
	}

}
