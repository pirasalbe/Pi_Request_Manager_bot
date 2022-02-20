package com.pirasalbe.services.telegram.handlers.command;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Audio;
import com.pengrad.telegrambot.model.Chat.Type;
import com.pengrad.telegrambot.model.Document;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.models.UserRole;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.database.Request;
import com.pirasalbe.models.request.Format;
import com.pirasalbe.models.request.Source;
import com.pirasalbe.models.telegram.handlers.TelegramCondition;
import com.pirasalbe.models.telegram.handlers.TelegramHandler;
import com.pirasalbe.services.GroupService;
import com.pirasalbe.services.RequestManagementService;
import com.pirasalbe.services.RequestService;
import com.pirasalbe.services.telegram.handlers.AbstractTelegramHandlerService;
import com.pirasalbe.utils.DateUtils;
import com.pirasalbe.utils.RequestUtils;
import com.pirasalbe.utils.TelegramUtils;

/**
 * Service to manage contributors related commands
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramContributorsCommandHandlerService extends AbstractTelegramHandlerService {

	private static final List<String> VALID_MIME_TYPES = Arrays.asList("application/zip", "application/vnd.rar",
			"document/x-m4b", "audio/x-m4b", "audio/mpeg", "application/epub+zip", "application/vnd.amazon.mobi8-ebook",
			"application/vnd.amazon.ebook", "application/x-mobipocket-ebook", "application/pdf", "image/vnd.djvu",
			"application/octet-stream");
	private static final List<String> VALID_EXTENSIONS = Arrays.asList(".zip", ".rar", ".mobi", ".pdf", ".epub",
			".azw3", ".azw", ".txt", ".doc", ".docx", ".rtf", ".cbz", ".cbr", ".djvu", ".chm", ".fb2", ".mp3", ".m4b",
			".opus");

	public static final String COMMAND_PENDING = "/pending";
	public static final String COMMAND_CANCEL = "/cancel";
	public static final String COMMAND_REMOVE = "/remove";
	public static final String COMMAND_DONE = "/done";
	public static final String COMMAND_SILENT_DONE = "/sdone";

	public static final String COMMAND_REQUESTS = "/requests";

	private static final String FORMAT_CONDITION = "format=";
	private static final String SOURCE_CONDITION = "source=";
	private static final String ORDER_CONDITION = "order=";
	private static final String ORDER_CONDITION_OLD = "OLD";
	private static final String ORDER_CONDITION_NEW = "NEW";

	public static final UserRole ROLE = UserRole.CONTRIBUTOR;

	@Autowired
	private GroupService groupService;

	@Autowired
	private RequestManagementService requestManagementService;

	@Autowired
	private RequestService requestService;

	public TelegramCondition replyToMessageCondition() {
		return this::replyToMessage;
	}

	private boolean replyToMessage(Update update) {
		return update.message() != null && update.message().replyToMessage() != null;
	}

	public TelegramHandler markPending() {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);

			Optional<Group> optional = groupService.findById(chatId);

			if (optional.isPresent()) {
				Message message = update.message().replyToMessage();

				boolean success = requestManagementService.markPending(message);

				// send a message to notify operation
				String link = TelegramUtils.getLink(message);
				StringBuilder stringBuilder = new StringBuilder();
				stringBuilder.append(requestStatusMessage(link, success, "marked as pending"));
				SendMessage sendMessage = new SendMessage(chatId, stringBuilder.toString());
				sendMessage.parseMode(ParseMode.HTML);

				sendMessageAndDelete(bot, sendMessage, 5, TimeUnit.SECONDS);
				deleteMessage(bot, update.message());
			}
		};
	}

	private String requestStatusMessage(String link, boolean success, String successMessage) {
		StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append("<a href='");
		stringBuilder.append(link);
		stringBuilder.append("'>Request</a>");
		if (success) {
			stringBuilder.append(" ").append(successMessage);
		} else {
			stringBuilder.append(" not found");
		}

		return stringBuilder.toString();
	}

	public TelegramHandler markDone() {
		return markDone(true);
	}

	public TelegramHandler markDoneSilently() {
		return markDone(false);
	}

	private TelegramHandler markDone(boolean reply) {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);

			Optional<Group> optional = groupService.findById(chatId);

			if (optional.isPresent()) {
				Message message = update.message().replyToMessage();

				boolean success = requestManagementService.markDone(message);

				// reply
				String link = TelegramUtils.getLink(message);
				if (reply) {
					markDoneWithMessage(bot, update, chatId, message, link);
				}

				// send a message to notify operation
				StringBuilder notificationBuilder = new StringBuilder();
				notificationBuilder.append(requestStatusMessage(link, success, "marked as done"));
				SendMessage sendMessageNotification = new SendMessage(chatId, notificationBuilder.toString());
				sendMessageNotification.parseMode(ParseMode.HTML);

				sendMessageAndDelete(bot, sendMessageNotification, 5, TimeUnit.SECONDS, true);
				deleteMessage(bot, update.message());
			}
		};
	}

	private void markDoneWithMessage(TelegramBot bot, Update update, Long chatId, Message message, String link) {
		StringBuilder replyBuilder = new StringBuilder();

		String text = TelegramUtils.removeCommand(update.message().text(), update.message().entities()).trim();

		replyBuilder.append(TelegramUtils.tagUser(message));
		replyBuilder.append(text).append("\n");

		replyBuilder.append("Request fulfilled by <code>").append(TelegramUtils.getUserName(update.message().from()))
				.append("</code>");

		SendMessage sendMessageReply = new SendMessage(chatId, replyBuilder.toString());
		sendMessageReply.parseMode(ParseMode.HTML);

		// reply to the request
		sendMessageReply.replyToMessageId(message.messageId());

		bot.execute(sendMessageReply);
	}

	public TelegramCondition replyToMessageWithFileCondition() {
		return update -> replyToMessage(update)
				&& (isValidDocument(update.message().document()) || isValidAudio(update.message().audio()));
	}

	private boolean isValidDocument(Document document) {
		return document != null && (document.mimeType().isEmpty() || VALID_MIME_TYPES.contains(document.mimeType())
				|| isValidExtension(document.fileName()));
	}

	private boolean isValidAudio(Audio audio) {
		return audio != null && (audio.mimeType().isEmpty() || VALID_MIME_TYPES.contains(audio.mimeType())
				|| isValidExtension(audio.fileName()));
	}

	private boolean isValidExtension(String filename) {
		boolean valid = false;

		String lowerFileName = filename.toLowerCase();
		for (int i = 0; i < VALID_EXTENSIONS.size() && !valid; i++) {
			String extension = VALID_EXTENSIONS.get(i);

			valid = lowerFileName.endsWith(extension);
		}

		return valid;
	}

	public TelegramHandler markDoneWithFile() {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);

			Optional<Group> optional = groupService.findById(chatId);

			if (optional.isPresent()) {
				Message message = update.message().replyToMessage();

				boolean success = requestManagementService.markDone(message);

				String link = TelegramUtils.getLink(message);
				StringBuilder stringBuilder = new StringBuilder();
				stringBuilder.append(requestStatusMessage(link, success, "marked as done"));
				SendMessage sendMessage = new SendMessage(chatId, stringBuilder.toString());
				sendMessage.parseMode(ParseMode.HTML);

				sendMessageAndDelete(bot, sendMessage, 5, TimeUnit.SECONDS);
			}
		};
	}

	public TelegramHandler markCancelled() {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);
			String text = update.message().text();

			Optional<Group> optional = groupService.findById(chatId);
			if (optional.isPresent()) {
				String message = null;

				// get message id
				Long messageId = null;
				if (update.message().replyToMessage() != null) {
					messageId = update.message().replyToMessage().messageId().longValue();
				} else {
					String messageText = TelegramUtils.removeCommand(text, update.message().entities()).trim();
					messageId = messageText.isEmpty() ? null : Long.parseLong(messageText);
				}

				// delete message
				if (messageId != null) {
					boolean success = requestManagementService.markCancelled(messageId, chatId);

					String link = TelegramUtils.getLink(chatId.toString(), messageId.toString());
					StringBuilder builder = new StringBuilder();
					builder.append(requestStatusMessage(link, success, "marked as cancelled"));
					message = builder.toString();
				} else {
					StringBuilder builder = new StringBuilder();

					builder.append("The right format is: <code>");
					builder.append(COMMAND_CANCEL);
					builder.append("</code> [message id]");
					message = builder.toString();
				}

				SendMessage sendMessage = new SendMessage(chatId, message);
				sendMessage.parseMode(ParseMode.HTML);

				sendMessageAndDelete(bot, sendMessage, 5, TimeUnit.SECONDS);
				deleteMessage(bot, update.message());
			}
		};
	}

	public TelegramHandler removeRequest() {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);
			String text = update.message().text();

			Optional<Group> optional = groupService.findById(chatId);
			if (optional.isPresent()) {
				String message = null;

				// get message id
				Long messageId = null;
				if (update.message().replyToMessage() != null) {
					messageId = update.message().replyToMessage().messageId().longValue();
				} else {
					String messageText = TelegramUtils.removeCommand(text, update.message().entities()).trim();
					messageId = messageText.isEmpty() ? null : Long.parseLong(messageText);
				}

				// delete message
				if (messageId != null) {
					boolean success = requestManagementService.deleteRequest(messageId, chatId);

					String link = TelegramUtils.getLink(chatId.toString(), messageId.toString());
					StringBuilder builder = new StringBuilder();
					builder.append(requestStatusMessage(link, success, "removed"));
					message = builder.toString();
				} else {
					StringBuilder builder = new StringBuilder();

					builder.append("The right format is: <code>");
					builder.append(COMMAND_REMOVE);
					builder.append("</code> [message id]");
					message = builder.toString();
				}

				SendMessage sendMessage = new SendMessage(chatId, message);
				sendMessage.parseMode(ParseMode.HTML);

				sendMessageAndDelete(bot, sendMessage, 5, TimeUnit.SECONDS);
				deleteMessage(bot, update.message());
			}
		};
	}

	public TelegramHandler getGroupRequests() {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);

			Optional<Long> group = null;
			boolean isPrivate = update.message().chat().type() == Type.Private;
			if (isPrivate) {
				// get requests in PM
				group = Optional.empty();
			} else {
				// get requests of the group
				group = Optional.of(chatId);
			}

			// check if the context is valid, either enabled group or PM
			if (groupService.existsById(chatId) || isPrivate) {
				deleteMessage(bot, update.message(), !isPrivate);
				String text = update.message().text();

				Optional<Format> format = getFormat(text);
				Optional<Source> source = getSource(text);
				Optional<Boolean> optionalDescendent = getDescendent(text);

				boolean descendent = optionalDescendent.isPresent() && optionalDescendent.get();
				List<Request> requests = requestService.findRequests(group, source, format, descendent);

				String title = getTitle(format, source, descendent);

				if (requests.isEmpty()) {
					SendMessage sendMessage = new SendMessage(chatId, title + "No requests found");
					sendMessage.parseMode(ParseMode.HTML);
					sendMessageAndDelete(bot, sendMessage, 30, TimeUnit.SECONDS, group.isPresent());
				} else {
					sendRequestList(bot, chatId, group, title, requests);
				}
			}
		};
	}

	private void sendRequestList(TelegramBot bot, Long chatId, Optional<Long> group, String title,
			List<Request> requests) {
		StringBuilder builder = new StringBuilder(title);

		// chat name, when in PM
		Map<Long, String> chatNames = new HashMap<>();
		LocalDateTime now = DateUtils.getNow();
		boolean deleteMessages = group.isPresent();

		// create text foreach request
		for (int i = 0; i < requests.size(); i++) {
			Request request = requests.get(i);

			// build request text
			StringBuilder requestBuilder = new StringBuilder();
			Long messageId = request.getId().getMessageId();
			Long groupId = request.getId().getGroupId();
			requestBuilder.append("<a href='").append(TelegramUtils.getLink(groupId.toString(), messageId.toString()))
					.append("'>");

			requestBuilder.append(getChatName(chatNames, groupId)).append(" ");
			requestBuilder.append(i + 1).append("</a> ");

			requestBuilder.append(RequestUtils.getTimeBetweenDates(request.getRequestDate(), now)).append(" ago ");
			requestBuilder.append("(<code>").append(COMMAND_CANCEL).append(" ").append(messageId).append("</code>)\n");

			String requestText = requestBuilder.toString();

			// if length is > message limit, send current text
			if (builder.length() + requestText.length() > 4096) {
				SendMessage sendMessage = new SendMessage(chatId, builder.toString());
				sendMessage.parseMode(ParseMode.HTML);
				sendMessageAndDelete(bot, sendMessage, 5, TimeUnit.MINUTES, deleteMessages);
				builder = new StringBuilder(title);
			}
			builder.append(requestText);
			// send last message
			if (i == requests.size() - 1) {
				SendMessage sendMessage = new SendMessage(chatId, builder.toString());
				sendMessage.parseMode(ParseMode.HTML);
				sendMessageAndDelete(bot, sendMessage, 5, TimeUnit.MINUTES, deleteMessages);
			}
		}
	}

	private String getChatName(Map<Long, String> chatNames, Long groupId) {
		String chatName = null;
		if (chatNames.containsKey(groupId)) {
			chatName = chatNames.get(groupId);
		} else {
			Optional<Group> optional = groupService.findById(groupId);
			if (optional.isPresent()) {
				chatName = optional.get().getName();
				chatNames.put(groupId, chatName);
			} else {
				chatName = "Unknown";
			}
		}

		return chatName;
	}

	private <T> Optional<T> getCondition(String text, String condition, Function<String, T> function) {
		T result = null;

		int indexOf = text.toLowerCase().indexOf(condition);
		int end = text.indexOf(' ', indexOf);

		if (end < indexOf) {
			end = text.length();
		}

		if (indexOf > -1) {
			String conditionString = text.toUpperCase().substring(indexOf + condition.length(), end);
			result = function.apply(conditionString);
		}

		return Optional.ofNullable(result);
	}

	private Optional<Format> getFormat(String text) {
		return getCondition(text, FORMAT_CONDITION, Format::valueOf);
	}

	private Optional<Source> getSource(String text) {
		return getCondition(text, SOURCE_CONDITION, Source::valueOf);
	}

	private Optional<Boolean> getDescendent(String text) {
		return getCondition(text, ORDER_CONDITION, s -> s.equals(ORDER_CONDITION_NEW));
	}

	private String getTitle(Optional<Format> format, Optional<Source> source, boolean descendent) {
		StringBuilder title = new StringBuilder();
		title.append("<b>Requests</b>");
		if (format.isPresent()) {
			title.append("\nFormat [").append(format.get()).append("]");
		}
		if (source.isPresent()) {
			title.append("\nSource [").append(source.get()).append("]");
		}
		title.append("\nShow ").append(descendent ? ORDER_CONDITION_NEW : ORDER_CONDITION_OLD).append(" first.")
				.append("\n\n");

		return title.toString();
	}

}
