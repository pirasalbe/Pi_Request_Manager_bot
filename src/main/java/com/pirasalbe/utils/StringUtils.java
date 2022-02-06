package com.pirasalbe.utils;

/**
 * Utility methods to manage strings
 *
 * @author pirasalbe
 *
 */
public class StringUtils {

	private StringUtils() {
		super();
	}

	public static String getPlural(long count) {
		String result = "";

		if (count > 1) {
			result = "s";
		}

		return result;
	}

}
