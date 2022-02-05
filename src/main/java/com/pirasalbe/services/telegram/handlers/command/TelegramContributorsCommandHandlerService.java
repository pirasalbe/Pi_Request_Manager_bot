package com.pirasalbe.services.telegram.handlers.command;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.MessageEntity;
import com.pengrad.telegrambot.model.MessageEntity.Type;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import com.pirasalbe.models.UserRole;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.database.UserRequest;
import com.pirasalbe.models.request.Format;
import com.pirasalbe.models.request.Source;
import com.pirasalbe.models.telegram.handlers.TelegramCondition;
import com.pirasalbe.models.telegram.handlers.TelegramHandler;
import com.pirasalbe.services.GroupService;
import com.pirasalbe.services.RequestManagementService;
import com.pirasalbe.services.SchedulerService;
import com.pirasalbe.services.UserRequestService;
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
public class TelegramContributorsCommandHandlerService {

	public static final String COMMAND_PENDING = "/pending";
	public static final String COMMAND_DONE = "/done";
	public static final String COMMAND_SILENT_DONE = "/sdone";

	public static final String COMMAND_GET = "/requests";

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
	private UserRequestService userRequestService;

	@Autowired
	private SchedulerService schedulerService;

	private String getLink(Message message) {
		String chatId = message.chat().id().toString();
		String messageId = message.messageId().toString();
		return getLink(chatId, messageId);
	}

	private String getLink(String chatId, String messageId) {
		if (chatId.startsWith("-100")) {
			chatId = chatId.substring(4);
		}

		return "https://t.me/c/" + chatId + "/" + messageId;
	}

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
				String link = getLink(message);
				StringBuilder stringBuilder = new StringBuilder();
				stringBuilder.append(requestStatusMessage(link, success, "marked as pending"));
				SendMessage sendMessage = new SendMessage(chatId, stringBuilder.toString());
				sendMessage.parseMode(ParseMode.HTML);
				SendResponse response = bot.execute(sendMessage);

				// schedule delete
				schedulerService.schedule((b, r) -> b.execute(new DeleteMessage(chatId, r.message().messageId())),
						response, 5, TimeUnit.SECONDS);

				// delete command
				DeleteMessage deleteMessage = new DeleteMessage(chatId, update.message().messageId());
				bot.execute(deleteMessage);
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

				// send a message to notify operation
				String link = getLink(message);
				StringBuilder stringBuilder = new StringBuilder();
				if (reply) {
					// reply to the user only if reply
					String text = removeDoneCommand(update.message().text(), update.message().entities()).trim();
					stringBuilder.append(TelegramUtils.tagUser(message));
					stringBuilder.append(" ").append(text).append("\n");
				}
				stringBuilder.append(requestStatusMessage(link, success, "marked as done"));
				SendMessage sendMessage = new SendMessage(chatId, stringBuilder.toString());
				if (reply) {
					sendMessage.replyToMessageId(message.messageId());
				}
				sendMessage.parseMode(ParseMode.HTML);
				SendResponse response = bot.execute(sendMessage);

				// schedule delete for no reply
				if (!reply) {
					schedulerService.schedule((b, r) -> b.execute(new DeleteMessage(chatId, r.message().messageId())),
							response, 5, TimeUnit.SECONDS);
				}

				// delete command
				DeleteMessage deleteMessage = new DeleteMessage(chatId, update.message().messageId());
				bot.execute(deleteMessage);
			}
		};
	}

	private String removeDoneCommand(String text, MessageEntity[] entities) {
		StringBuilder builder = new StringBuilder();

		if (entities != null) {
			for (int i = 0; i < entities.length && builder.length() == 0; i++) {
				MessageEntity entity = entities[i];
				if (entity.type() == Type.bot_command) {
					Integer offset = entity.offset();
					builder.append(text.substring(0, offset));
					builder.append(text.substring(offset + entity.length()));
				}
			}
		}

		return builder.toString();
	}

	public TelegramCondition replyToMessageWithFileCondition() {
		return update -> replyToMessage(update)
				&& (update.message().document() != null || update.message().audio() != null);
	}

	public TelegramHandler markDoneWithFile() {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);

			Optional<Group> optional = groupService.findById(chatId);

			if (optional.isPresent()) {
				Message message = update.message().replyToMessage();

				boolean success = requestManagementService.markDone(message);

				String link = getLink(message);
				StringBuilder stringBuilder = new StringBuilder();
				stringBuilder.append(requestStatusMessage(link, success, "marked as done"));
				SendMessage sendMessage = new SendMessage(chatId, stringBuilder.toString());
				sendMessage.parseMode(ParseMode.HTML);

				SendResponse response = bot.execute(sendMessage);

				// schedule delete
				schedulerService.schedule((b, r) -> b.execute(new DeleteMessage(chatId, r.message().messageId())),
						response, 5, TimeUnit.SECONDS);
			}
		};
	}

	public TelegramHandler getGroupRequests() {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);

			Optional<Group> optional = groupService.findById(chatId);

			if (optional.isPresent()) {
				Message message = update.message();

				Optional<Format> format = getFormat(message.text());
				Optional<Source> source = getSource(message.text());
				Optional<Boolean> optionalDescendent = getDescendent(message.text());

				boolean descendent = optionalDescendent.isPresent() && optionalDescendent.get();
				List<UserRequest> requests = userRequestService.findRequests(Optional.of(chatId), source, format,
						descendent);

				String title = getTitle(format, source, descendent);

				if (requests.isEmpty()) {
					SendMessage sendMessage = new SendMessage(chatId, title + "No requests found");
					sendMessage.parseMode(ParseMode.HTML);
					bot.execute(sendMessage);
				} else {
					sendRequestList(bot, chatId, title, requests);
				}
			}
		};
	}

	private void sendRequestList(TelegramBot bot, Long chatId, String title, List<UserRequest> requests) {
		StringBuilder builder = new StringBuilder(title);

		LocalDateTime now = DateUtils.getNow();

		for (int i = 0; i < requests.size(); i++) {
			UserRequest request = requests.get(i);

			// build request text
			StringBuilder requestBuilder = new StringBuilder();
			Long messageId = request.getId().getMessageId();
			Long groupId = request.getId().getGroupId();
			requestBuilder.append("<a href='").append(getLink(groupId.toString(), messageId.toString())).append("'>");
			requestBuilder.append(messageId).append("</a> ");
			requestBuilder.append(RequestUtils.getTimeBetweenDates(request.getDate(), now)).append(" ago\n");
			String requestText = requestBuilder.toString();

			// if length is > message limit, send current text
			if (builder.length() + requestText.length() > 4096) {
				SendMessage sendMessage = new SendMessage(chatId, builder.toString());
				sendMessage.parseMode(ParseMode.HTML);
				bot.execute(sendMessage);
				builder = new StringBuilder(title);
			}
			builder.append(requestText);
			// send last message
			if (i == requests.size() - 1) {
				SendMessage sendMessage = new SendMessage(chatId, builder.toString());
				sendMessage.parseMode(ParseMode.HTML);
				bot.execute(sendMessage);
			}
		}
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
