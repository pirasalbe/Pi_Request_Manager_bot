package com.pirasalbe.models;

import java.util.List;

/**
 * Paginated result
 *
 * @author pirasalbe
 *
 * @param <T> Element type
 */
public class Pagination<T> {

	private long totalItems;

	private List<T> elements;

	public Pagination(long totalItems, List<T> elements) {
		super();
		this.totalItems = totalItems;
		this.elements = elements;
	}

	public long getTotalItems() {
		return totalItems;
	}

	public List<T> getElements() {
		return elements;
	}

}
