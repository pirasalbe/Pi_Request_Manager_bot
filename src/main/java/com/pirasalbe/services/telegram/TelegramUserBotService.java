package com.pirasalbe.services.telegram;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Component;

import com.pirasalbe.configurations.TelegramConfiguration;

import it.tdlight.client.APIToken;
import it.tdlight.client.AuthenticationData;
import it.tdlight.client.GenericResultHandler;
import it.tdlight.client.SimpleTelegramClient;
import it.tdlight.client.TDLibSettings;
import it.tdlight.common.Init;
import it.tdlight.common.utils.CantLoadLibrary;
import it.tdlight.jni.TdApi;

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

	public <R extends TdApi.Object> void send(TdApi.Function<R> function, GenericResultHandler<R> resultHandler) {
		client.send(function, resultHandler);
	}

}