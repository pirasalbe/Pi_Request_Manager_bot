package com.pirasalbe.models.database;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Channel
 *
 * @author pirasalbe
 *
 */
@Entity
@Table(name = "CHANNELS")
public class Channel {

	@Id
	private Long id;

	private String name;

	public Channel() {
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

}
