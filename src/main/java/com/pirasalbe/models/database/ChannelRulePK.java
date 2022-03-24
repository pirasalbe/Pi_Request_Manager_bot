package com.pirasalbe.models.database;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import com.pirasalbe.models.ChannelRuleType;

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

	@Enumerated(EnumType.STRING)
	private ChannelRuleType type;

	private String value;

	public ChannelRulePK() {
		super();
	}

	public ChannelRulePK(Long channelId, ChannelRuleType type, String value) {
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

	public ChannelRuleType getType() {
		return type;
	}

	public void setType(ChannelRuleType type) {
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
