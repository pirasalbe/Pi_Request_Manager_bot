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

	public static String firstToUpperCase(String string) {
		String result = null;

		if (string != null && string.length() > 1) {
			result = string.substring(0, 1).toUpperCase() + string.substring(1);
		}

		return result;
	}

}
