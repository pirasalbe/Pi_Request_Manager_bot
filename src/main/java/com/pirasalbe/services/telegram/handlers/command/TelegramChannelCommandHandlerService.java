package com.pirasalbe.services.telegram.handlers.command;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.models.ChannelRuleType;
import com.pirasalbe.models.UserRole;
import com.pirasalbe.models.database.ChannelRule;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.request.Format;
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

	public static final String COMMAND_CONFIGURE = "/configure_channel";
	public static final String COMMAND_DISABLE = "/disable_channel";

	private static final String CALLBACK_DATA_BASE = COMMAND_CONFIGURE + " ";

	public static final UserRole ROLE = UserRole.CONTRIBUTOR;

	@Autowired
	private ChannelManagementService channelManagementService;

	@Autowired
	private GroupService groupService;

	private void sendMessage(TelegramBot bot, Update update, SendMessage sendMessage) {
		sendMessageAndDelete(bot, sendMessage, 30, TimeUnit.SECONDS);
		deleteMessage(bot, update.message());
	}

	public TelegramHandler disableChannel() {
		return (bot, update) -> {
			Long chatId = TelegramUtils.getChatId(update);

			channelManagementService.deleteIfExists(chatId);
			SendMessage sendMessage = new SendMessage(chatId, "Channel disabled");

			sendMessage(bot, update, sendMessage);
		};
	}

	public TelegramHandler startConfiguration() {
		return (bot, update) -> {
			deleteMessage(bot, update.message());

			Long chatId = TelegramUtils.getChatId(update);

			channelManagementService.insertIfNotExists(chatId, update.message().chat().title());

			sendGroupConfiguration(bot, chatId);
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

			if (text.contains(TelegramConditionUtils.GROUP_CONDITION)) {
				manageGroupCondition(chatId, text);

				sendGroupConfiguration(bot, chatId);
			} else if (text.contains(TelegramConditionUtils.FORMAT_CONDITION)) {
				manageFormatCondition(chatId, text);

				sendFormatConfiguration(bot, chatId);
			}
		};
	}

	private <T> void sendConfiguration(TelegramBot bot, Long chatId, String messageText, ChannelRuleType type,
			String condition, List<T> possibleValues, Function<T, String> valueFunction,
			Function<T, String> buttonNameFunction, InlineKeyboardButton previousButton,
			InlineKeyboardButton nextButton) {
		SendMessage sendMessage = new SendMessage(chatId, messageText);
		sendMessage.parseMode(ParseMode.HTML);

		// prepare keyboard
		InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();

		String callbackDataBase = COMMAND_CONFIGURE + " ";
		List<InlineKeyboardButton> buttons = new LinkedList<>();

		// get rules
		List<ChannelRule> rules = channelManagementService.getChannelRulesByType(chatId, type);

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
			button.callbackData(callbackDataBase + condition + value);
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

	private InlineKeyboardButton getGroupsButton() {
		InlineKeyboardButton format = new InlineKeyboardButton("👥 Groups");
		format.callbackData(CALLBACK_DATA_BASE + TelegramConditionUtils.GROUP_CONDITION);
		return format;
	}

	private InlineKeyboardButton getFormatsButton() {
		InlineKeyboardButton format = new InlineKeyboardButton("🔎 Formats");
		format.callbackData(CALLBACK_DATA_BASE + TelegramConditionUtils.FORMAT_CONDITION);
		return format;
	}

	private InlineKeyboardButton getSourcesButton() {
		InlineKeyboardButton source = new InlineKeyboardButton("🌐 Sources");
		source.callbackData(CALLBACK_DATA_BASE + TelegramConditionUtils.SOURCE_CONDITION);
		return source;
	}

	private void sendGroupConfiguration(TelegramBot bot, Long chatId) {

		StringBuilder messageTextBuilder = new StringBuilder();
		messageTextBuilder.append("Select the groups from which you want receive requests.\n");
		messageTextBuilder.append("<b>Leave empty to receive from all groups.</b>");

		List<Group> groups = groupService.findAll();

		sendConfiguration(bot, chatId, messageTextBuilder.toString(), ChannelRuleType.GROUP,
				TelegramConditionUtils.GROUP_CONDITION, groups, g -> g.getId().toString(), Group::getName, null,
				getFormatsButton());
	}

	private void sendFormatConfiguration(TelegramBot bot, Long chatId) {
		StringBuilder messageTextBuilder = new StringBuilder();
		messageTextBuilder.append("Select the format of the requests you want to receive.\n");
		messageTextBuilder.append("<b>Leave empty to receive all.</b>");

		List<Format> formats = Arrays.asList(Format.values());

		sendConfiguration(bot, chatId, messageTextBuilder.toString(), ChannelRuleType.FORMAT,
				TelegramConditionUtils.FORMAT_CONDITION, formats, Format::name, Format::name, getGroupsButton(),
				getSourcesButton());
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

}
