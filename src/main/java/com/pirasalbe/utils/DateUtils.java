package com.pirasalbe.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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

	public static LocalDateTime integerToLocalDateTime(Integer timestampUnix) {
		return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestampUnix), ZONE_ID);
	}

	public static LocalDateTime getNow() {
		return LocalDateTime.now(ZONE_ID);
	}

	public static LocalDateTime getToday() {
		LocalTime midnight = LocalTime.MIDNIGHT;
		LocalDate today = LocalDate.now(ZONE_ID);

		return LocalDateTime.of(today, midnight);
	}

	public static String formatDate(LocalDateTime localDateTime) {
		return localDateTime.format(DateTimeFormatter.ofPattern("dd LLL uuuu"));
	}

	public static long getHours(LocalDateTime from, LocalDateTime to) {
		return ChronoUnit.HOURS.between(from, to);
	}

	public static long getMinutes(LocalDateTime from, LocalDateTime to) {
		return getMinutes(from, to, getHours(from, to));
	}

	public static long getMinutes(LocalDateTime from, LocalDateTime to, long hours) {
		long minutes = ChronoUnit.MINUTES.between(from, to);

		return minutes - hours * 60;
	}

}
