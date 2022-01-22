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
import com.pirasalbe.services.telegram.handlers.command.TelegramHelpCommandHandlerService;
import com.pirasalbe.services.telegram.handlers.command.TelegramSuperAdminCommandHandlerService;

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

		// TODO add handlers

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

}
