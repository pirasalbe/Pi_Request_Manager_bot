package com.pirasalbe.services;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendDocument;
import com.pengrad.telegrambot.response.SendResponse;
import com.pirasalbe.configurations.TelegramConfiguration;
import com.pirasalbe.models.LogEvent;
import com.pirasalbe.models.database.Admin;
import com.pirasalbe.models.database.Channel;
import com.pirasalbe.models.database.ChannelRequest;
import com.pirasalbe.models.database.ChannelRule;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.database.Request;
import com.pirasalbe.services.channels.ChannelRequestService;
import com.pirasalbe.services.channels.ChannelRuleService;
import com.pirasalbe.services.channels.ChannelService;
import com.pirasalbe.services.telegram.TelegramBotService;
import com.pirasalbe.services.telegram.TelegramLogService;
import com.pirasalbe.utils.DateUtils;

/**
 * Service that retrieve all data and sends backups
 *
 * @author pirasalbe
 *
 */
@Component
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public class BackupService {

	private static final Logger LOGGER = LoggerFactory.getLogger(BackupService.class);

	private TelegramBot bot;

	@Autowired
	private TelegramLogService logService;

	@Autowired
	private TelegramConfiguration configuration;

	@Autowired
	private AdminService adminService;

	@Autowired
	private GroupService groupService;

	@Autowired
	private ChannelService channelService;

	@Autowired
	private ChannelRuleService channelRuleService;

	@Autowired
	private RequestManagementService requestManagementService;

	@Autowired
	private ChannelRequestService channelRequestService;

	public BackupService(TelegramBotService telegramBotService) {
		this.bot = telegramBotService.getBot();
	}

	@Scheduled(cron = "0 0 0 * * ?")
	public void sendBackup() {
		sendAdminsBackup();
		sendGroupsBackup();
		sendChannelsBackup();
		sendChannelRulesBackup();
		sendRequestsBackup();
		sendChannelRequestsBackup();
	}

	private void sendAdminsBackup() {
		List<Admin> admins = adminService.findAll();

		StringBuilder builder = new StringBuilder();
		builder.append("ID,NAME,ROLE\n");

		for (Admin admin : admins) {
			builder.append(admin.getId());
			builder.append(",");
			builder.append(admin.getName());
			builder.append(",");
			builder.append(admin.getRole());
			builder.append("\n");
		}

		sendBackup("Admins", builder.toString());
	}

	private void sendGroupsBackup() {
		List<Group> groups = groupService.findAll();

		StringBuilder builder = new StringBuilder();
		builder.append(
				"ID;NAME;REQUEST_LIMIT;ENGLISH_AUDIOBOOKS;NONENGLISH_AUDIOBOOKS;NO_REPEAT;REPEAT_HOURS_WAIT;ALLOW_EBOOKS;ALLOW_AUDIOBOOKS\n");

		for (Group group : groups) {
			builder.append(group.getId());
			builder.append(";");
			builder.append(group.getName());
			builder.append(";");
			builder.append(group.getRequestLimit());
			builder.append(";");
			builder.append(group.getEnglishAudiobooksDaysWait());
			builder.append(";");
			builder.append(group.getAudiobooksDaysWait());
			builder.append(";");
			builder.append(group.getNoRepeat());
			builder.append(";");
			builder.append(group.getRepeatHoursWait());
			builder.append(";");
			builder.append(group.isAllowEbooks());
			builder.append(";");
			builder.append(group.isAllowAudiobooks());
			builder.append("\n");
		}

		sendBackup("Groups", builder.toString());
	}

	private void sendChannelsBackup() {
		List<Channel> channels = channelService.findAll();

		StringBuilder builder = new StringBuilder();
		builder.append("ID,NAME\n");

		for (Channel channel : channels) {
			builder.append(channel.getId());
			builder.append(",");
			builder.append(channel.getName());
			builder.append("\n");
		}

		sendBackup("Channels", builder.toString());
	}

	private void sendChannelRulesBackup() {
		List<ChannelRule> rules = channelRuleService.findAll();

		StringBuilder builder = new StringBuilder();
		builder.append("CHANNEL_ID,TYPE,VALUE\n");

		for (ChannelRule rule : rules) {
			builder.append(rule.getId().getChannelId());
			builder.append(",");
			builder.append(rule.getId().getType());
			builder.append(",");
			builder.append(rule.getId().getValue());
			builder.append("\n");
		}

		sendBackup("Channel Rules", builder.toString());
	}

	private void sendRequestsBackup() {

		StringBuilder builder = new StringBuilder();
		builder.append(
				"GROUP_ID,MESSAGE_ID,STATUS,CONTENT,LINK,FORMAT,SOURCE,OTHER_TAGS,USER_ID,REQUEST_DATE,REPETITIONS,RESOLVED_DATE,RESOLVED_MESSAGE_ID,CONTRIBUTOR\n");

		int page = 0;
		int size = 100;
		boolean keep = true;

		// get all requests paginated
		while (keep) {
			Page<Request> requestPage = requestManagementService.findAll(page, size);

			// forward all requests
			List<Request> requests = requestPage.toList();

			for (Request request : requests) {
				builder.append(request.getId().getGroupId());
				builder.append(",");
				builder.append(request.getId().getMessageId());
				builder.append(",");
				builder.append(request.getStatus());
				builder.append(",");
				builder.append(request.getContent().replace("\n", "\\n"));
				builder.append(",");
				builder.append(request.getLink());
				builder.append(",");
				builder.append(request.getFormat());
				builder.append(",");
				builder.append(request.getSource());
				builder.append(",");
				builder.append(request.getOtherTags());
				builder.append(",");
				builder.append(request.getUserId());
				builder.append(",");
				builder.append(request.getRequestDate());
				builder.append(",");
				builder.append(request.getRepetitions());
				builder.append(",");
				builder.append(request.getResolvedDate());
				builder.append(",");
				builder.append(request.getResolvedMessageId());
				builder.append(",");
				builder.append(request.getContributor());
				builder.append("\n");
			}

			keep = requestPage.hasNext();
			page++;
		}

		sendBackup("Requests", builder.toString());
	}

	private void sendChannelRequestsBackup() {

		StringBuilder builder = new StringBuilder();
		builder.append("CHANNEL_ID,MESSAGE_ID,REQUEST_GROUP_ID,REQUEST_MESSAGE_ID\n");

		int page = 0;
		int size = 100;
		boolean keep = true;

		// get all requests paginated
		while (keep) {
			Page<ChannelRequest> channelRequestPage = channelRequestService.findAll(page, size);

			// forward all requests
			List<ChannelRequest> channelRequests = channelRequestPage.toList();

			for (ChannelRequest channelRequest : channelRequests) {
				builder.append(channelRequest.getId().getChannelId());
				builder.append(",");
				builder.append(channelRequest.getId().getMessageId());
				builder.append(",");
				builder.append(channelRequest.getRequestGroupId());
				builder.append(",");
				builder.append(channelRequest.getRequestMessageId());
				builder.append("\n");
			}

			keep = channelRequestPage.hasNext();
			page++;
		}

		sendBackup("Channel Requests", builder.toString());
	}

	private void sendBackup(String name, String content) {
		SendDocument sendDocument = new SendDocument(configuration.getBackupChat(), content.getBytes());
		sendDocument.fileName(name + ".csv");
		sendDocument.caption(DateUtils.formatDate(DateUtils.getNow()));
		SendResponse execute = bot.execute(sendDocument);

		if (!execute.isOk() && LOGGER.isErrorEnabled()) {
			StringBuilder builder = new StringBuilder();
			builder.append("Could not send backup [").append(name).append("] with error: ");
			builder.append(execute.errorCode()).append(" - ").append(execute.description());
			String reason = builder.toString();

			LOGGER.error(reason);

			logService.log(new LogEvent(reason));
		}
	}
}
