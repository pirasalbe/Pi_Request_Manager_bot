package com.pirasalbe.configurations;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "error")
public class ErrorConfiguration {

	private String incompleteRequest;

	private String nonBotRequest;

	private String quoteBotRequest;

	public String getIncompleteRequest() {
		return incompleteRequest;
	}

	public void setIncompleteRequest(String incompleteRequest) {
		this.incompleteRequest = incompleteRequest;
	}

	public String getNonBotRequest() {
		return nonBotRequest;
	}

	public void setNonBotRequest(String nonBotRequest) {
		this.nonBotRequest = nonBotRequest;
	}

	public String getQuoteBotRequest() {
		return quoteBotRequest;
	}

	public void setQuoteBotRequest(String quoteBotRequest) {
		this.quoteBotRequest = quoteBotRequest;
	}

}
