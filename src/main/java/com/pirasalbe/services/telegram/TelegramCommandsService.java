package com.pirasalbe.services.telegram;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.model.botcommandscope.BotCommandScope;
import com.pengrad.telegrambot.model.botcommandscope.BotCommandScopeAllChatAdministrators;
import com.pengrad.telegrambot.model.botcommandscope.BotCommandScopeAllGroupChats;
import com.pengrad.telegrambot.model.botcommandscope.BotCommandScopeAllPrivateChats;
import com.pengrad.telegrambot.model.botcommandscope.BotCommandScopeDefault;
import com.pengrad.telegrambot.model.botcommandscope.BotCommandsScopeChat;
import com.pengrad.telegrambot.request.SetMyCommands;
import com.pirasalbe.models.database.Admin;
import com.pirasalbe.services.AdminService;

/**
 * Service to manage the commands logic
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramCommandsService {

	@Autowired
	private TelegramBotService bot;

	@Autowired
	private AdminService adminService;

	public void registerCommands() {
		TelegramBot telegramBot = bot.getBot();

		/*
		 * Default commands
		 */
		BotCommand start = new BotCommand("start", "Checks if the bot is online");
		BotCommand alive = new BotCommand("alive", "Checks if the bot is online");
		BotCommand help = new BotCommand("help", "Shows information on how to use the bot");

		SetMyCommands defaultCommands = new SetMyCommands(start, alive, help);

		/*
		 * Contributors commands
		 */
		BotCommand requests = new BotCommand("requests", "See the requests. Supports filters.");
		// TODO

		/*
		 * Manager commands
		 */
		// group
		BotCommand groupInfo = new BotCommand("group_info", "Shows the group settings");
		BotCommand enableGroup = new BotCommand("enable_group",
				"Enables the group. After this command, the group can be configured and requests will be tracked.");
		BotCommand disableGroup = new BotCommand("disable_group",
				"Disables the group. It removes also all the requests tracked.");

		BotCommand requestLimit = new BotCommand("request_limit",
				"Define the limit of ebook requests per day to [number of requests].");
		BotCommand nonenglishAudiobooksDaysWait = new BotCommand("nonenglish_audiobooks_days_wait",
				"Define the days to wait after the last audiobook requests/received not in English before requesting a new audiobook to [number of days to wait].");
		BotCommand englishAudiobooksDaysWait = new BotCommand("english_audiobooks_days_wait",
				"Define the days to wait after the last English audiobook requests/received before requesting a new audiobook to [number of days to wait].");
		BotCommand allow = new BotCommand("allow", "Define what can be requested between ebooks, audiobooks, both.");
		BotCommand noRepeat = new BotCommand("no_repeat",
				"Define the tags whose requests can't be repeated. It accepts a list of sources.");

		// TODO

		/*
		 * Admin commands
		 */
		BotCommand admins = new BotCommand("admins", "Show commands to manage admins");
		BotCommand adminsAdd = new BotCommand("admins_add", "Add admin");
		BotCommand adminsRemove = new BotCommand("admins_remove", "Remove admin");

		// TODO

		/**
		 * Definition
		 */

		// private, default, admin scopes
		BotCommandScopeDefault defaultScope = new BotCommandScopeDefault();
		setCommands(telegramBot, defaultCommands, defaultScope);

		BotCommandScopeAllPrivateChats privateChatsScope = new BotCommandScopeAllPrivateChats();
		setCommands(telegramBot, defaultCommands, privateChatsScope);

		BotCommandScopeAllChatAdministrators allChatAdministratorsScope = new BotCommandScopeAllChatAdministrators();
		setCommands(telegramBot, defaultCommands, allChatAdministratorsScope);

		// no commands in groups
		BotCommandScopeAllGroupChats allGroupChatsScope = new BotCommandScopeAllGroupChats();
		setCommands(telegramBot, allGroupChatsScope);

		// admins
		List<Admin> adminList = adminService.findAll();

		for (Admin admin : adminList) {
			BotCommandsScopeChat botCommandsScopeChat = new BotCommandsScopeChat(admin.getId());
			setCommands(telegramBot, botCommandsScopeChat);
		}
	}

	private void setCommands(TelegramBot telegramBot, BotCommandScope scope, BotCommand... commands) {
		SetMyCommands myCommands = new SetMyCommands(commands);
		setCommands(telegramBot, myCommands, scope);
	}

	private void setCommands(TelegramBot telegramBot, SetMyCommands myCommands, BotCommandScope scope) {
		myCommands.scope(scope);
		telegramBot.execute(myCommands);
	}

}
