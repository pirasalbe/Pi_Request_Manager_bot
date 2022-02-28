package com.pirasalbe.models.database;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * User Request Primary Key
 *
 * @author pirasalbe
 *
 */
@Embeddable
public class ChannelRulePK implements Serializable {

	private static final long serialVersionUID = 3126637537858452476L;

	@Column(name = "CHANNEL_ID")
	private Long channelId;

	private String type;

	private String value;

	public ChannelRulePK() {
		super();
	}

	public ChannelRulePK(Long channelId, String type, String value) {
		super();
		this.channelId = channelId;
		this.type = type;
		this.value = value;
	}

	public Long getChannelId() {
		return channelId;
	}

	public void setChannelId(Long channelId) {
		this.channelId = channelId;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		return Objects.hash(channelId, type, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		ChannelRulePK other = (ChannelRulePK) obj;
		return Objects.equals(channelId, other.channelId) && Objects.equals(type, other.type)
				&& Objects.equals(value, other.value);
	}

}
