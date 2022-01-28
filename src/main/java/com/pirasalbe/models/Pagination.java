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

	private long totalPages;

	private List<T> elements;

	public Pagination(long totalPages, List<T> elements) {
		super();
		this.totalPages = totalPages;
		this.elements = elements;
	}

	public long getTotalPages() {
		return totalPages;
	}

	public List<T> getElements() {
		return elements;
	}

}
