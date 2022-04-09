package com.pirasalbe.models;

public class MultipleCounter {

	private Long first;

	private Long second;

	public MultipleCounter() {
		first = 0l;
		second = 0l;
	}

	public void incrementFirst() {
		first++;
	}

	public Long getFirst() {
		return first;
	}

	public void incrementSecond() {
		second++;
	}

	public Long getSecond() {
		return second;
	}

}
