package com.pirasalbe.models;

/**
 * Pagination request
 *
 * @author pirasalbe
 */
public class PaginationRequest {

	private long limit;

	private long skip;

	public PaginationRequest(long limit, long skip) {
		super();
		this.limit = limit;
		this.skip = skip;
	}

	public long getLimit() {
		return limit;
	}

	public long getSkip() {
		return skip;
	}

}
