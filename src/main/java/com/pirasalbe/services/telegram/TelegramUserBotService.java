package com.pirasalbe.services.telegram;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.pirasalbe.configurations.TelegramConfiguration;
import com.pirasalbe.models.exceptions.UserBotException;
import com.pirasalbe.utils.TelegramUtils;

import it.tdlight.Init;
import it.tdlight.client.APIToken;
import it.tdlight.client.AuthenticationSupplier;
import it.tdlight.client.Result;
import it.tdlight.client.SimpleAuthenticationSupplier;
import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.client.SimpleTelegramClientBuilder;
import it.tdlight.client.SimpleTelegramClientFactory;
import it.tdlight.client.TDLibSettings;
import it.tdlight.jni.TdApi;
import it.tdlight.jni.TdApi.GetMessageLinkInfo;
import it.tdlight.jni.TdApi.MessageLinkInfo;
import it.tdlight.util.UnsupportedNativeLibraryException;

/**
 * Telegram Service for the user bot
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramUserBotService implements AutoCloseable {

	private static final Logger LOGGER = LoggerFactory.getLogger(TelegramLogService.class);

	private static final String USERBOT_PATH = "userbot-data";

	private static SimpleTelegramClientFactory clientFactory;
	private static SimpleTelegramClient client;

	public TelegramUserBotService(TelegramConfiguration configuration)
			throws InterruptedException, UnsupportedNativeLibraryException {
		// Initialize TDLight native libraries
		Init.init();

		clientFactory = new SimpleTelegramClientFactory();

		// Obtain the API token
		APIToken apiToken = new APIToken(configuration.getApiId(), configuration.getApiHash());

		// Configure the client
		TDLibSettings settings = TDLibSettings.create(apiToken);

		// Configure the session directory
		Path sessionPath = Paths.get(USERBOT_PATH);
		settings.setDatabaseDirectoryPath(sessionPath.resolve("data"));
		settings.setDownloadedFilesDirectoryPath(sessionPath.resolve("downloads"));

		// Create a client
		SimpleTelegramClientBuilder clientBuilder = clientFactory.builder(settings);

		// Configure the authentication info
		SimpleAuthenticationSupplier<?> authenticationData = AuthenticationSupplier.user(configuration.getNumber());

		ExecutorService executorService = Executors.newSingleThreadExecutor();
		executorService.execute(() -> {
			client = clientBuilder.build(authenticationData);
			LOGGER.info("User bot initialized");
		});
	}

	@Override
	public void close() throws Exception {
		client.close();
		clientFactory.close();
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