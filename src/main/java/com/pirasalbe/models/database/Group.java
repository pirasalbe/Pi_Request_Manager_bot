package com.pirasalbe.models.database;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Group
 *
 * @author pirasalbe
 *
 */
@Entity
@Table(name = "GROUPS")
public class Group {

	private static final String END_OF_LINE_TO_STRING = "</i>\n";

	@Id
	private Long id;

	@Column(name = "REQUEST_LIMIT")
	private Integer requestLimit;

	@Column(name = "AUDIOBOOKS_DAYS_WAIT")
	private Integer audiobooksDaysWait;

	@Column(name = "ENGLISH_AUDIOBOOKS_DAYS_WAIT")
	private Integer englishAudiobooksDaysWait;

	@Column(name = "ALLOW_EBOOKS")
	private boolean allowEbooks;

	@Column(name = "ALLOW_AUDIOBOOKS")
	private boolean allowAudiobooks;

	public Group() {
		super();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getRequestLimit() {
		return requestLimit;
	}

	public void setRequestLimit(Integer requestLimit) {
		this.requestLimit = requestLimit;
	}

	public Integer getAudiobooksDaysWait() {
		return audiobooksDaysWait;
	}

	public void setAudiobooksDaysWait(Integer audiobooksDaysWait) {
		this.audiobooksDaysWait = audiobooksDaysWait;
	}

	public Integer getEnglishAudiobooksDaysWait() {
		return englishAudiobooksDaysWait;
	}

	public void setEnglishAudiobooksDaysWait(Integer englishAudiobooksDaysWait) {
		this.englishAudiobooksDaysWait = englishAudiobooksDaysWait;
	}

	public boolean isAllowEbooks() {
		return allowEbooks;
	}

	public void setAllowEbooks(boolean allowEbooks) {
		this.allowEbooks = allowEbooks;
	}

	public boolean isAllowAudiobooks() {
		return allowAudiobooks;
	}

	public void setAllowAudiobooks(boolean allowAudiobooks) {
		this.allowAudiobooks = allowAudiobooks;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append("<b>Group info:</b>\n");
		builder.append("Id: <i>").append(id).append(END_OF_LINE_TO_STRING);
		builder.append("Request Limit: <i>").append(requestLimit).append(END_OF_LINE_TO_STRING);
		builder.append("Days to wait to request a new audiobook: <i>").append(audiobooksDaysWait)
				.append(END_OF_LINE_TO_STRING);
		builder.append("Days to wait to request a new english audiobook: <i>").append(englishAudiobooksDaysWait)
				.append(END_OF_LINE_TO_STRING);
		builder.append("Can request ebooks: <i>").append(allowEbooks).append(END_OF_LINE_TO_STRING);
		builder.append("Can request audiobooks: <i>").append(allowAudiobooks).append("</i>");

		return builder.toString();
	}

}
