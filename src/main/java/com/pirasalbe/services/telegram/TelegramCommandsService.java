package com.pirasalbe.services.telegram;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.pengrad.telegrambot.model.botcommandscope.BotCommandsScopeChatMember;
import com.pengrad.telegrambot.request.SetMyCommands;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pirasalbe.models.UserRole;
import com.pirasalbe.models.database.Admin;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.services.AdminService;
import com.pirasalbe.services.GroupService;
import com.pirasalbe.services.SchedulerService;
import com.pirasalbe.utils.TelegramUtils;

/**
 * Service to manage the commands logic
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramCommandsService {

	private static final Logger LOGGER = LoggerFactory.getLogger(TelegramCommandsService.class);

	@Autowired
	private TelegramBotService bot;

	@Autowired
	private AdminService adminService;

	@Autowired
	private GroupService groupService;

	@Autowired
	private SchedulerService schedulerService;

	private SetMyCommands userCommandsPM;
	private SetMyCommands userCommandsGroup;

	private SetMyCommands contributorsCommandsPM;
	private SetMyCommands contributorsCommandsGroup;

	private SetMyCommands managersCommandsPM;
	private SetMyCommands managersCommandsGroup;

	private SetMyCommands adminsCommandsPM;

	public TelegramCommandsService() {
		/*
		 * Default commands
		 */
		BotCommand start = new BotCommand("start", "Checks if the bot is online");
		BotCommand alive = new BotCommand("alive", "Checks if the bot is online");
		BotCommand help = new BotCommand("help", "Shows information on how to use the bot");

		BotCommand me = new BotCommand("me", "Shows user info");
		BotCommand myRequests = new BotCommand("my_requests", "Shows open/fulfilled requests");

		userCommandsPM = new SetMyCommands(start, alive, help, me, myRequests);
		userCommandsGroup = new SetMyCommands(me);

		/*
		 * Contributors commands
		 */
		BotCommand them = new BotCommand("them", "Shows another user info");

		BotCommand requests = new BotCommand("requests", "See the requests. Supports filters.");

		BotCommand show = new BotCommand("show", "Shows a request.");

		BotCommand done = new BotCommand("done",
				"Marks the request as done and the bot replies to the request with [text].");
		BotCommand sdone = new BotCommand("sdone", "Marks the request as done.");

		BotCommand pending = new BotCommand("pending", "Marks a request as pending.");
		BotCommand acceptRequest = new BotCommand("accept_request", "Accept a manual request.");
		BotCommand pause = new BotCommand("pause", "Marks a request as paused.");
		BotCommand inProgress = new BotCommand("in_progress", "Marks a request as in progress.");

		BotCommand cancel = new BotCommand("cancel", "Removes a request from the list of the pending requests.");
		BotCommand remove = new BotCommand("remove", "Deletes a request.");

		BotCommand nextAudiobook = new BotCommand("next_audiobook",
				"Force a user to wait [days] days before requesting a new audiobook.");

		BotCommand configureChannel = new BotCommand("configure_channel", "Starts the configuration process");
		BotCommand disableChannel = new BotCommand("disable_channel", "Disables channel forwarding.");
		BotCommand configureChannels = new BotCommand("configure_channels",
				"Shows the channels enabled and allows the configuration.");
		BotCommand refreshChannel = new BotCommand("refresh_channel",
				"Checks all requests in the database and sync them with the one in the channel.");

		BotCommand stats = new BotCommand("stats", "Get statistics.");
		BotCommand trending = new BotCommand("trending", "Get trending requests.");

		contributorsCommandsPM = new SetMyCommands(start, alive, help, me, myRequests, requests, configureChannel,
				disableChannel, configureChannels, refreshChannel, stats, trending);
		contributorsCommandsGroup = new SetMyCommands(start, alive, help, me, them, requests, show, done, sdone,
				pending, acceptRequest, pause, inProgress, cancel, remove, nextAudiobook);

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
		BotCommand repeatHoursWait = new BotCommand("/repeat_hours_wait",
				"Define the hours to wait before repeating a request to [number of hours to wait].");
		BotCommand allow = new BotCommand("allow", "Define what can be requested between ebooks, audiobooks, both.");
		BotCommand noRepeat = new BotCommand("no_repeat",
				"Define the tags whose requests can't be repeated. It accepts a list of sources.");

		managersCommandsPM = new SetMyCommands(start, alive, help, me, myRequests, requests, configureChannel,
				disableChannel, configureChannels, refreshChannel, stats, trending);
		managersCommandsGroup = new SetMyCommands(start, alive, help, me, them, groupInfo, enableGroup, disableGroup,
				requestLimit, nonenglishAudiobooksDaysWait, englishAudiobooksDaysWait, repeatHoursWait, allow, noRepeat,
				requests, show, done, sdone, pending, acceptRequest, pause, inProgress, cancel, remove, nextAudiobook);

		/*
		 * Admin commands
		 */
		BotCommand admins = new BotCommand("admins", "Show commands to manage admins");
		BotCommand adminsAdd = new BotCommand("admins_add", "Add admin");
		BotCommand adminsRemove = new BotCommand("admins_remove", "Remove admin");

		BotCommand info = new BotCommand("info", "Provides bot information");

		adminsCommandsPM = new SetMyCommands(start, alive, info, help, me, myRequests, admins, adminsAdd, adminsRemove,
				requests, configureChannel, disableChannel, configureChannels, refreshChannel, stats, trending);
	}

	public void registerCommandsAsync() {
		schedulerService.schedule(this::registerCommands, 5, TimeUnit.MILLISECONDS);
	}

	private void registerCommands() {
		TelegramBot telegramBot = bot.getBot();

		/**
		 * Definition
		 */

		// private, default, admin scopes
		BotCommandScopeDefault defaultScope = new BotCommandScopeDefault();
		setCommands(telegramBot, userCommandsPM, defaultScope);

		BotCommandScopeAllPrivateChats privateChatsScope = new BotCommandScopeAllPrivateChats();
		setCommands(telegramBot, userCommandsPM, privateChatsScope);

		BotCommandScopeAllChatAdministrators allChatAdministratorsScope = new BotCommandScopeAllChatAdministrators();
		setCommands(telegramBot, userCommandsGroup, allChatAdministratorsScope);

		// no commands in groups
		BotCommandScopeAllGroupChats allGroupChatsScope = new BotCommandScopeAllGroupChats();
		setCommands(telegramBot, userCommandsGroup, allGroupChatsScope);

		// admins
		defineAdminsCommands(telegramBot);
	}

	private void defineAdminsCommands(TelegramBot telegramBot) {
		List<Admin> adminList = adminService.findAll();

		List<Group> groupList = groupService.findAll();

		for (Admin admin : adminList) {
			defineAdminCommands(telegramBot, groupList, admin.getId(), admin.getRole());
		}
	}

	public void defineAdminCommandsAsync(Long adminId) {
		schedulerService.schedule(() -> defineAdminCommands(adminId), 5, TimeUnit.MILLISECONDS);
	}

	private void defineAdminCommands(Long adminId) {
		TelegramBot telegramBot = bot.getBot();

		UserRole role = adminService.getAuthority(adminId);

		if (role.getAuthorityLevel() > UserRole.USER.getAuthorityLevel()) {
			List<Group> groupList = groupService.findAll();

			defineAdminCommands(telegramBot, groupList, adminId, role);
		}
	}

	private void defineAdminCommands(TelegramBot telegramBot, List<Group> groupList, Long adminId, UserRole role) {
		BotCommandsScopeChat botCommandsPmScopeChat = new BotCommandsScopeChat(adminId);

		switch (role) {
		case CONTRIBUTOR:
			setCommands(telegramBot, contributorsCommandsPM, botCommandsPmScopeChat);
			break;
		case MANAGER:
			setCommands(telegramBot, managersCommandsPM, botCommandsPmScopeChat);
			break;
		case SUPERADMIN:
			setCommands(telegramBot, adminsCommandsPM, botCommandsPmScopeChat);
			break;
		default:
			setCommands(telegramBot, userCommandsPM, botCommandsPmScopeChat);
			break;
		}

		for (Group group : groupList) {
			BotCommandsScopeChatMember botCommandsGroupScopeChat = new BotCommandsScopeChatMember(group.getId(),
					adminId);

			switch (role) {
			case CONTRIBUTOR:
				setCommands(telegramBot, contributorsCommandsGroup, botCommandsGroupScopeChat);
				break;
			case MANAGER:
			case SUPERADMIN:
				setCommands(telegramBot, managersCommandsGroup, botCommandsGroupScopeChat);
				break;
			default:
				setCommands(telegramBot, userCommandsGroup, botCommandsGroupScopeChat);
				break;
			}

			TelegramUtils.cooldown();
		}
	}

	private void setCommands(TelegramBot telegramBot, SetMyCommands myCommands, BotCommandScope scope) {
		myCommands.scope(scope);
		BaseResponse response = telegramBot.execute(myCommands);

		if (!response.isOk()) {
			LOGGER.error("Cannot define commands {}: {}", myCommands, response);
		}
	}

}
