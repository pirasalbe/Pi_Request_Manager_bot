package com.pirasalbe.models;

/**
 * Model of a message to log
 *
 * @author pirasalbe
 *
 */
public class LogEventMessage {

	private String content;

	private Integer messageId;

	public LogEventMessage(Integer messageId, String content) {
		super();
		this.content = content;
		this.messageId = messageId;
	}

	public String getContent() {
		return content;
	}

	public Integer getMessageId() {
		return messageId;
	}

}
