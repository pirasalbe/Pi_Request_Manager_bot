package com.pirasalbe.models.database;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Channel rule
 *
 * @author pirasalbe
 *
 */
@Entity
@Table(name = "CHANNEL_RULES")
public class ChannelRule {

	@Id
	private ChannelRulePK id;

	public ChannelRule() {
		super();
	}

	public ChannelRulePK getId() {
		return id;
	}

	public void setId(ChannelRulePK id) {
		this.id = id;
	}

}
