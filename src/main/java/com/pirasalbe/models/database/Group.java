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

	private String name;

	@Column(name = "REQUEST_LIMIT")
	private Integer requestLimit;

	@Column(name = "AUDIOBOOKS_DAYS_WAIT")
	private Integer audiobooksDaysWait;

	@Column(name = "ENGLISH_AUDIOBOOKS_DAYS_WAIT")
	private Integer englishAudiobooksDaysWait;

	@Column(name = "REPEAT_HOURS_WAIT")
	private Integer repeatHoursWait;

	@Column(name = "ALLOW_EBOOKS")
	private boolean allowEbooks;

	@Column(name = "ALLOW_AUDIOBOOKS")
	private boolean allowAudiobooks;

	@Column(name = "NO_REPEAT")
	private String noRepeat;

	public Group() {
		super();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public Integer getRepeatHoursWait() {
		return repeatHoursWait;
	}

	public void setRepeatHoursWait(Integer repeatHoursWait) {
		this.repeatHoursWait = repeatHoursWait;
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

	public String getNoRepeat() {
		return noRepeat;
	}

	public void setNoRepeat(String noRepeat) {
		this.noRepeat = noRepeat;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append("<b>Group info:</b>\n");
		builder.append("Id: <i>").append(id).append(END_OF_LINE_TO_STRING);
		builder.append("Name: <i>").append(name).append(END_OF_LINE_TO_STRING);
		builder.append("Request Limit: <i>").append(requestLimit).append(END_OF_LINE_TO_STRING);
		builder.append("Days to wait to request a new non-English audiobook: <i>").append(audiobooksDaysWait)
				.append(END_OF_LINE_TO_STRING);
		builder.append("Days to wait to request a new English audiobook: <i>").append(englishAudiobooksDaysWait)
				.append(END_OF_LINE_TO_STRING);
		builder.append("Hours to wait to bump a request: <i>").append(repeatHoursWait).append(END_OF_LINE_TO_STRING);
		builder.append("Can request ebooks: <i>").append(allowEbooks).append(END_OF_LINE_TO_STRING);
		builder.append("Can request audiobooks: <i>").append(allowAudiobooks).append(END_OF_LINE_TO_STRING);
		builder.append("Forbid repeated requests for: <i>").append(noRepeat).append("</i>");

		return builder.toString();
	}

}
