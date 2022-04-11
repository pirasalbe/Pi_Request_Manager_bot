package com.pirasalbe.models;

import com.pirasalbe.models.request.Format;

/**
 * Info for looking up the request
 *
 * @author pirasalbe
 *
 */
public class LookupInfo {

	private boolean valid;

	private String name;

	private String caption;

	private Format format;

	public LookupInfo(boolean valid, String name, String caption, Format format) {
		this.valid = valid;
		this.name = name;
		this.caption = caption;
		this.format = format;
	}

	public boolean isValid() {
		return valid;
	}

	public String getName() {
		return name;
	}

	public String getCaption() {
		return caption;
	}

	public Format getFormat() {
		return format;
	}

}
