package com.pirasalbe.services.telegram;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.springframework.stereotype.Component;

import com.pirasalbe.configurations.TelegramConfiguration;
import com.pirasalbe.models.exceptions.UserBotException;
import com.pirasalbe.utils.TelegramUtils;

import it.tdlight.client.APIToken;
import it.tdlight.client.AuthenticationData;
import it.tdlight.client.Result;
import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.client.TDLibSettings;
import it.tdlight.common.Init;
import it.tdlight.common.utils.CantLoadLibrary;
import it.tdlight.jni.TdApi;
import it.tdlight.jni.TdApi.GetMessageLinkInfo;
import it.tdlight.jni.TdApi.MessageLinkInfo;

/**
 * Telegram Service for the user bot
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramUserBotService {

	private static final String USERBOT_PATH = "userbot-data";

	private SimpleTelegramClient client;

	public SimpleTelegramClient getClient() {
		return client;
	}

	public TelegramUserBotService(TelegramConfiguration configuration) throws InterruptedException, CantLoadLibrary {
		// Initialize TDLight native libraries
		Init.start();

		// Obtain the API token
		APIToken apiToken = new APIToken(configuration.getApiId(), configuration.getApiHash());

		// Configure the client
		TDLibSettings settings = TDLibSettings.create(apiToken);

		// Configure the session directory
		Path sessionPath = Paths.get(USERBOT_PATH);
		settings.setDatabaseDirectoryPath(sessionPath.resolve("data"));
		settings.setDownloadedFilesDirectoryPath(sessionPath.resolve("downloads"));

		// Create a client
		client = new SimpleTelegramClient(settings);

		// Configure the authentication info
		AuthenticationData authenticationData = AuthenticationData.user(configuration.getNumber());

		// Start the client
		client.start(authenticationData);
	}

	/**
	 * Execute method async
	 *
	 * @param <R>      Result type
	 * @param function Method to execute
	 * @return Future
	 */
	public <R extends TdApi.Object> Future<Result<R>> sendAsync(TdApi.Function<R> function) {
		CompletableFuture<Result<R>> completableFuture = new CompletableFuture<>();

		client.send(function, completableFuture::complete);

		return completableFuture;
	}

	/**
	 * Execute method sync
	 *
	 * @param <R>      Result type
	 * @param function Method to execute
	 * @return Result of R
	 */
	public <R extends TdApi.Object> Result<R> sendSync(TdApi.Function<R> function) {
		try {
			return sendAsync(function).get();
		} catch (InterruptedException | ExecutionException e) {
			throw new UserBotException(e);
		}
	}

	/**
	 * Get tdlib message id
	 *
	 * @param groupId   Group Id
	 * @param messageId Bot message id
	 * @return tdlib message id
	 */
	public Result<MessageLinkInfo> getMessageId(Long groupId, Long messageId) {
		return sendSync(new GetMessageLinkInfo(TelegramUtils.getLink(groupId, messageId)));
	}
}