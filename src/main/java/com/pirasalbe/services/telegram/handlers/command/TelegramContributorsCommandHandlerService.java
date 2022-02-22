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
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.GetChatMember;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetChatMemberResponse;
import com.pirasalbe.configurations.TelegramConfiguration;
import com.pirasalbe.models.ContributorAction;
import com.pirasalbe.models.UserRole;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.database.Request;
import com.pirasalbe.models.request.Format;
import com.pirasalbe.models.request.RequestStatus;
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

	private static final String START_PAYLOAD_SHOW = "^\\/start show_message=[0-9]+_group=[+-]?[0-9]+$";
	private static final String MESSAGE_INFO_CALLBACK = "message=[0-9]+ group=[+-]?[0-9]+";
	private static final String CONFIRM_CALLBACK = "^" + ContributorAction.CONFIRM + " " + MESSAGE_INFO_CALLBACK
			+ " action=[a-zA-Z]+$";
	private static final String CHANGE_STATUS_CALLBACK = "^(" + ContributorAction.PENDING + "|" + ContributorAction.DONE
			+ "|" + ContributorAction.CANCEL + "|" + ContributorAction.REMOVE + ") " + MESSAGE_INFO_CALLBACK + "$";

	public static final String COMMAND_SHOW = "/show";
	public static final String COMMAND_PENDING = "/pending";
	public static final String COMMAND_CANCEL = "/cancel";
	public static final String COMMAND_REMOVE = "/remove";
	public static final String COMMAND_DONE = "/done";
	public static final String COMMAND_SILENT_DONE = "/sdone";

	public static final String COMMAND_REQUESTS = "/requests";

	private static final String MESSAGE_CONDITION = "message=";
	private static final String GROUP_CONDITION = "group=";
	private static final String ACTION_CONDITION = "action=";
	private static final String FORMAT_CONDITION = "format=";
	private static final String SOURCE_CONDITION = "source=";
	private static final String ORDER_CONDITION = "order=";
	private static final String ORDER_CONDITION_OLD = "OLD";
	private static final String ORDER_CONDITION_NEW = "NEW";

	private static final String REQUEST_NOT_FOUND = "Request not found";

	public static final UserRole ROLE = UserRole.CONTRIBUTOR;

	@Autowired
	private TelegramConfiguration configuration;

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

	private String getErrorMessage(String command) {
		StringBuilder builder = new StringBuilder();

		builder.append("The right format is: <code>");
		builder.append(command);
		builder.append("</code> [message id]");

		return builder.toString();
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

	public TelegramCondition changeStatusCallbackCondition() {
		return update -> update.callbackQuery() != null
				&& update.callbackQuery().data().matches(CHANGE_STATUS_CALLBACK);
	}

	public TelegramHandler changeStatusWithCallback() {
		return (bot, update) -> {
			String text = update.callbackQuery().data();

			Optional<Long> optionalGroupId = getGroupId(text);
			Optional<Long> optionalMessageId = getMessageId(text);

			String actionString = text.substring(0, text.indexOf(' '));
			ContributorAction action = ContributorAction.valueOf(actionString);

			String result = null;
			if (optionalGroupId.isPresent() && optionalMessageId.isPresent()) {
				Long messageId = optionalMessageId.get();
				Long groupId = optionalGroupId.get();

				result = performAction(action, messageId, groupId);

			} else {
				result = "Request id not found";
			}

			AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery(update.callbackQuery().id());
			answerCallbackQuery.text(result);

			bot.execute(answerCallbackQuery);
		};
	}

	private String performAction(ContributorAction action, Long messageId, Long groupId) {
		String result;
		if (action == ContributorAction.REMOVE) {

			boolean deleteRequest = requestManagementService.deleteRequest(messageId, groupId);
			result = deleteRequest ? "Request removed" : REQUEST_NOT_FOUND;

		} else if (action == ContributorAction.CANCEL || action == ContributorAction.DONE
				|| action == ContributorAction.PENDING) {

			result = changeRequestStatus(action, messageId, groupId);

		} else {
			result = "Nothing to do";
		}
		return result;
	}

	private String changeRequestStatus(ContributorAction action, Long messageId, Long groupId) {
		String result;
		RequestStatus newStatus = null;

		switch (action) {
		case CANCEL:
			newStatus = RequestStatus.CANCELLED;
			break;
		case DONE:
			newStatus = RequestStatus.RESOLVED;
			break;
		case PENDING:
		default:
			newStatus = RequestStatus.NEW;
			break;
		}

		Optional<Request> optional = requestService.findById(messageId, groupId);
		if (optional.isPresent()) {
			requestService.updateStatus(optional.get(), newStatus);
			result = "Request marked as " + newStatus.getDescription();
		} else {
			result = REQUEST_NOT_FOUND;
		}
		return result;
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
					markDoneWithMessage(bot, update, chatId, message);
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

	private void markDoneWithMessage(TelegramBot bot, Update update, Long chatId, Message message) {
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
					message = getErrorMessage(COMMAND_CANCEL);
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
					message = getErrorMessage(COMMAND_REMOVE);
				}

				SendMessage sendMessage = new SendMessage(chatId, message);
				sendMessage.parseMode(ParseMode.HTML);

				sendMessageAndDelete(bot, sendMessage, 5, TimeUnit.SECONDS);
				deleteMessage(bot, update.message());
			}
		};
	}

	public TelegramHandler getRequests() {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);

			String text = update.message().text();
			boolean isPrivate = update.message().chat().type() == Type.Private;
			Optional<Long> group = getGroup(chatId, text, isPrivate);

			// check if the context is valid, either enabled group or PM
			if (groupService.existsById(chatId) || isPrivate) {
				deleteMessage(bot, update.message(), !isPrivate);

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

	private Optional<Long> getGroup(Long chatId, String text, boolean isPrivate) {
		Optional<Long> group;

		if (isPrivate) {
			// get requests in PM
			group = getGroupId(text);
		} else {
			// get requests of the group
			group = Optional.of(chatId);
		}

		return group;
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

			requestBuilder.append("[<a href='")
					.append(TelegramUtils.getStartLink(configuration.getUsername(),
							"show_message=" + messageId + "_group=" + groupId))
					.append("'>Actions</a> for <code>").append(messageId).append("</code>]\n");

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

	public TelegramHandler showRequest() {
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

				// get message info
				if (messageId != null) {
					Optional<Request> requestOptional = requestService.findById(messageId, optional.get().getId());
					message = getRequestInfo(bot, optional.get(), requestOptional, messageId);
				} else {
					message = getErrorMessage(COMMAND_SHOW);
				}

				SendMessage sendMessage = new SendMessage(chatId, message);
				sendMessage.parseMode(ParseMode.HTML);

				sendMessageAndDelete(bot, sendMessage, 60, TimeUnit.SECONDS);
				deleteMessage(bot, update.message());
			}
		};
	}

	public TelegramCondition showRequestWithActionCondition() {
		return update -> update.message() != null && update.message().text().matches(START_PAYLOAD_SHOW);
	}

	public TelegramHandler showRequestWithAction() {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);
			String text = update.message().text().replace('_', ' ');

			Optional<Long> optionalGroupId = getGroupId(text);
			Optional<Long> optionalMessageId = getMessageId(text);

			if (optionalGroupId.isPresent() && optionalMessageId.isPresent()) {
				Long groupId = optionalGroupId.get();
				Long messageId = optionalMessageId.get();

				Optional<Group> optional = groupService.findById(groupId);
				if (optional.isPresent()) {

					Optional<Request> requestOptional = requestService.findById(messageId, optional.get().getId());
					if (requestOptional.isPresent()) {
						RequestStatus status = requestOptional.get().getStatus();

						String message = getRequestInfo(bot, optional.get(), requestOptional, messageId);
						SendMessage sendMessage = new SendMessage(chatId, message);
						sendMessage.parseMode(ParseMode.HTML);

						String callbackMessage = MESSAGE_CONDITION + messageId + " " + GROUP_CONDITION + groupId;
						String callbackBegin = ContributorAction.CONFIRM + " " + callbackMessage + " action=";

						InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
						InlineKeyboardButton requestButton = new InlineKeyboardButton("📚 Request")
								.url(TelegramUtils.getLink(groupId, messageId));
						InlineKeyboardButton doneButton = new InlineKeyboardButton("✅ Done")
								.callbackData(callbackBegin + ContributorAction.DONE);
						InlineKeyboardButton pendingButton = new InlineKeyboardButton("⏳ Pending")
								.callbackData(callbackBegin + ContributorAction.PENDING);
						InlineKeyboardButton cancelButton = new InlineKeyboardButton("✖️ Cancel")
								.callbackData(callbackBegin + ContributorAction.CANCEL);
						InlineKeyboardButton removeButton = new InlineKeyboardButton("🗑 Remove")
								.callbackData(callbackBegin + ContributorAction.REMOVE);

						inlineKeyboard.addRow(requestButton,
								status == RequestStatus.RESOLVED ? pendingButton : doneButton);
						inlineKeyboard.addRow(status == RequestStatus.CANCELLED ? pendingButton : cancelButton,
								removeButton);

						sendMessage.replyMarkup(inlineKeyboard);

						bot.execute(sendMessage);
						deleteMessage(bot, update.message());
					}
				}
			}
		};
	}

	private String getRequestInfo(TelegramBot bot, Group group, Optional<Request> requestOptional, Long messageId) {
		StringBuilder messageBuilder = new StringBuilder();

		if (requestOptional.isPresent()) {
			Request request = requestOptional.get();

			messageBuilder.append(request.getContent());
			messageBuilder.append("\n\n[");
			messageBuilder.append("Request by ").append(getUser(bot, request)).append("(<code>")
					.append(request.getUserId()).append("</code>)");
			messageBuilder.append(" in ").append("#").append(group.getName().replace(' ', '_')).append(".");
			messageBuilder.append(" Status: <b>").append(request.getStatus().getDescription().toUpperCase())
					.append("</b>");
			messageBuilder.append("]");
		} else {
			messageBuilder.append(REQUEST_NOT_FOUND);
		}

		return messageBuilder.toString();
	}

	private String getUser(TelegramBot bot, Request request) {
		GetChatMember getChatMember = new GetChatMember(request.getId().getGroupId(), request.getUserId());
		GetChatMemberResponse member = bot.execute(getChatMember);

		String user = null;
		if (member.isOk()) {
			user = TelegramUtils.tagUser(member.chatMember().user());
		} else {
			user = TelegramUtils.tagUser(request.getUserId());
		}

		return user.replace(".", "");
	}

	public TelegramCondition confirmActionCondition() {
		return update -> update.callbackQuery() != null && update.callbackQuery().data().matches(CONFIRM_CALLBACK);
	}

	public TelegramHandler confirmAction() {
		return (bot, update) -> {
			String text = update.callbackQuery().data();

			Optional<ContributorAction> actionOptional = getAction(text);
			if (actionOptional.isPresent()) {
				ContributorAction action = actionOptional.get();

				// reply to callback
				AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery(update.callbackQuery().id());
				bot.execute(answerCallbackQuery);

				// prepare confirmation button
				StringBuilder stringBuilder = new StringBuilder();
				stringBuilder.append("You chose to <code>");
				stringBuilder.append(action.getDescription());
				stringBuilder.append("</code>\n");
				stringBuilder.append("Are you sure you want to continue?\n");
				stringBuilder.append("<i>This message will disappear in 1 minute.</i>");
				SendMessage sendMessage = new SendMessage(update.callbackQuery().from().id(), stringBuilder.toString());
				sendMessage.parseMode(ParseMode.HTML);

				String callbackData = text.substring("confirm ".length(), text.indexOf(ACTION_CONDITION) - 1);

				InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
				InlineKeyboardButton yesButton = new InlineKeyboardButton("✔️ Yes")
						.callbackData(action + " " + callbackData);

				inlineKeyboard.addRow(yesButton);

				sendMessage.replyMarkup(inlineKeyboard);

				sendMessageAndDelete(bot, sendMessage, 1, TimeUnit.MINUTES);
			}
		};
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

	private Optional<Long> getMessageId(String text) {
		return getCondition(text, MESSAGE_CONDITION, Long::parseLong);
	}

	private Optional<Long> getGroupId(String text) {
		return getCondition(text, GROUP_CONDITION, Long::parseLong);
	}

	private Optional<ContributorAction> getAction(String text) {
		return getCondition(text, ACTION_CONDITION, ContributorAction::valueOf);
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
