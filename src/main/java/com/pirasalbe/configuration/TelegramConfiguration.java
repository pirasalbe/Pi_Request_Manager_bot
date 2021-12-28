package com.pirasalbe.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bot")
public class TelegramConfiguration {

	private String token;

	public String getToken() {
		return token;
	}

}
