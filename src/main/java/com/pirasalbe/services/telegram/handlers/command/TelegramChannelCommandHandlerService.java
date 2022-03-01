package com.pirasalbe.services.telegram.handlers.command;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
import com.pirasalbe.models.telegram.handlers.TelegramHandler;
import com.pirasalbe.services.ChannelManagementService;
import com.pirasalbe.services.GroupService;
import com.pirasalbe.services.telegram.handlers.AbstractTelegramHandlerService;
import com.pirasalbe.utils.TelegramUtils;

/**
 * Service to manage channel related commands
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramChannelCommandHandlerService extends AbstractTelegramHandlerService {

	private static final String GROUP_CONDITION = "group=";
	private static final String STATUS_CONDITION = "status=";
	private static final String FORMAT_CONDITION = "format=";
	private static final String SOURCE_CONDITION = "source=";

	public static final String COMMAND_CONFIGURE = "/configure_channel";
	public static final String COMMAND_DISABLE = "/disable_channel";

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

	private void sendGroupConfiguration(TelegramBot bot, Long chatId) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Select the groups from which you want receive requests.\n");
		stringBuilder.append("<b>Leave empty to receive from all groups.</b>");
		SendMessage sendMessage = new SendMessage(chatId, stringBuilder.toString());
		sendMessage.parseMode(ParseMode.HTML);

		// prepare keyboard
		InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup();

		String callbackDataBase = COMMAND_CONFIGURE + " ";
		List<InlineKeyboardButton> buttons = new LinkedList<>();

		// get groups and rules
		List<Group> groups = groupService.findAll();
		List<ChannelRule> rules = channelManagementService.getChannelRulesByType(chatId, ChannelRuleType.GROUP);

		// prepare buttons
		for (int i = 0; i < groups.size(); i++) {
			Group group = groups.get(i);

			// check if there is a rule for the group
			boolean isSelected = rules.stream().anyMatch(c -> c.getId().getValue().equals(group.getId().toString()));

			StringBuilder groupName = new StringBuilder();
			if (isSelected) {
				groupName.append("✅ ");
			}
			groupName.append(group.getName());
			InlineKeyboardButton button = new InlineKeyboardButton(groupName.toString());
			button.callbackData(callbackDataBase + GROUP_CONDITION + group.getId());
			buttons.add(button);

			if ((i > 1 && (i + 1) % 3 == 0) || i == groups.size() - 1) {
				// every 3 groups or on the last one
				inlineKeyboard.addRow(buttons.toArray(new InlineKeyboardButton[0]));
				buttons = new LinkedList<>();
			}
		}

		InlineKeyboardButton format = new InlineKeyboardButton("🔎 Format");
		format.callbackData(callbackDataBase + GROUP_CONDITION + group.getId());
		inlineKeyboard.addRow(format);

		sendMessage.replyMarkup(inlineKeyboard);

		bot.execute(sendMessage);
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

			sendGroupConfiguration(bot, chatId);
		};
	}

}
