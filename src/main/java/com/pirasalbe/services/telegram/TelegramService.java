package com.pirasalbe.services.telegram;

import java.util.Arrays;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.model.Chat.Type;
import com.pengrad.telegrambot.model.botcommandscope.BotCommandScopeAllChatAdministrators;
import com.pengrad.telegrambot.model.botcommandscope.BotCommandScopeAllGroupChats;
import com.pengrad.telegrambot.model.botcommandscope.BotCommandScopeAllPrivateChats;
import com.pengrad.telegrambot.model.botcommandscope.BotCommandScopeDefault;
import com.pengrad.telegrambot.request.SetMyCommands;
import com.pirasalbe.models.telegram.handlers.TelegramCondition;
import com.pirasalbe.services.telegram.conditions.TelegramCallbackQueryConditionFactory;
import com.pirasalbe.services.telegram.conditions.TelegramCallbackQueryConditionFactory.Condition;
import com.pirasalbe.services.telegram.conditions.TelegramChatConditionFactory;
import com.pirasalbe.services.telegram.conditions.TelegramCommandConditionFactory;
import com.pirasalbe.services.telegram.conditions.TelegramReplyToCommandConditionFactory;
import com.pirasalbe.services.telegram.conditions.TelegramRoleConditionFactory;
import com.pirasalbe.services.telegram.handlers.command.TelegramAliveCommandHandlerService;
import com.pirasalbe.services.telegram.handlers.command.TelegramChannelCommandHandlerService;
import com.pirasalbe.services.telegram.handlers.command.TelegramContributorsCommandHandlerService;
import com.pirasalbe.services.telegram.handlers.command.TelegramDeleteCommandHandlerService;
import com.pirasalbe.services.telegram.handlers.command.TelegramGroupsCommandHandlerService;
import com.pirasalbe.services.telegram.handlers.command.TelegramHelpCommandHandlerService;
import com.pirasalbe.services.telegram.handlers.command.TelegramMeCommandHandlerService;
import com.pirasalbe.services.telegram.handlers.command.TelegramNextAudiobookCommandHandlerService;
import com.pirasalbe.services.telegram.handlers.command.TelegramSuperAdminCommandHandlerService;
import com.pirasalbe.services.telegram.handlers.request.TelegramBumpRequestHandlerService;
import com.pirasalbe.services.telegram.handlers.request.TelegramNewRequestHandlerService;
import com.pirasalbe.services.telegram.handlers.request.TelegramUpdateRequestHandlerService;

/**
 * Service to manage the telegram logic
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramService {

	@Autowired
	private TelegramBotService bot;

	/*
	 * CONDITIONS
	 */

	@Autowired
	private TelegramChatConditionFactory chatConditionFactory;

	@Autowired
	@Qualifier("telegramCommandConditionFactory")
	private TelegramCommandConditionFactory commandConditionFactory;

	@Autowired
	private TelegramReplyToCommandConditionFactory replyToCommandConditionFactory;

	@Autowired
	private TelegramCallbackQueryConditionFactory callbackQueryConditionFactory;

	@Autowired
	private TelegramRoleConditionFactory roleConditionFactory;

	/*
	 * HANDLERS
	 */

	@Autowired
	private TelegramHelpCommandHandlerService helpCommandHandlerService;

	@Autowired
	private TelegramAliveCommandHandlerService aliveCommandHandlerService;

	@Autowired
	private TelegramMeCommandHandlerService meCommandHandlerService;

	@Autowired
	private TelegramSuperAdminCommandHandlerService superAdminCommandHandlerService;

	@Autowired
	private TelegramGroupsCommandHandlerService groupsCommandHandlerService;

	@Autowired
	private TelegramChannelCommandHandlerService channelCommandHandlerService;

	@Autowired
	private TelegramNewRequestHandlerService newRequestHandlerService;

	@Autowired
	private TelegramUpdateRequestHandlerService updateRequestHandlerService;

	@Autowired
	private TelegramBumpRequestHandlerService bumpRequestHandlerService;

	@Autowired
	private TelegramContributorsCommandHandlerService contributorsCommandHandlerService;

	@Autowired
	private TelegramNextAudiobookCommandHandlerService nextAudiobookCommandHandlerService;

	@Autowired
	private TelegramDeleteCommandHandlerService deleteCommandHandlerService;

	@PostConstruct
	public void initialize() {

		// register commands
		registerCommands();

		// help
		bot.register(commandConditionFactory.onCommand(TelegramHelpCommandHandlerService.COMMAND),
				helpCommandHandlerService);

		// alive
		bot.register(commandConditionFactory.onCommands(TelegramAliveCommandHandlerService.COMMANDS, false),
				aliveCommandHandlerService);

		// me
		bot.register(commandConditionFactory.onCommand(TelegramMeCommandHandlerService.COMMAND),
				meCommandHandlerService);

		// super admin
		registerSuperAdminHandlers();

		// groups
		registerGroupsHandlers();

		// channel
		registerChannelHandlers();

		// requests
		registerRequestsHandlers();

		// contributors
		registerContributorsHandlers();

		// remove all commands
		bot.register(deleteCommandHandlerService, deleteCommandHandlerService);

		bot.launch();
	}

	private void registerCommands() {
		TelegramBot telegramBot = bot.getBot();

		// default commands
		BotCommand start = new BotCommand("start", "Checks if the bot is online");
		BotCommand alive = new BotCommand("alive", "Checks if the bot is online");
		BotCommand help = new BotCommand("help", "Shows information on how to use the bot");

		// private, default, admin scopes
		BotCommandScopeDefault defaultScope = new BotCommandScopeDefault();
		BotCommandScopeAllPrivateChats privateChatsScope = new BotCommandScopeAllPrivateChats();

		SetMyCommands commands = new SetMyCommands(start, alive, help);
		commands.scope(defaultScope);
		telegramBot.execute(commands);

		commands.scope(privateChatsScope);
		telegramBot.execute(commands);

		BotCommandScopeAllChatAdministrators allChatAdministratorsScope = new BotCommandScopeAllChatAdministrators();
		commands.scope(allChatAdministratorsScope);

		telegramBot.execute(commands);

		// no commands in groups
		BotCommandScopeAllGroupChats allGroupChatsScope = new BotCommandScopeAllGroupChats();
		commands = new SetMyCommands();
		commands.scope(allGroupChatsScope);

		telegramBot.execute(commands);
	}

	private void registerSuperAdminHandlers() {
		TelegramCondition superAdminChatCondition = chatConditionFactory.onChatType(Type.Private);
		TelegramCondition superAdminRoleCondition = roleConditionFactory
				.onRole(TelegramSuperAdminCommandHandlerService.ROLE);

		// main command
		bot.register(Arrays.asList(superAdminChatCondition,
				commandConditionFactory.onCommand(TelegramSuperAdminCommandHandlerService.COMMAND),
				superAdminRoleCondition), superAdminCommandHandlerService.showActions());

		// list
		bot.register(
				Arrays.asList(
						callbackQueryConditionFactory.onCallbackQuery(
								TelegramSuperAdminCommandHandlerService.COMMAND_LIST, Condition.STARTS_WITH),
						superAdminRoleCondition),
				superAdminCommandHandlerService.listUsers());
		bot.register(
				Arrays.asList(
						callbackQueryConditionFactory.onCallbackQuery(
								TelegramSuperAdminCommandHandlerService.COMMAND_COPY, Condition.STARTS_WITH),
						superAdminRoleCondition),
				superAdminCommandHandlerService.copyUser());

		// add
		bot.register(
				Arrays.asList(
						callbackQueryConditionFactory
								.onCallbackQuery(TelegramSuperAdminCommandHandlerService.COMMAND_ADD, Condition.EQUALS),
						superAdminRoleCondition),
				superAdminCommandHandlerService.addUserHelp());
		bot.register(Arrays.asList(
				replyToCommandConditionFactory.onCommand(TelegramSuperAdminCommandHandlerService.COMMAND_ADD),
				superAdminRoleCondition), superAdminCommandHandlerService.addUser());
		bot.register(
				Arrays.asList(commandConditionFactory.onCommand(TelegramSuperAdminCommandHandlerService.COMMAND_ADD),
						superAdminRoleCondition),
				superAdminCommandHandlerService.addUser());

		// remove
		bot.register(
				Arrays.asList(
						callbackQueryConditionFactory.onCallbackQuery(
								TelegramSuperAdminCommandHandlerService.COMMAND_REMOVE, Condition.EQUALS),
						superAdminRoleCondition),
				superAdminCommandHandlerService.removeUserHelp());
		bot.register(
				Arrays.asList(replyToCommandConditionFactory
						.onCommand(TelegramSuperAdminCommandHandlerService.COMMAND_REMOVE), superAdminRoleCondition),
				superAdminCommandHandlerService.removeUser());
		bot.register(
				Arrays.asList(commandConditionFactory.onCommand(TelegramSuperAdminCommandHandlerService.COMMAND_REMOVE),
						superAdminRoleCondition),
				superAdminCommandHandlerService.removeUser());

	}

	private void registerGroupsHandlers() {
		TelegramCondition groupChatCondition = chatConditionFactory
				.onChatTypes(Arrays.asList(Type.group, Type.supergroup));
		TelegramCondition groupRoleCondition = roleConditionFactory.onRole(TelegramGroupsCommandHandlerService.ROLE);

		bot.register(Arrays.asList(groupChatCondition,
				commandConditionFactory.onCommand(TelegramGroupsCommandHandlerService.COMMAND_INFO),
				groupRoleCondition), groupsCommandHandlerService.showInfo());

		bot.register(Arrays.asList(groupChatCondition,
				commandConditionFactory.onCommand(TelegramGroupsCommandHandlerService.COMMAND_ENABLE),
				groupRoleCondition), groupsCommandHandlerService.enableGroup());

		bot.register(Arrays.asList(groupChatCondition,
				commandConditionFactory.onCommand(TelegramGroupsCommandHandlerService.COMMAND_DISABLE),
				groupRoleCondition), groupsCommandHandlerService.disableGroup());

		bot.register(Arrays.asList(groupChatCondition,
				commandConditionFactory.onCommand(TelegramGroupsCommandHandlerService.COMMAND_REQUEST_LIMIT),
				groupRoleCondition), groupsCommandHandlerService.updateRequestLimit());

		bot.register(Arrays.asList(groupChatCondition,
				commandConditionFactory
						.onCommand(TelegramGroupsCommandHandlerService.COMMAND_NONENGLISH_AUDIOBOOK_DAYS_WAIT),
				groupRoleCondition), groupsCommandHandlerService.updateAudiobooksDaysWait());

		bot.register(Arrays.asList(groupChatCondition,
				commandConditionFactory.onCommand(
						TelegramGroupsCommandHandlerService.COMMAND_ENGLISH_AUDIOBOOK_DAYS_WAIT),
				groupRoleCondition), groupsCommandHandlerService.updateEnglishAudiobooksDaysWait());

		bot.register(Arrays.asList(groupChatCondition,
				commandConditionFactory.onCommand(TelegramGroupsCommandHandlerService.COMMAND_ALLOW),
				groupRoleCondition), groupsCommandHandlerService.updateAllow());

		bot.register(Arrays.asList(groupChatCondition,
				commandConditionFactory.onCommand(TelegramGroupsCommandHandlerService.COMMAND_NO_REPEAT),
				groupRoleCondition), groupsCommandHandlerService.updateNoRepeat());
	}

	private void registerChannelHandlers() {
		TelegramCondition privateChatCondition = chatConditionFactory.onChatTypes(Arrays.asList(Type.Private));
		TelegramCondition channelChatCondition = chatConditionFactory
				.onChatTypes(Arrays.asList(Type.group, Type.supergroup, Type.channel));
		TelegramCondition channelRoleCondition = roleConditionFactory.onRole(TelegramChannelCommandHandlerService.ROLE);

		bot.register(
				Arrays.asList(channelChatCondition,
						commandConditionFactory.onCommand(TelegramChannelCommandHandlerService.COMMAND_CHANNEL_ID)),
				channelCommandHandlerService.getId());

		bot.register(Arrays.asList(privateChatCondition,
				commandConditionFactory.onCommand(TelegramChannelCommandHandlerService.COMMAND_DISABLE),
				channelRoleCondition), channelCommandHandlerService.disableChannel());

		bot.register(Arrays.asList(privateChatCondition,
				commandConditionFactory.onCommand(TelegramChannelCommandHandlerService.COMMAND_CONFIGURE_LIST),
				channelRoleCondition), channelCommandHandlerService.configureChannels());

		bot.register(Arrays.asList(privateChatCondition,
				commandConditionFactory.onCommand(TelegramChannelCommandHandlerService.COMMAND_CONFIGURE),
				channelRoleCondition), channelCommandHandlerService.startConfiguration());

		bot.register(Arrays.asList(
				privateChatCondition, callbackQueryConditionFactory
						.onCallbackQuery(TelegramChannelCommandHandlerService.COMMAND_CONFIGURE, Condition.STARTS_WITH),
				channelRoleCondition), channelCommandHandlerService.configuration());

		bot.register(Arrays.asList(privateChatCondition,
				commandConditionFactory.onCommand(TelegramChannelCommandHandlerService.COMMAND_REFRESH),
				channelRoleCondition), channelCommandHandlerService.refreshChannel());
	}

	private void registerRequestsHandlers() {
		bot.register(newRequestHandlerService.getCondition(), newRequestHandlerService);
		bot.register(updateRequestHandlerService.getCondition(), updateRequestHandlerService);
		bot.register(bumpRequestHandlerService.getCondition(), bumpRequestHandlerService);
	}

	private void registerContributorsHandlers() {
		TelegramCondition groupAndPrivateChatCondition = chatConditionFactory
				.onChatTypes(Arrays.asList(Type.group, Type.supergroup, Type.Private));
		TelegramCondition groupChatCondition = chatConditionFactory
				.onChatTypes(Arrays.asList(Type.group, Type.supergroup));
		TelegramCondition contributorRoleCondition = roleConditionFactory
				.onRole(TelegramContributorsCommandHandlerService.ROLE);

		// done
		bot.register(Arrays.asList(groupChatCondition,
				contributorsCommandHandlerService.replyToMessageWithFileCondition(), contributorRoleCondition),
				contributorsCommandHandlerService.markDoneWithFile());

		bot.register(
				Arrays.asList(groupChatCondition,
						commandConditionFactory.onCommand(TelegramContributorsCommandHandlerService.COMMAND_DONE),
						contributorsCommandHandlerService.replyToMessageCondition(), contributorRoleCondition),
				contributorsCommandHandlerService.markDone());
		bot.register(
				Arrays.asList(groupChatCondition,
						commandConditionFactory
								.onCommand(TelegramContributorsCommandHandlerService.COMMAND_SILENT_DONE),
						contributorsCommandHandlerService.replyToMessageCondition(), contributorRoleCondition),
				contributorsCommandHandlerService.markDoneSilently());

		// requests
		bot.register(Arrays.asList(groupAndPrivateChatCondition,
				commandConditionFactory.onCommand(TelegramContributorsCommandHandlerService.COMMAND_REQUESTS),
				contributorRoleCondition), contributorsCommandHandlerService.findRequests());

		// request actions
		bot.register(
				Arrays.asList(chatConditionFactory.onChatType(Type.Private),
						contributorsCommandHandlerService.showRequestWithActionCondition(), contributorRoleCondition),
				contributorsCommandHandlerService.showRequestWithAction());

		bot.register(Arrays.asList(
				callbackQueryConditionFactory
						.onCallbackQuery(TelegramContributorsCommandHandlerService.CONFIRM_CALLBACK, Condition.MATCHES),
				contributorRoleCondition), contributorsCommandHandlerService.confirmAction());

		bot.register(Arrays.asList(
				callbackQueryConditionFactory.onCallbackQuery(
						TelegramContributorsCommandHandlerService.CHANGE_STATUS_CALLBACK, Condition.MATCHES),
				contributorRoleCondition), contributorsCommandHandlerService.changeStatusWithCallback());

		// show
		bot.register(Arrays.asList(groupChatCondition,
				commandConditionFactory.onCommand(TelegramContributorsCommandHandlerService.COMMAND_SHOW),
				contributorRoleCondition), contributorsCommandHandlerService.showRequest());

		// penging
		bot.register(
				Arrays.asList(groupChatCondition,
						commandConditionFactory.onCommand(TelegramContributorsCommandHandlerService.COMMAND_PENDING),
						contributorsCommandHandlerService.replyToMessageCondition(), contributorRoleCondition),
				contributorsCommandHandlerService.markPending());

		// pause
		bot.register(
				Arrays.asList(groupChatCondition,
						commandConditionFactory.onCommand(TelegramContributorsCommandHandlerService.COMMAND_PAUSE),
						contributorsCommandHandlerService.replyToMessageCondition(), contributorRoleCondition),
				contributorsCommandHandlerService.markPaused());

		// penging
		bot.register(
				Arrays.asList(groupChatCondition,
						commandConditionFactory
								.onCommand(TelegramContributorsCommandHandlerService.COMMAND_IN_PROGRESS),
						contributorsCommandHandlerService.replyToMessageCondition(), contributorRoleCondition),
				contributorsCommandHandlerService.markInProgress());

		// cancel
		bot.register(Arrays.asList(groupChatCondition,
				commandConditionFactory.onCommand(TelegramContributorsCommandHandlerService.COMMAND_CANCEL),
				contributorRoleCondition), contributorsCommandHandlerService.markCancelled());
		// remove
		bot.register(Arrays.asList(groupChatCondition,
				commandConditionFactory.onCommand(TelegramContributorsCommandHandlerService.COMMAND_REMOVE),
				contributorRoleCondition), contributorsCommandHandlerService.removeRequest());

		// next audiobook
		bot.register(
				Arrays.asList(groupChatCondition,
						commandConditionFactory.onCommand(TelegramNextAudiobookCommandHandlerService.COMMAND),
						nextAudiobookCommandHandlerService, contributorRoleCondition),
				nextAudiobookCommandHandlerService);
	}

}
