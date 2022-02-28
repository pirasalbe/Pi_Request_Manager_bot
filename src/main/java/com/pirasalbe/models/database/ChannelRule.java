package com.pirasalbe.models.database;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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
