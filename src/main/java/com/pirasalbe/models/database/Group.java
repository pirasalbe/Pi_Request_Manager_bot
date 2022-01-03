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

}
