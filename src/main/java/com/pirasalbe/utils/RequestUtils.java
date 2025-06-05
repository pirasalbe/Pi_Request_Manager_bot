package com.pirasalbe.utils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.MessageEntity;
import com.pengrad.telegrambot.model.MessageEntity.Type;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.GetChatMember;
import com.pengrad.telegrambot.response.GetChatMemberResponse;
import com.pirasalbe.models.Cache;
import com.pirasalbe.models.ContributorAction;
import com.pirasalbe.models.database.Request;
import com.pirasalbe.models.request.RequestStatus;
import com.pirasalbe.models.request.Source;

/**
 * Utility methods to manage requests
 *
 * @author pirasalbe
 *
 */
public class RequestUtils {

	public static final String OTHER_TAGS_ENGLISH = "english";

	private static Cache<Long, String> userNames = new Cache<>(604800l);

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

		if (link != null) {
			// remove params
			link = link.split("\\?")[0];
		}

		return link;
	}

	/**
	 * Extract content from message
	 *
	 * @param text     Text content
	 * @param entities Entities
	 * @return
	 */
	public static String getContent(String text, MessageEntity[] entities) {
		StringBuilder contentBuilder = new StringBuilder(text);

		if (entities != null && entities.length > 0) {
			int offset = 0;

			// add all parts
			for (MessageEntity entity : entities) {
				// remove the entity part
				int begin = offset + entity.offset();
				int end = begin + entity.length();
				contentBuilder.delete(begin, end);

				// add formatted text
				String value = getEntityValue(text, entity);
				contentBuilder.insert(begin, value);

				// update offset
				offset += value.length() - (end - begin);
			}
		}

		return contentBuilder.toString();
	}

	private static String getEntityValue(String text, MessageEntity entity) {
		StringBuilder value = new StringBuilder();

		switch (entity.type()) {
		case bold:
			value.append("<b>");
			value.append(text.substring(entity.offset(), entity.offset() + entity.length()));
			value.append("</b>");
			break;
		case code:
			value.append("<code>");
			value.append(text.substring(entity.offset(), entity.offset() + entity.length()));
			value.append("</code>");
			break;
		case italic:
			value.append("<i>");
			value.append(text.substring(entity.offset(), entity.offset() + entity.length()));
			value.append("</i>");
			break;
		case pre:
			value.append("<pre>");
			value.append(text.substring(entity.offset(), entity.offset() + entity.length()));
			value.append("</pre>");
			break;
		case spoiler:
			value.append("<tg-spoiler>");
			value.append(text.substring(entity.offset(), entity.offset() + entity.length()));
			value.append("</tg-spoiler>");
			break;
		case strikethrough:
			value.append("<s>");
			value.append(text.substring(entity.offset(), entity.offset() + entity.length()));
			value.append("</s>");
			break;
		case text_link:
			value.append("<a href='").append(entity.url()).append("'>");
			value.append(text.substring(entity.offset(), entity.offset() + entity.length()));
			value.append("</a>");
			break;
		case underline:
			value.append("<u>");
			value.append(text.substring(entity.offset(), entity.offset() + entity.length()));
			value.append("</u>");
			break;
		case bot_command:
		case cashtag:
		case hashtag:
		case url:
		case email:
		case mention:
		case phone_number:
		case text_mention:
		default:
			value.append(text.substring(entity.offset(), entity.offset() + entity.length()));
			break;
		}

		return value.toString();
	}

	public static String getTimeBetweenDates(LocalDateTime from, LocalDateTime to) {
		return getTimeBetweenDates(from, to, false);
	}

	public static String getTimeBetweenDates(LocalDateTime from, LocalDateTime to, boolean shortNames) {
		StringBuilder stringBuilder = new StringBuilder();

		// get numbers
		long days = DateUtils.getDays(from, to);
		long hours = DateUtils.getHours(from, to, days);
		long minutes = DateUtils.getMinutes(from, to, days, hours);

		// aggregate them
		appendTime(stringBuilder, days, false, shortNames, "d", "day");

		// get hours
		if (days > 0 && hours > 0) {
			stringBuilder.append(minutes > 0 ? ", " : " and ");
		}

		appendTime(stringBuilder, hours, false, shortNames, "h", "hour");

		// get minutes
		if (hours > 0 && minutes > 0 || days > 0 && minutes > 0) {
			stringBuilder.append(" and ");
		}

		appendTime(stringBuilder, minutes, stringBuilder.length() == 0, shortNames, "m", "minute");

		return stringBuilder.toString();
	}

	private static void appendTime(StringBuilder stringBuilder, long value, boolean force, boolean shortNames,
			String shortName, String longName) {
		if (value > 0 || force) {
			stringBuilder.append(value).append(" ");
			if (shortNames) {
				stringBuilder.append(shortName);
			} else {
				stringBuilder.append(longName).append(StringUtils.getPlural(value));
			}
		}
	}

	public static String getComeBackAgain(LocalDateTime requestTime, LocalDateTime nextValidRequest) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Come back again in ");

		stringBuilder.append(getTimeBetweenDates(requestTime, nextValidRequest));

		stringBuilder.append(".");

		return stringBuilder.toString();
	}

	public static String getActionsLink(String username, Long messageId, Long groupId) {
		StringBuilder paramBuilder = new StringBuilder();
		paramBuilder.append("show_message=");
		paramBuilder.append(messageId);
		paramBuilder.append("_").append(TelegramConditionUtils.GROUP_CONDITION);
		paramBuilder.append(groupId);
		return TelegramUtils.getStartLink(username, paramBuilder.toString());
	}

	public static String getRequestInfo(TelegramBot bot, String groupName, Request request) {
		StringBuilder messageBuilder = new StringBuilder();

		messageBuilder.append(request.getContent());

		messageBuilder.append("\n\n[");

		// 👤 Hayut (5258002384) | 👥 #Audoroom | ⏳PENDING | 🕔 7h 4m ago

		// user info
		messageBuilder.append("👤 ").append(getUser(bot, request.getId().getGroupId(), request.getUserId()))
				.append("(<code>").append(request.getUserId()).append("</code>)");

		// group info
		messageBuilder.append(" | ");
		String groupNameSanitized = groupName.replace(' ', '_').replace(".", "").replace(":", "");
		messageBuilder.append("👥 #").append(groupNameSanitized);

		// time info
		messageBuilder.append(" | ");
		messageBuilder.append("🕔 ").append(getTimeBetweenDates(request.getRequestDate(), DateUtils.getNow(), true));

		// repetitions
		messageBuilder.append(" | ");
		messageBuilder.append("🔢 ").append(request.getRepetitions());

		// status info
		messageBuilder.append(" | ");
		messageBuilder.append(request.getStatus().getIcon()).append(" ")
				.append(request.getStatus().getDescription().toUpperCase());

		// resolved info
		if (request.getStatus() == RequestStatus.RESOLVED) {
			messageBuilder.append(" | ");
			messageBuilder.append(RequestStatus.RESOLVED.getIcon()).append(" ")
					.append(getTimeBetweenDates(request.getResolvedDate(), DateUtils.getNow(), true));

			if (request.getResolvedMessageId() != null) {
				messageBuilder.append(" | ");
				messageBuilder.append("<a href='")
						.append(TelegramUtils.getLink(request.getId().getGroupId(), request.getResolvedMessageId()))
						.append("'>Fulfilled here").append("</a>");

			}
		}

		// contributor
		if (request.getContributor() != null) {
			messageBuilder.append(" | ");
			messageBuilder.append("🙋 ").append(getUser(bot, request.getId().getGroupId(), request.getContributor()));
		}

		messageBuilder.append("]");

		return messageBuilder.toString();
	}

	public static String getUser(TelegramBot bot, Long groupId, Long userId) {
		String username = null;

		if (userNames.containsKey(userId)) {
			username = userNames.get(userId);
		} else {
			GetChatMember getChatMember = new GetChatMember(groupId, userId);
			GetChatMemberResponse member = bot.execute(getChatMember);

			if (member.isOk()) {
				username = TelegramUtils.getUserName(member.chatMember().user());
				userNames.put(userId, username);
			} else {
				username = userId.toString();
			}
		}

		return TelegramUtils.tagUser(userId, username).replace(".", "");
	}

	public static InlineKeyboardMarkup getRequestKeyboard(String username, Long groupId, Long messageId,
			RequestStatus status, String refreshButtonText) {
		InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();

		InlineKeyboardButton requestButton = new InlineKeyboardButton("📚 Request")
				.url(TelegramUtils.getLink(groupId, messageId));
		InlineKeyboardButton refreshButton = new InlineKeyboardButton(refreshButtonText)
				.url(RequestUtils.getActionsLink(username, messageId, groupId));

		inlineKeyboard.addRow(requestButton, refreshButton);

		InlineKeyboardButton pendingButton = new InlineKeyboardButton(RequestStatus.PENDING.getIcon() + " Pending")
				.callbackData(RequestUtils.getActionCallback(messageId, groupId, ContributorAction.PENDING));

		InlineKeyboardButton doneButton = new InlineKeyboardButton(RequestStatus.RESOLVED.getIcon() + " Done")
				.callbackData(RequestUtils.getActionCallback(messageId, groupId, ContributorAction.DONE));
		InlineKeyboardButton inProgressButton = new InlineKeyboardButton(
				RequestStatus.IN_PROGRESS.getIcon() + " In Progress").callbackData(
						RequestUtils.getActionCallback(messageId, groupId, ContributorAction.IN_PROGRESS));
		InlineKeyboardButton pauseButton = new InlineKeyboardButton(RequestStatus.PAUSED.getIcon() + " Pause")
				.callbackData(RequestUtils.getActionCallback(messageId, groupId, ContributorAction.PAUSE));

		inlineKeyboard.addRow(getButton(status, RequestStatus.PAUSED, pauseButton, pendingButton),
				getButton(status, RequestStatus.IN_PROGRESS, inProgressButton, pendingButton),
				getButton(status, RequestStatus.RESOLVED, doneButton, pendingButton));

		InlineKeyboardButton cancelButton = new InlineKeyboardButton(RequestStatus.CANCELLED.getIcon() + " Cancel")
				.callbackData(RequestUtils.getActionCallback(messageId, groupId, ContributorAction.CANCEL));
		InlineKeyboardButton removeButton = new InlineKeyboardButton("🗑 Remove")
				.callbackData(RequestUtils.getConfirmActionCallback(messageId, groupId, ContributorAction.REMOVE));

		inlineKeyboard.addRow(getButton(status, RequestStatus.CANCELLED, cancelButton, pendingButton), removeButton);

		return inlineKeyboard;
	}

	/**
	 * Get the right button. Pending if status == otherButtonStatus, otherwise
	 * otherButton
	 *
	 * @param status            Status of the request
	 * @param otherButtonStatus Status of the button
	 * @param otherButton       Button
	 * @param pendingButton     Pending button
	 * @return Button
	 */
	private static InlineKeyboardButton getButton(RequestStatus status, RequestStatus otherButtonStatus,
			InlineKeyboardButton otherButton, InlineKeyboardButton pendingButton) {
		return status == otherButtonStatus ? pendingButton : otherButton;
	}

	private static String getConfirmActionCallback(Long messageId, Long groupId, ContributorAction action) {
		StringBuilder callbackBuilder = new StringBuilder();

		callbackBuilder.append(ContributorAction.CONFIRM.getCode());
		callbackBuilder.append(" ");
		callbackBuilder.append(getCallbackRequestData(messageId, groupId));
		callbackBuilder.append(" ");
		callbackBuilder.append(TelegramConditionUtils.ACTION_CONDITION).append(action.getCode());

		return callbackBuilder.toString();
	}

	public static String getActionCallback(Long messageId, Long groupId, ContributorAction action) {
		return getActionCallback(messageId, groupId, action, false);
	}

	public static String getActionCallback(Long messageId, Long groupId, ContributorAction action,
			boolean forceDelete) {
		return getActionCallback(messageId, groupId, action, Optional.empty(), Optional.empty(), forceDelete);
	}

	public static String getActionCallback(Long messageId, Long groupId, ContributorAction action,
			Optional<Long> refreshMessage, Optional<Long> refreshChat) {
		return getActionCallback(messageId, groupId, action, refreshMessage, refreshChat, false);
	}

	public static String getActionCallback(Long messageId, Long groupId, ContributorAction action,
			Optional<Long> refreshMessage, Optional<Long> refreshChat, boolean forceDelete) {
		StringBuilder callbackBuilder = new StringBuilder();

		callbackBuilder.append(action.getCode());
		callbackBuilder.append(" ");
		callbackBuilder.append(getCallbackRequestData(messageId, groupId));

		if (refreshMessage.isPresent() && refreshChat.isPresent()) {
			callbackBuilder.append(" ");
			callbackBuilder.append(TelegramConditionUtils.REFRESH_SHOW_MESSAGE_CONDITION).append(refreshMessage.get());
			callbackBuilder.append(" ");
			callbackBuilder.append(TelegramConditionUtils.REFRESH_SHOW_CHAT_CONDITION).append(refreshChat.get());
		}

		if (forceDelete) {
			callbackBuilder.append(" ");
			callbackBuilder.append(ContributorAction.FORCE_DELETE);
		}

		return callbackBuilder.toString();
	}

	private static String getCallbackRequestData(Long messageId, Long groupId) {
		StringBuilder callbackReequestDataBuilder = new StringBuilder();

		callbackReequestDataBuilder.append(TelegramConditionUtils.MESSAGE_CONDITION).append(messageId);
		callbackReequestDataBuilder.append(" ");
		callbackReequestDataBuilder.append(TelegramConditionUtils.GROUP_CONDITION).append(groupId);

		return callbackReequestDataBuilder.toString();
	}

}
