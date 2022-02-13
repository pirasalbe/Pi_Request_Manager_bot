package com.pirasalbe.models.database;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.Table;

import com.pirasalbe.models.request.Format;
import com.pirasalbe.models.request.RequestStatus;
import com.pirasalbe.models.request.Source;

/**
 * Request
 *
 * @author pirasalbe
 *
 */
@Entity
@Table(name = "REQUESTS")
public class Request {

	@Id
	private RequestPK id;

	@Enumerated(EnumType.STRING)
	private RequestStatus status;

	private String content;

	private String link;

	@Enumerated(EnumType.STRING)
	private Format format;

	@Enumerated(EnumType.STRING)
	private Source source;

	@Column(name = "OTHER_TAGS")
	private String otherTags;

	@Column(name = "USER_ID")
	private Long userId;

	@Column(name = "REQUEST_DATE")
	private LocalDateTime requestDate;

	@Column(name = "RESOLVED_DATE")
	private LocalDateTime resolvedDate;

	public Request() {
		super();
	}

	public RequestPK getId() {
		return id;
	}

	public void setId(RequestPK id) {
		this.id = id;
	}

	public RequestStatus getStatus() {
		return status;
	}

	public void setStatus(RequestStatus status) {
		this.status = status;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

	public Format getFormat() {
		return format;
	}

	public void setFormat(Format format) {
		this.format = format;
	}

	public Source getSource() {
		return source;
	}

	public void setSource(Source source) {
		this.source = source;
	}

	public String getOtherTags() {
		return otherTags;
	}

	public void setOtherTags(String otherTags) {
		this.otherTags = otherTags;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public LocalDateTime getRequestDate() {
		return requestDate;
	}

	public void setRequestDate(LocalDateTime requestDate) {
		this.requestDate = requestDate;
	}

	public LocalDateTime getResolvedDate() {
		return resolvedDate;
	}

	public void setResolvedDate(LocalDateTime resolvedDate) {
		this.resolvedDate = resolvedDate;
	}

}
