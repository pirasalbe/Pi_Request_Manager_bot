package com.pirasalbe.services.telegram.handlers.command;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.models.telegram.handlers.TelegramHandler;

/**
 * Service to manage /alive and /start
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramInfoCommandHandlerService implements TelegramHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(TelegramInfoCommandHandlerService.class);

	public static final String COMMAND = "/info";

	@Override
	public void handle(TelegramBot bot, Update update) {
		Long chatId = update.message().chat().id();

		StringBuilder message = new StringBuilder();
		message.append("<b>Bot Info</b>\n\n");
		message.append("Addresses:\n").append(getIpAddresses());

		SendMessage sendMessage = new SendMessage(chatId, message.toString());
		sendMessage.parseMode(ParseMode.HTML);

		bot.execute(sendMessage);
	}

	private String getIpAddresses() {
		StringBuilder builder = new StringBuilder();

		Enumeration<NetworkInterface> networkIntefaces;
		try {
			networkIntefaces = NetworkInterface.getNetworkInterfaces();
			while (networkIntefaces.hasMoreElements()) {
				NetworkInterface networkInterface = networkIntefaces.nextElement();
				Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
				while (inetAddresses.hasMoreElements()) {
					InetAddress inetAddress = inetAddresses.nextElement();
					builder.append("<code>").append(inetAddress.getHostAddress()).append("</code>\n");
				}
			}
		} catch (SocketException e) {
			LOGGER.error("Cannot obtain IP addresses", e);
		}

		return builder.toString();
	}

}
