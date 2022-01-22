package com.pirasalbe.services.telegram;

import java.util.Arrays;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.model.Chat.Type;
import com.pirasalbe.models.telegram.handlers.TelegramCondition;
import com.pirasalbe.services.telegram.conditions.TelegramCallbackQueryConditionFactory;
import com.pirasalbe.services.telegram.conditions.TelegramCallbackQueryConditionFactory.Condition;
import com.pirasalbe.services.telegram.conditions.TelegramChatConditionFactory;
import com.pirasalbe.services.telegram.conditions.TelegramCommandConditionFactory;
import com.pirasalbe.services.telegram.conditions.TelegramReplyToCommandConditionFactory;
import com.pirasalbe.services.telegram.conditions.TelegramRoleConditionFactory;
import com.pirasalbe.services.telegram.handlers.command.TelegramAliveCommandHandlerService;
import com.pirasalbe.services.telegram.handlers.command.TelegramGroupsCommandHandlerService;
import com.pirasalbe.services.telegram.handlers.command.TelegramHelpCommandHandlerService;
import com.pirasalbe.services.telegram.handlers.command.TelegramSuperAdminCommandHandlerService;
import com.pirasalbe.services.telegram.handlers.request.TelegramNewRequestHandlerService;

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
	private TelegramSuperAdminCommandHandlerService superAdminCommandHandlerService;

	@Autowired
	private TelegramGroupsCommandHandlerService groupsCommandHandlerService;

	@Autowired
	private TelegramNewRequestHandlerService newRequestHandlerService;

	@PostConstruct
	public void initialize() {

		// help
		bot.register(commandConditionFactory.onCommand(TelegramHelpCommandHandlerService.COMMAND),
				helpCommandHandlerService);

		// alive
		bot.register(commandConditionFactory.onCommands(TelegramAliveCommandHandlerService.COMMANDS),
				aliveCommandHandlerService);

		// super admin
		registerSuperAdminHandlers();

		// groups
		registerGroupsHandlers();

		// requests
		registerRequestsHandlers();

		bot.launch();

	}

	private void registerSuperAdminHandlers() {
		TelegramCondition superAdminChatCondition = chatConditionFactory.onChatType(Type.Private);
		TelegramCondition superAdminRoleCondition = roleConditionFactory
				.onRole(TelegramSuperAdminCommandHandlerService.ROLE);

		// main command
		bot.register(
				Arrays.asList(superAdminChatCondition, superAdminRoleCondition,
						commandConditionFactory.onCommand(TelegramSuperAdminCommandHandlerService.COMMAND)),
				superAdminCommandHandlerService.showActions());

		// list
		bot.register(
				Arrays.asList(superAdminRoleCondition,
						callbackQueryConditionFactory.onCallbackQuery(
								TelegramSuperAdminCommandHandlerService.COMMAND_LIST, Condition.STARTS_WITH)),
				superAdminCommandHandlerService.listUsers());
		bot.register(
				Arrays.asList(superAdminRoleCondition,
						callbackQueryConditionFactory.onCallbackQuery(
								TelegramSuperAdminCommandHandlerService.COMMAND_COPY, Condition.STARTS_WITH)),
				superAdminCommandHandlerService.copyUser());

		// add
		bot.register(
				Arrays.asList(superAdminRoleCondition,
						callbackQueryConditionFactory.onCallbackQuery(
								TelegramSuperAdminCommandHandlerService.COMMAND_ADD, Condition.EQUALS)),
				superAdminCommandHandlerService.addUserHelp());
		bot.register(
				Arrays.asList(superAdminRoleCondition,
						replyToCommandConditionFactory.onCommand(TelegramSuperAdminCommandHandlerService.COMMAND_ADD)),
				superAdminCommandHandlerService.addUser());

		// remove
		bot.register(
				Arrays.asList(superAdminRoleCondition,
						callbackQueryConditionFactory.onCallbackQuery(
								TelegramSuperAdminCommandHandlerService.COMMAND_REMOVE, Condition.EQUALS)),
				superAdminCommandHandlerService.removeUserHelp());
		bot.register(
				Arrays.asList(superAdminRoleCondition,
						replyToCommandConditionFactory
								.onCommand(TelegramSuperAdminCommandHandlerService.COMMAND_REMOVE)),
				superAdminCommandHandlerService.removeUser());

	}

	private void registerGroupsHandlers() {
		TelegramCondition groupChatCondition = chatConditionFactory
				.onChatTypes(Arrays.asList(Type.group, Type.supergroup));
		TelegramCondition groupRoleCondition = roleConditionFactory.onRole(TelegramGroupsCommandHandlerService.ROLE);

		bot.register(
				Arrays.asList(groupChatCondition, groupRoleCondition,
						commandConditionFactory.onCommand(TelegramGroupsCommandHandlerService.COMMAND_INFO)),
				groupsCommandHandlerService.showInfo());

		bot.register(
				Arrays.asList(groupChatCondition, groupRoleCondition,
						commandConditionFactory.onCommand(TelegramGroupsCommandHandlerService.COMMAND_ENABLE)),
				groupsCommandHandlerService.enableGroup());

		bot.register(
				Arrays.asList(groupChatCondition, groupRoleCondition,
						commandConditionFactory.onCommand(TelegramGroupsCommandHandlerService.COMMAND_DISABLE)),
				groupsCommandHandlerService.disableGroup());

		bot.register(
				Arrays.asList(groupChatCondition, groupRoleCondition,
						commandConditionFactory.onCommand(TelegramGroupsCommandHandlerService.COMMAND_REQUEST_LIMIT)),
				groupsCommandHandlerService.updateRequestLimit());

		bot.register(
				Arrays.asList(groupChatCondition, groupRoleCondition,
						commandConditionFactory
								.onCommand(TelegramGroupsCommandHandlerService.COMMAND_AUDIOBOOK_DAYS_WAIT)),
				groupsCommandHandlerService.updateAudiobooksDaysWait());

		bot.register(
				Arrays.asList(groupChatCondition, groupRoleCondition,
						commandConditionFactory
								.onCommand(TelegramGroupsCommandHandlerService.COMMAND_ENGLISH_AUDIOBOOK_DAYS_WAIT)),
				groupsCommandHandlerService.updateEnglishAudiobooksDaysWait());

		bot.register(
				Arrays.asList(groupChatCondition, groupRoleCondition,
						commandConditionFactory.onCommand(TelegramGroupsCommandHandlerService.COMMAND_ALLOW)),
				groupsCommandHandlerService.updateAllow());
	}

	private void registerRequestsHandlers() {
		bot.register(newRequestHandlerService.geCondition(), newRequestHandlerService);

	}

}
