package com.pirasalbe.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Utility methods to manage dates
 *
 * @author pirasalbe
 *
 */
public class DateUtils {

	private static final ZoneId ZONE_ID = TimeZone.getDefault().toZoneId();

	private DateUtils() {
		super();
	}

	public static LocalDateTime longToLocalDateTime(Integer timestampUnix) {
		return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestampUnix), ZONE_ID);
	}

	public static LocalDateTime getToday() {
		LocalTime midnight = LocalTime.MIDNIGHT;
		LocalDate today = LocalDate.now(ZONE_ID);

		return LocalDateTime.of(today, midnight);
	}

	public static String formatDate(LocalDateTime localDateTime) {
		return localDateTime.format(DateTimeFormatter.ofPattern("dd LLL uuuu"));
	}

}
