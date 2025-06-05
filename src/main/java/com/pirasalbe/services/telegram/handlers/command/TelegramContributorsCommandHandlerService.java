package com.pirasalbe.services.telegram.handlers.command;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Audio;
import com.pengrad.telegrambot.model.Chat.Type;
import com.pengrad.telegrambot.model.Document;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.EditMessageReplyMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import com.pirasalbe.models.ContributorAction;
import com.pirasalbe.models.LookupInfo;
import com.pirasalbe.models.UserRole;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.database.Request;
import com.pirasalbe.models.request.Format;
import com.pirasalbe.models.request.RequestStatus;
import com.pirasalbe.models.request.Source;
import com.pirasalbe.models.telegram.handlers.TelegramCondition;
import com.pirasalbe.models.telegram.handlers.TelegramHandler;
import com.pirasalbe.services.RequestManagementService;
import com.pirasalbe.services.RequestService;
import com.pirasalbe.services.telegram.TelegramCommandsService;
import com.pirasalbe.services.telegram.conditions.TelegramReplyToMessageCondition;
import com.pirasalbe.services.telegram.handlers.AbstractTelegramHandlerService;
import com.pirasalbe.utils.RequestUtils;
import com.pirasalbe.utils.TelegramConditionUtils;
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
	private static final List<String> VALID_COMMON_EXTENSIONS = Arrays.asList(".zip", ".rar");
	private static final List<String> VALID_EBOOKS_EXTENSIONS = Arrays.asList(".mobi", ".pdf", ".epub", ".azw3", ".azw",
			".txt", ".doc", ".docx", ".rtf", ".cbz", ".cbr", ".djvu", ".chm", ".fb2");
	private static final List<String> VALID_AUDIOBOOKS_EXTENSIONS = Arrays.asList(".mp3", ".m4b", ".opus");
	private static final List<String> VALID_EXTENSIONS = Stream
			.of(VALID_COMMON_EXTENSIONS, VALID_EBOOKS_EXTENSIONS, VALID_AUDIOBOOKS_EXTENSIONS)
			.flatMap(Collection::stream).collect(Collectors.toList());

	private static final String START_PAYLOAD_SHOW = "^\\/start show_message=[0-9]+_group=[+-]?[0-9]+$";
	private static final String MESSAGE_INFO_CALLBACK = TelegramConditionUtils.MESSAGE_CONDITION + "[0-9]+ "
			+ TelegramConditionUtils.GROUP_CONDITION + "[+-]?[0-9]+";
	public static final String CONFIRM_CALLBACK = "^" + ContributorAction.CONFIRM.getCode() + " "
			+ MESSAGE_INFO_CALLBACK + " " + TelegramConditionUtils.ACTION_CONDITION + "[a-zA-Z]+$";
	public static final String CHANGE_STATUS_CALLBACK = "^(" + ContributorAction.PENDING.getCode() + "|"
			+ ContributorAction.IN_PROGRESS.getCode() + "|" + ContributorAction.PAUSE.getCode() + "|"
			+ ContributorAction.DONE.getCode() + "|" + ContributorAction.CANCEL.getCode() + "|"
			+ ContributorAction.REMOVE.getCode() + ") " + MESSAGE_INFO_CALLBACK + "( "
			+ TelegramConditionUtils.REFRESH_SHOW_MESSAGE_CONDITION + "[0-9]+ "
			+ TelegramConditionUtils.REFRESH_SHOW_CHAT_CONDITION + "[+-]?[0-9]+)?" + "( "
			+ ContributorAction.FORCE_DELETE + ")?$";

	public static final String COMMAND_REFRESH_COMMANDS = "/refresh_commands";

	public static final String COMMAND_SHOW = "/show";
	public static final String COMMAND_PENDING = "/pending";
	public static final String COMMAND_PAUSE = "/pause";
	public static final String COMMAND_IN_PROGRESS = "/in_progress";
	public static final String COMMAND_CANCEL = "/cancel";
	public static final String COMMAND_REMOVE = "/remove";
	public static final String COMMAND_DONE = "/done";
	public static final String COMMAND_SILENT_DONE = "/sdone";

	public static final String COMMAND_REQUESTS = "/requests";

	private static final String REQUEST_NOT_FOUND = "Request not found";

	public static final UserRole ROLE = UserRole.CONTRIBUTOR;

	@Autowired
	private TelegramReplyToMessageCondition replyToMessageCondition;

	@Autowired
	private TelegramCommandsService commandsService;

	@Autowired
	private RequestService requestService;

	@Autowired
	private RequestManagementService requestManagementService;

	public TelegramHandler refreshCommands() {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);

			commandsService.defineAdminCommandsAsync(chatId);

			SendMessage sendMessage = new SendMessage(chatId, "Refreshing commands in progress.");
			bot.execute(sendMessage);
		};
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

				boolean success = requestManagementService.markPending(message, optional.get(),
						update.message().from().id());

				// send a message to notify operation
				String link = TelegramUtils.getLink(message);
				StringBuilder stringBuilder = new StringBuilder();
				stringBuilder.append(requestStatusMessage(link, success, "marked as pending"));
				SendMessage sendMessage = new SendMessage(chatId, stringBuilder.toString());
				TelegramUtils.setMessageThreadId(sendMessage, update.message());
				sendMessage.parseMode(ParseMode.HTML);

				sendMessageAndDelete(bot, sendMessage, 5, TimeUnit.SECONDS);
				deleteMessage(bot, update.message());
			}
		};
	}

	public TelegramHandler markPaused() {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);

			Optional<Group> optional = groupService.findById(chatId);

			if (optional.isPresent()) {
				Message message = update.message().replyToMessage();

				boolean success = requestManagementService.markPaused(message, optional.get(),
						update.message().from().id());

				// send a message to notify operation
				String link = TelegramUtils.getLink(message);
				StringBuilder stringBuilder = new StringBuilder();
				stringBuilder.append(requestStatusMessage(link, success, "marked as paused"));
				SendMessage sendMessage = new SendMessage(chatId, stringBuilder.toString());
				TelegramUtils.setMessageThreadId(sendMessage, update.message());
				sendMessage.parseMode(ParseMode.HTML);

				sendMessageAndDelete(bot, sendMessage, 5, TimeUnit.SECONDS);
				deleteMessage(bot, update.message());
			}
		};
	}

	public TelegramHandler markInProgress() {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);

			Optional<Group> optional = groupService.findById(chatId);

			if (optional.isPresent()) {
				Message message = update.message().replyToMessage();

				boolean success = requestManagementService.markInProgress(message, optional.get(),
						update.message().from().id());

				// send a message to notify operation
				String link = TelegramUtils.getLink(message);
				StringBuilder stringBuilder = new StringBuilder();
				stringBuilder.append(requestStatusMessage(link, success, "marked as in progress"));
				SendMessage sendMessage = new SendMessage(chatId, stringBuilder.toString());
				TelegramUtils.setMessageThreadId(sendMessage, update.message());
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

	public TelegramHandler changeStatusWithCallback() {
		return (bot, update) -> {
			String text = update.callbackQuery().data();

			Optional<Long> optionalGroupId = TelegramConditionUtils.getGroupId(text);
			Optional<Long> optionalMessageId = TelegramConditionUtils.getMessageId(text);
			Optional<Integer> optionalShowMessageId = TelegramConditionUtils.getRefreshShowMessage(text);
			Optional<Long> optionalShowChatId = TelegramConditionUtils.getRefreshShowChat(text);

			String actionString = text.substring(0, text.indexOf(' '));
			ContributorAction action = ContributorAction.getByCode(actionString);

			String result = null;
			if (optionalGroupId.isPresent() && optionalMessageId.isPresent()) {
				Long messageId = optionalMessageId.get();
				Long groupId = optionalGroupId.get();

				result = performAction(bot, action, messageId, groupId, update.callbackQuery().from());

				if (optionalShowMessageId.isPresent() && optionalShowChatId.isPresent()) {
					// delete original messsage and send it again
					DeleteMessage deleteMessage = new DeleteMessage(optionalShowChatId.get(),
							optionalShowMessageId.get());
					bot.execute(deleteMessage);
				}

				sendRequestWithAction(bot, update.callbackQuery().from().id(), groupId, messageId);

			} else {
				result = "Request ids not found";
			}

			// callback response
			AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery(update.callbackQuery().id());
			answerCallbackQuery.text(result);

			bot.execute(answerCallbackQuery);

			// delete previous message if in private
			if (isCallbackMessageFromPM(update)
					|| update.callbackQuery().data().endsWith(ContributorAction.FORCE_DELETE)) {
				DeleteMessage deleteMessage = new DeleteMessage(update.callbackQuery().message().chat().id(),
						update.callbackQuery().message().messageId());
				bot.execute(deleteMessage);
			}

		};
	}

	private boolean isCallbackMessageFromPM(Update update) {
		return update.callbackQuery().message() != null
				&& update.callbackQuery().message().chat().type() == Type.Private;
	}

	private String performAction(TelegramBot bot, ContributorAction action, Long messageId, Long groupId,
			User contributor) {
		String result = null;

		if (action == ContributorAction.REMOVE) {

			boolean deleteRequest = requestManagementService.deleteRequest(messageId, groupId);
			result = deleteRequest ? "Request removed" : REQUEST_NOT_FOUND;

		} else if (action == ContributorAction.DONE) {

			Optional<Request> optional = requestService.findById(messageId, groupId);

			Long resolvedMessageId = notifyDone(bot, groupId, messageId, contributor, optional, false);

			result = changeRequestStatus(action, groupId, messageId, resolvedMessageId, contributor.id());

		} else if (action == ContributorAction.CANCEL || action == ContributorAction.PAUSE
				|| action == ContributorAction.PENDING || action == ContributorAction.IN_PROGRESS) {

			result = changeRequestStatus(action, groupId, messageId, contributor.id());

		} else {
			result = "Nothing to do";
		}

		return result;
	}

	private Long notifyDone(TelegramBot bot, Long groupId, Long messageId, User contributor, Optional<Request> optional,
			boolean showUndo) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Hey there 👋 Here's your requested book. Happy ");

		if (optional.isPresent() && optional.get().getFormat() == Format.AUDIOBOOK) {
			stringBuilder.append("Listening");
		} else {
			stringBuilder.append("Reading");
		}

		stringBuilder.append("!\n");
		stringBuilder.append("Request fulfilled by <code>").append(TelegramUtils.getUserName(contributor))
				.append("</code>");
		SendMessage sendMessage = new SendMessage(groupId, stringBuilder.toString());
		sendMessage.parseMode(ParseMode.HTML);
		sendMessage.replyToMessageId(messageId.intValue());

		// undo button
		if (showUndo) {
			InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
			InlineKeyboardButton undoButton = new InlineKeyboardButton("🔙 Undo")
					.callbackData(RequestUtils.getActionCallback(messageId, groupId, ContributorAction.PENDING, true));

			inlineKeyboard.addRow(undoButton);

			sendMessage.replyMarkup(inlineKeyboard);
		}

		SendResponse response = bot.execute(sendMessage);

		Long resolvedMessageId = messageId;
		if (response.isOk()) {
			resolvedMessageId = response.message().messageId().longValue();
		}

		return resolvedMessageId;
	}

	private String changeRequestStatus(ContributorAction action, Long groupId, Long messageId, Long contributorId) {
		return changeRequestStatus(action, groupId, messageId, null, contributorId);
	}

	private String changeRequestStatus(ContributorAction action, Long groupId, Long messageId, Long resolvedMessageId,
			Long contributorId) {
		String result;
		RequestStatus newStatus = null;

		switch (action) {
		case CANCEL:
			newStatus = RequestStatus.CANCELLED;
			break;
		case DONE:
			newStatus = RequestStatus.RESOLVED;
			break;
		case PAUSE:
			newStatus = RequestStatus.PAUSED;
			break;
		case IN_PROGRESS:
			newStatus = RequestStatus.IN_PROGRESS;
			break;
		case PENDING:
		default:
			newStatus = RequestStatus.PENDING;
			break;
		}

		Optional<Group> group = groupService.findById(groupId);
		Optional<Request> optional = requestService.findById(messageId, groupId);
		if (group.isPresent() && optional.isPresent()) {
			requestManagementService.updateStatus(optional.get(), group.get(), newStatus, resolvedMessageId,
					contributorId);
			result = "Request marked as " + newStatus.getDescription();
		} else {
			result = REQUEST_NOT_FOUND;
		}

		return result;
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

				// reply
				Integer resolvedMessageId = null;
				String link = TelegramUtils.getLink(message);
				if (reply) {
					resolvedMessageId = markDoneWithMessage(bot, update, chatId, message);
				}

				if (resolvedMessageId == null) {
					resolvedMessageId = message.messageId();
				}

				boolean success = requestManagementService.markDone(message, optional.get(),
						resolvedMessageId.longValue(), update.message().from().id());

				// send a message to notify operation
				StringBuilder notificationBuilder = new StringBuilder();
				notificationBuilder.append(requestStatusMessage(link, success, "marked as done"));
				SendMessage sendMessageNotification = new SendMessage(chatId, notificationBuilder.toString());
				TelegramUtils.setMessageThreadId(sendMessageNotification, update.message());
				sendMessageNotification.parseMode(ParseMode.HTML);

				sendMessageAndDelete(bot, sendMessageNotification, 5, TimeUnit.SECONDS, true);
				deleteMessage(bot, update.message());
			}

		};
	}

	private Integer markDoneWithMessage(TelegramBot bot, Update update, Long chatId, Message message) {
		StringBuilder replyBuilder = new StringBuilder();

		String text = TelegramUtils.removeCommand(update.message().text(), update.message().entities()).trim();

		replyBuilder.append(TelegramUtils.tagUser(message));
		replyBuilder.append(text).append("\n");

		replyBuilder.append("Request fulfilled by <code>").append(TelegramUtils.getUserName(update.message().from()))
				.append("</code>");

		SendMessage sendMessageReply = new SendMessage(chatId, replyBuilder.toString());
		TelegramUtils.setMessageThreadId(sendMessageReply, update.message());
		sendMessageReply.parseMode(ParseMode.HTML);

		// reply to the request
		sendMessageReply.replyToMessageId(message.messageId());

		SendResponse response = bot.execute(sendMessageReply);

		Integer resolvedMessageId = null;
		if (response.isOk()) {
			resolvedMessageId = response.message().messageId();
		}

		return resolvedMessageId;
	}

	public TelegramCondition replyToMessageWithFileCondition() {
		return update -> replyToMessageCondition.check(update) && isFile(update);
	}

	public TelegramCondition fileCondition() {
		return this::isFile;
	}

	private boolean isFile(Update update) {
		return update.message() != null
				&& (isValidDocument(update.message().document()) || isValidAudio(update.message().audio()));
	}

	private boolean isValidDocument(Document document) {
		return document != null && (isNotNullAndEmpty(document.mimeType())
				|| VALID_MIME_TYPES.contains(document.mimeType()) || isValidExtension(document.fileName()));
	}

	private boolean isValidAudio(Audio audio) {
		return audio != null && (isNotNullAndEmpty(audio.mimeType()) || VALID_MIME_TYPES.contains(audio.mimeType())
				|| isValidExtension(audio.fileName()));
	}

	private boolean isNotNullAndEmpty(String mimeType) {
		return mimeType != null && mimeType.isEmpty();
	}

	private boolean isValidExtension(String filename) {
		return isValidExtension(VALID_EXTENSIONS, filename);
	}

	private boolean isValidExtension(List<String> extensions, String filename) {
		boolean valid = false;

		String lowerFileName = filename.toLowerCase();
		for (int i = 0; i < extensions.size() && !valid; i++) {
			String extension = extensions.get(i);

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

				boolean success = requestManagementService.markDone(message, optional.get(),
						update.message().messageId().longValue(), update.message().from().id());

				String link = TelegramUtils.getLink(message);
				StringBuilder stringBuilder = new StringBuilder();
				stringBuilder.append(requestStatusMessage(link, success, "marked as done"));
				SendMessage sendMessage = new SendMessage(chatId, stringBuilder.toString());
				TelegramUtils.setMessageThreadId(sendMessage, update.message());
				sendMessage.parseMode(ParseMode.HTML);

				sendMessageAndDelete(bot, sendMessage, 5, TimeUnit.SECONDS);
			}
		};
	}

	public TelegramHandler lookupWithFile() {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);

			Optional<Group> optional = groupService.findById(chatId);

			if (optional.isPresent()) {

				// get data from document
				LookupInfo lookupInfo = getLookupInfo(update);

				// if valid data look for matching request
				if (lookupInfo.isValid()) {
					List<Request> requests = requestManagementService.lookup(chatId, lookupInfo.getName(),
							lookupInfo.getCaption(), lookupInfo.getFormat());

					for (Request request : requests) {
						Long groupId = request.getId().getGroupId();
						Long messageId = request.getId().getMessageId();
						User contributor = update.message().from();

						// notify user
						Long doneMessageId = notifyDone(bot, groupId, messageId, contributor, Optional.of(request),
								true);

						// change status
						Long resolvedMessageId = update.message().messageId().longValue();
						changeRequestStatus(ContributorAction.DONE, groupId, messageId, resolvedMessageId,
								contributor.id());

						// remove button after some time
						EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup(groupId,
								doneMessageId.intValue());
						schedulerService.schedule((b, m) -> b.execute(m), editMessageReplyMarkup, 5, TimeUnit.MINUTES);
					}
				}
			}
		};
	}

	private LookupInfo getLookupInfo(Update update) {
		boolean valid = false;
		Format format = null;
		String name = null;
		String caption = update.message().caption();
		if (update.message().document() != null) {
			name = update.message().document().fileName();
			if (isValidExtension(VALID_AUDIOBOOKS_EXTENSIONS, update.message().document().fileName())) {
				format = Format.AUDIOBOOK;
				valid = true;
			} else if (isValidExtension(VALID_EBOOKS_EXTENSIONS, update.message().document().fileName())) {
				format = Format.EBOOK;
				valid = true;
			} else if (isValidExtension(VALID_COMMON_EXTENSIONS, update.message().document().fileName())) {
				valid = true;
			}
		} else if (update.message().audio() != null
				&& isValidExtension(VALID_AUDIOBOOKS_EXTENSIONS, update.message().audio().fileName())) {
			name = update.message().audio().fileName();
			format = Format.AUDIOBOOK;
			valid = true;
		}

		return new LookupInfo(valid, name, caption, format);
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
					boolean success = requestManagementService.markCancelled(messageId, optional.get(),
							update.message().from().id());

					String link = TelegramUtils.getLink(chatId.toString(), messageId.toString());
					StringBuilder builder = new StringBuilder();
					builder.append(requestStatusMessage(link, success, "marked as cancelled"));
					message = builder.toString();
				} else {
					message = getErrorMessage(COMMAND_CANCEL);
				}

				SendMessage sendMessage = new SendMessage(chatId, message);
				TelegramUtils.setMessageThreadId(sendMessage, update.message());
				sendMessage.parseMode(ParseMode.HTML);

				sendMessageAndDelete(bot, sendMessage, 5, TimeUnit.SECONDS);
				deleteMessage(bot, update.message());
			}
		};
	}

	public TelegramHandler removeRequest() {
		return (bot, update) -> {
			deleteMessage(bot, update.message());

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
				TelegramUtils.setMessageThreadId(sendMessage, update.message());
				sendMessage.parseMode(ParseMode.HTML);

				sendMessageAndDelete(bot, sendMessage, 5, TimeUnit.SECONDS);
			}
		};
	}

	public TelegramHandler findRequests() {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);

			String text = update.message().text();
			boolean isPrivate = update.message().chat().type() == Type.Private;
			Optional<Long> group = getGroup(chatId, text, isPrivate);

			// check if the context is valid, either enabled group or PM
			if (groupService.existsById(chatId) || isPrivate) {
				deleteMessage(bot, update.message(), !isPrivate);

				Optional<Long> user = TelegramConditionUtils.getUserId(text);
				Optional<RequestStatus> status = TelegramConditionUtils.getStatus(text);
				Optional<Format> format = TelegramConditionUtils.getFormat(text);
				Optional<Source> source = TelegramConditionUtils.getSource(text);
				Optional<String> otherTags = TelegramConditionUtils.getOtherTags(text);
				Optional<Boolean> optionalDescendent = TelegramConditionUtils.getDescendent(text);

				boolean descendent = optionalDescendent.isPresent() && optionalDescendent.get();
				RequestStatus requestStatus = status.orElse(RequestStatus.PENDING);
				List<Request> requests = requestService.findRequests(group, requestStatus, user, source, format,
						otherTags, descendent);

				String title = getTitle(requestStatus, group, user, format, source, otherTags, descendent);

				if (requests.isEmpty()) {
					SendMessage sendMessage = new SendMessage(chatId, title + "No requests found");
					sendMessage.parseMode(ParseMode.HTML);
					sendMessageAndDelete(bot, sendMessage, 30, TimeUnit.SECONDS, group.isPresent());
				} else {
					sendRequestList(chatId, group, title, requests, true);
				}
			}
		};
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
					message = getRequestInfo(bot, optional.get(), requestOptional);
				} else {
					message = getErrorMessage(COMMAND_SHOW);
				}

				SendMessage sendMessage = new SendMessage(chatId, message);
				TelegramUtils.setMessageThreadId(sendMessage, update.message());
				sendMessage.parseMode(ParseMode.HTML);
				sendMessage.disableWebPagePreview(true);

				sendMessageAndDelete(bot, sendMessage, 60, TimeUnit.SECONDS);
				deleteMessage(bot, update.message());
			}
		};
	}

	public TelegramCondition showRequestWithActionCondition() {
		return update -> update.message() != null && update.message().text() != null
				&& update.message().text().matches(START_PAYLOAD_SHOW);
	}

	public TelegramHandler showRequestWithAction() {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);
			String text = update.message().text().replace('_', ' ');

			Optional<Long> optionalGroupId = TelegramConditionUtils.getGroupId(text);
			Optional<Long> optionalMessageId = TelegramConditionUtils.getMessageId(text);

			if (optionalGroupId.isPresent() && optionalMessageId.isPresent()) {
				Long groupId = optionalGroupId.get();
				Long messageId = optionalMessageId.get();

				sendRequestWithAction(bot, chatId, groupId, messageId);
				deleteMessage(bot, update.message());
			}
		};
	}

	private void sendRequestWithAction(TelegramBot bot, Long chatId, Long groupId, Long messageId) {
		Optional<Group> optional = groupService.findById(groupId);
		if (optional.isPresent()) {

			Optional<Request> requestOptional = requestService.findById(messageId, groupId);
			if (requestOptional.isPresent()) {
				RequestStatus status = requestOptional.get().getStatus();

				String message = getRequestInfo(bot, optional.get(), requestOptional);
				SendMessage sendMessage = new SendMessage(chatId, message);
				sendMessage.parseMode(ParseMode.HTML);
				sendMessage.disableWebPagePreview(true);
				InlineKeyboardMarkup inlineKeyboard = RequestUtils.getRequestKeyboard(configuration.getUsername(),
						groupId, messageId, status, "📝 Refresh");

				sendMessage.replyMarkup(inlineKeyboard);
				sendMessage.disableNotification(true);

				bot.execute(sendMessage);
			}
		}
	}

	private String getRequestInfo(TelegramBot bot, Group group, Optional<Request> requestOptional) {
		StringBuilder messageBuilder = new StringBuilder();

		if (requestOptional.isPresent()) {
			Request request = requestOptional.get();

			String requestInfo = RequestUtils.getRequestInfo(bot, group.getName(), request);
			messageBuilder.append(requestInfo);
		} else {
			messageBuilder.append(REQUEST_NOT_FOUND);
		}

		return messageBuilder.toString();
	}

	public TelegramHandler confirmAction() {
		return (bot, update) -> {
			String text = update.callbackQuery().data();

			Optional<ContributorAction> actionOptional = TelegramConditionUtils.getAction(text);
			Optional<Long> optionalGroupId = TelegramConditionUtils.getGroupId(text);
			Optional<Long> optionalMessageId = TelegramConditionUtils.getMessageId(text);
			if (actionOptional.isPresent() && optionalGroupId.isPresent() && optionalMessageId.isPresent()) {
				ContributorAction action = actionOptional.get();

				// reply to callback
				AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery(update.callbackQuery().id());
				bot.execute(answerCallbackQuery);

				// prepare confirmation button
				StringBuilder stringBuilder = new StringBuilder();
				stringBuilder.append("You chose to <code>");
				stringBuilder.append(action.getDescription());
				stringBuilder.append("</code> <a href='")
						.append(TelegramUtils.getLink(optionalGroupId.get(), optionalMessageId.get()))
						.append("'>this request.</a>\n");
				stringBuilder.append("Are you sure you want to continue?\n");
				stringBuilder.append("<i>This message will disappear in 1 minute.</i>");
				SendMessage sendMessage = new SendMessage(update.callbackQuery().from().id(), stringBuilder.toString());
				sendMessage.parseMode(ParseMode.HTML);

				// get previous message
				Long showRequestMessageId = null;
				Long showRequestChatId = null;
				if (isCallbackMessageFromPM(update)) {
					showRequestMessageId = update.callbackQuery().message().messageId().longValue();
					showRequestChatId = update.callbackQuery().from().id();
				}

				InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();
				InlineKeyboardButton yesButton = new InlineKeyboardButton("✔️ Yes").callbackData(
						RequestUtils.getActionCallback(optionalMessageId.get(), optionalGroupId.get(), action,
								Optional.ofNullable(showRequestMessageId), Optional.ofNullable(showRequestChatId)));

				inlineKeyboard.addRow(yesButton);

				sendMessage.replyMarkup(inlineKeyboard);

				sendMessageAndDelete(bot, sendMessage, 1, TimeUnit.MINUTES);
			}
		};
	}

	private String getTitle(RequestStatus requestStatus, Optional<Long> group, Optional<Long> user,
			Optional<Format> format, Optional<Source> source, Optional<String> otherTags, boolean descendent) {
		StringBuilder title = new StringBuilder();
		title.append("<b>Requests ").append(requestStatus.getDescription()).append("</b>");
		if (group.isPresent()) {
			Long groupId = group.get();
			Optional<Group> groupOptional = groupService.findById(groupId);
			title.append("\nGroup [").append(groupOptional.orElseThrow().getName()).append(" (<code>").append(groupId)
					.append("</code>)]");
		}
		if (user.isPresent()) {
			title.append("\nUser [<code>").append(user.get()).append("</code>]");
		}
		if (format.isPresent()) {
			title.append("\nFormat [").append(format.get()).append("]");
		}
		if (source.isPresent()) {
			title.append("\nSource [").append(source.get()).append("]");
		}
		if (otherTags.isPresent()) {
			title.append("\nOther [").append(otherTags.get()).append("]");
		}
		title.append("\nShow ").append(
				descendent ? TelegramConditionUtils.ORDER_CONDITION_NEW : TelegramConditionUtils.ORDER_CONDITION_OLD)
				.append(" first.").append("\n\n");

		return title.toString();
	}

}
