package com.pirasalbe.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "bot")
public class TelegramConfiguration {

	private String username;

	private String token;

	private Long number;

	private Integer apiId;

	private String apiHash;

	private Long backupChat;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public Long getNumber() {
		return number;
	}

	public void setNumber(Long number) {
		this.number = number;
	}

	public String getToken() {
		return token;
	}

	public Integer getApiId() {
		return apiId;
	}

	public void setApiId(Integer apiId) {
		this.apiId = apiId;
	}

	public String getApiHash() {
		return apiHash;
	}

	public void setApiHash(String apiHash) {
		this.apiHash = apiHash;
	}

	public Long getBackupChat() {
		return backupChat;
	}

	public void setBackupChat(Long backupChat) {
		this.backupChat = backupChat;
	}

}
