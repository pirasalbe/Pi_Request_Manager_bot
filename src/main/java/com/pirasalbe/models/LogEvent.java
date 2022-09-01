package com.pirasalbe.models;

import java.time.LocalDateTime;

/**
 * Model of a log event
 *
 * @author pirasalbe
 *
 */
public class LogEvent {

	private LocalDateTime timestamp;

	private Long userId;

	private Long groupId;

	private LogEventMessage originalMessage;

	private String reason;

	public LogEvent(String reason) {
		this(null, null, reason);
	}

	public LogEvent(LogEventMessage originalMessage, String reason) {
		this(null, null, originalMessage, reason);
	}

	public LogEvent(Long userId, Long groupId, String reason) {
		this(userId, groupId, null, reason);
	}

	public LogEvent(Long userId, Long groupId, LogEventMessage originalMessage, String reason) {
		super();
		this.userId = userId;
		this.groupId = groupId;
		this.originalMessage = originalMessage;
		this.reason = reason;
		this.timestamp = LocalDateTime.now();
	}

	public Long getUserId() {
		return userId;
	}

	public Long getGroupId() {
		return groupId;
	}

	public String getReason() {
		return reason;
	}

	public LogEventMessage getOriginalMessage() {
		return originalMessage;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

}
