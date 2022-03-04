package com.pirasalbe.services.telegram.handlers.command;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.GetChat;
import com.pengrad.telegrambot.request.PinChatMessage;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetChatResponse;
import com.pengrad.telegrambot.response.SendResponse;
import com.pirasalbe.models.ChannelRuleType;
import com.pirasalbe.models.UserRole;
import com.pirasalbe.models.database.ChannelRule;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.request.Format;
import com.pirasalbe.models.request.RequestStatus;
import com.pirasalbe.models.request.Source;
import com.pirasalbe.models.telegram.handlers.TelegramHandler;
import com.pirasalbe.services.ChannelManagementService;
import com.pirasalbe.services.GroupService;
import com.pirasalbe.services.telegram.handlers.AbstractTelegramHandlerService;
import com.pirasalbe.utils.TelegramConditionUtils;
import com.pirasalbe.utils.TelegramUtils;

/**
 * Service to manage channel related commands
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramChannelCommandHandlerService extends AbstractTelegramHandlerService {

	public static final String COMMAND_CHANNEL_ID = "/channel_id";
	public static final String COMMAND_CONFIGURE = "/configure_channel";
	public static final String COMMAND_DISABLE = "/disable_channel";
	public static final String COMMAND_REFRESH = "/refresh_channel";

	private static final String CALLBACK_DATA_BASE = COMMAND_CONFIGURE + " ";

	public static final UserRole ROLE = UserRole.CONTRIBUTOR;

	private static final String LEAVE_EMPTY_TO_RECEIVE_ALL = "<b>Leave empty to receive all.</b>";

	@Autowired
	private ChannelManagementService channelManagementService;

	@Autowired
	private GroupService groupService;

	public TelegramHandler getId() {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);

			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("Channel id: ");
			stringBuilder.append("<code>").append(chatId).append("</code>");
			SendMessage sendMessage = new SendMessage(chatId, stringBuilder.toString());
			sendMessage.parseMode(ParseMode.HTML);

			sendMessageAndDelete(bot, sendMessage, 30, TimeUnit.SECONDS);
			deleteMessage(bot, TelegramUtils.getMessage(update));
		};
	}

	private long getChannelIdFromText(Update update) {
		Message message = update.message();

		String messageText = TelegramUtils.removeCommand(message.text(), update.message().entities());

		return Long.parseLong(messageText.trim());
	}

	public TelegramHandler disableChannel() {
		return (bot, update) -> {
			Long channelId = getChannelIdFromText(update);
			Long chatId = TelegramUtils.getChatId(update);

			channelManagementService.deleteIfExists(channelId);
			SendMessage sendMessage = new SendMessage(chatId, "Channel disabled");

			bot.execute(sendMessage);
		};
	}

	public TelegramHandler startConfiguration() {
		return (bot, update) -> {
			Long channelId = getChannelIdFromText(update);
			Long chatId = TelegramUtils.getChatId(update);

			GetChat getChat = new GetChat(channelId);
			GetChatResponse getChatResponse = bot.execute(getChat);

			if (getChatResponse.isOk()) {
				channelManagementService.insertIfNotExists(channelId, getChatResponse.chat().title());

				sendGroupConfiguration(bot, chatId, channelId);
			} else {
				SendMessage sendMessage = new SendMessage(chatId, "Add me first to the channel");
				bot.execute(sendMessage);
			}
		};
	}

	private void answerCallbackQueryEmpty(TelegramBot bot, Update update) {
		AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery(update.callbackQuery().id());
		bot.execute(answerCallbackQuery);
	}

	public TelegramHandler configuration() {
		return (bot, update) -> {
			answerCallbackQueryEmpty(bot, update);
			deleteMessage(bot, update.callbackQuery().message());

			Long chatId = update.callbackQuery().message().chat().id();
			String text = update.callbackQuery().data();

			Optional<Long> channelIdOptional = TelegramConditionUtils.getChannelId(text);

			if (channelIdOptional.isPresent()) {
				Long channelId = channelIdOptional.get();

				if (text.contains(TelegramConditionUtils.GROUP_CONDITION)) {
					manageGroupCondition(channelId, text);

					sendGroupConfiguration(bot, chatId, channelId);
				} else if (text.contains(TelegramConditionUtils.FORMAT_CONDITION)) {
					manageFormatCondition(channelId, text);

					sendFormatConfiguration(bot, chatId, channelId);
				} else if (text.contains(TelegramConditionUtils.SOURCE_CONDITION)) {
					manageSourceCondition(channelId, text);

					sendSourceConfiguration(bot, chatId, channelId);
				} else if (text.contains(TelegramConditionUtils.STATUS_CONDITION)) {
					manageStatusCondition(channelId, text);

					sendStatusConfiguration(bot, chatId, channelId);
				} else {
					sendConfigurationEnd(bot, chatId, channelId);
				}
			}
		};
	}

	private String getCallback(Long channelId, String condition) {
		return getCallback(channelId, condition, null);
	}

	private String getCallback(Long channelId, String condition, Object value) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(CALLBACK_DATA_BASE);
		stringBuilder.append(TelegramConditionUtils.CHANNEL_CONDITION).append(channelId).append(" ");
		stringBuilder.append(condition);
		if (value != null) {
			stringBuilder.append(value);
		}
		return stringBuilder.toString();
	}

	private <T> void sendConfiguration(TelegramBot bot, Long chatId, Long channelId, String messageText,
			ChannelRuleType type, String condition, List<T> possibleValues, Function<T, String> valueFunction,
			Function<T, String> buttonNameFunction, InlineKeyboardButton previousButton,
			InlineKeyboardButton nextButton) {
		SendMessage sendMessage = new SendMessage(chatId, messageText);
		sendMessage.parseMode(ParseMode.HTML);

		// prepare keyboard
		InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();

		List<InlineKeyboardButton> buttons = new LinkedList<>();

		// get rules
		List<ChannelRule> rules = channelManagementService.findChannelRulesByType(channelId, type);

		// prepare buttons
		for (int i = 0; i < possibleValues.size(); i++) {
			T possibleValue = possibleValues.get(i);

			String name = buttonNameFunction.apply(possibleValue);
			String value = valueFunction.apply(possibleValue);

			// check if there is a rule for the value
			boolean isSelected = rules.stream().anyMatch(c -> c.getId().getValue().equals(value));

			StringBuilder formatName = new StringBuilder();
			if (isSelected) {
				formatName.append("✅ ");
			}
			formatName.append(name);
			InlineKeyboardButton button = new InlineKeyboardButton(formatName.toString());
			button.callbackData(getCallback(channelId, condition, value));
			buttons.add(button);

			if ((i > 1 && (i + 1) % 3 == 0) || i == possibleValues.size() - 1) {
				// every 3 groups or on the last one
				inlineKeyboard.addRow(buttons.toArray(new InlineKeyboardButton[0]));
				buttons = new LinkedList<>();
			}
		}

		if (previousButton != null) {
			inlineKeyboard.addRow(previousButton, nextButton);
		} else {
			inlineKeyboard.addRow(nextButton);
		}

		sendMessage.replyMarkup(inlineKeyboard);

		bot.execute(sendMessage);
	}

	private InlineKeyboardButton getGroupsButton(Long channelId) {
		InlineKeyboardButton format = new InlineKeyboardButton("👥 Groups");
		format.callbackData(getCallback(channelId, TelegramConditionUtils.GROUP_CONDITION));
		return format;
	}

	private InlineKeyboardButton getFormatsButton(Long channelId) {
		InlineKeyboardButton format = new InlineKeyboardButton("🔎 Formats");
		format.callbackData(getCallback(channelId, TelegramConditionUtils.FORMAT_CONDITION));
		return format;
	}

	private InlineKeyboardButton getSourcesButton(Long channelId) {
		InlineKeyboardButton source = new InlineKeyboardButton("🌐 Sources");
		source.callbackData(getCallback(channelId, TelegramConditionUtils.SOURCE_CONDITION));
		return source;
	}

	private InlineKeyboardButton getStatusesButton(Long channelId) {
		InlineKeyboardButton source = new InlineKeyboardButton("💭 Statuses");
		source.callbackData(getCallback(channelId, TelegramConditionUtils.STATUS_CONDITION));
		return source;
	}

	private InlineKeyboardButton getFinishButton(Long channelId) {
		InlineKeyboardButton source = new InlineKeyboardButton("👌 End configuration");
		source.callbackData(getCallback(channelId, "end"));
		return source;
	}

	private void sendGroupConfiguration(TelegramBot bot, Long chatId, Long channelId) {

		StringBuilder messageTextBuilder = new StringBuilder();
		messageTextBuilder.append("Select the groups from which you want receive requests.\n");
		messageTextBuilder.append("<b>Leave empty to receive from all groups.</b>");

		List<Group> groups = groupService.findAll();

		sendConfiguration(bot, chatId, channelId, messageTextBuilder.toString(), ChannelRuleType.GROUP,
				TelegramConditionUtils.GROUP_CONDITION, groups, g -> g.getId().toString(), Group::getName, null,
				getFormatsButton(channelId));
	}

	private void sendFormatConfiguration(TelegramBot bot, Long chatId, Long channelId) {
		StringBuilder messageTextBuilder = new StringBuilder();
		messageTextBuilder.append("Select the format of the requests you want to receive.\n");
		messageTextBuilder.append(LEAVE_EMPTY_TO_RECEIVE_ALL);

		List<Format> formats = Arrays.asList(Format.values());

		sendConfiguration(bot, chatId, channelId, messageTextBuilder.toString(), ChannelRuleType.FORMAT,
				TelegramConditionUtils.FORMAT_CONDITION, formats, Format::name, Format::name,
				getGroupsButton(channelId), getSourcesButton(channelId));
	}

	private void sendSourceConfiguration(TelegramBot bot, Long chatId, Long channelId) {
		StringBuilder messageTextBuilder = new StringBuilder();
		messageTextBuilder.append("Select the source of the requests you want to receive.\n");
		messageTextBuilder.append(LEAVE_EMPTY_TO_RECEIVE_ALL);

		List<Source> sources = Arrays.asList(Source.values());

		sendConfiguration(bot, chatId, channelId, messageTextBuilder.toString(), ChannelRuleType.SOURCE,
				TelegramConditionUtils.SOURCE_CONDITION, sources, Source::name, Source::name,
				getFormatsButton(channelId), getStatusesButton(channelId));
	}

	private void sendStatusConfiguration(TelegramBot bot, Long chatId, Long channelId) {
		StringBuilder messageTextBuilder = new StringBuilder();
		messageTextBuilder.append("Select the status of the requests you want to receive.\n");
		messageTextBuilder.append(LEAVE_EMPTY_TO_RECEIVE_ALL);

		List<RequestStatus> status = Arrays.asList(RequestStatus.values());

		sendConfiguration(bot, chatId, channelId, messageTextBuilder.toString(), ChannelRuleType.STATUS,
				TelegramConditionUtils.STATUS_CONDITION, status, RequestStatus::name,
				s -> s.getDescription().toUpperCase(), getSourcesButton(channelId), getFinishButton(channelId));
	}

	private <T> void manageCondition(Long channelId, ChannelRuleType type, Optional<T> optional) {
		if (optional.isPresent()) {
			channelManagementService.toggleRule(channelId, type, optional.get().toString());
		}
	}

	private void manageGroupCondition(Long channelId, String text) {
		Optional<Long> optional = TelegramConditionUtils.getGroupId(text);
		manageCondition(channelId, ChannelRuleType.GROUP, optional);
	}

	private void manageFormatCondition(Long channelId, String text) {
		Optional<Format> optional = TelegramConditionUtils.getFormat(text);
		manageCondition(channelId, ChannelRuleType.FORMAT, optional);
	}

	private void manageSourceCondition(Long channelId, String text) {
		Optional<Source> optional = TelegramConditionUtils.getSource(text);
		manageCondition(channelId, ChannelRuleType.SOURCE, optional);
	}

	private void manageStatusCondition(Long channelId, String text) {
		Optional<RequestStatus> optional = TelegramConditionUtils.getStatus(text);
		manageCondition(channelId, ChannelRuleType.STATUS, optional);
	}

	private void sendConfigurationEnd(TelegramBot bot, Long chatId, Long channelId) {
		// notify end of configuration to user
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Configurations saved.\n");
		stringBuilder.append("Send <code>").append(COMMAND_REFRESH).append(" ").append(channelId)
				.append("</code> to reload the requests.");
		SendMessage sendMessage = new SendMessage(chatId, stringBuilder.toString());
		sendMessage.parseMode(ParseMode.HTML);
		bot.execute(sendMessage);

		// send configuration to the channel
		List<ChannelRule> channelRules = channelManagementService.findChannelRules(channelId);

		StringBuilder configurationBuilder = new StringBuilder();
		configurationBuilder.append("<b>Forwarding configurations</b>\n\n");
		configurationBuilder.append("Groups: <i>").append(getRulesByType(channelRules, ChannelRuleType.GROUP))
				.append("</i>\n");
		configurationBuilder.append("Formats: <i>").append(getRulesByType(channelRules, ChannelRuleType.FORMAT))
				.append("</i>\n");
		configurationBuilder.append("Sources: <i>").append(getRulesByType(channelRules, ChannelRuleType.SOURCE))
				.append("</i>\n");
		configurationBuilder.append("Statuses: <i>").append(getRulesByType(channelRules, ChannelRuleType.STATUS))
				.append("</i>");
		SendMessage sendConfigurationMessage = new SendMessage(channelId, configurationBuilder.toString());
		sendConfigurationMessage.parseMode(ParseMode.HTML);
		SendResponse sendResponse = bot.execute(sendConfigurationMessage);

		if (sendResponse.isOk()) {
			PinChatMessage pinChatMessage = new PinChatMessage(channelId, sendResponse.message().messageId());
			bot.execute(pinChatMessage);
		}
	}

	private String getRulesByType(List<ChannelRule> channelRules, ChannelRuleType type) {
		List<String> filteredRuleValues = channelRules.stream().filter(r -> r.getId().getType().equals(type))
				.map(r -> r.getId().getValue()).collect(Collectors.toList());

		String values = null;

		if (filteredRuleValues.isEmpty()) {
			values = "all";
		} else {
			values = String.join(", ", filteredRuleValues);
		}

		return values;
	}

}
