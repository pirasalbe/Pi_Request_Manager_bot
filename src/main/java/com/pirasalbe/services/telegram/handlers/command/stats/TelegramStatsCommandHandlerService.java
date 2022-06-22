package com.pirasalbe.services.telegram.handlers.command.stats;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.pirasalbe.models.MultipleCounter;
import com.pirasalbe.models.database.Admin;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.database.Request;
import com.pirasalbe.models.request.Format;
import com.pirasalbe.models.request.RequestStatus;
import com.pirasalbe.models.request.Source;
import com.pirasalbe.models.telegram.handlers.TelegramHandler;
import com.pirasalbe.utils.DateUtils;
import com.pirasalbe.utils.StringUtils;
import com.pirasalbe.utils.TelegramConditionUtils;

/**
 * Service to manage /stats
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramStatsCommandHandlerService extends AbstractTelegramStatsCommandHandlerService
		implements TelegramHandler {

	public static final String COMMAND = "/stats";

	@Override
	protected void getAndSendStats(Long chatId, Optional<Long> group, String text) {

		Optional<Long> user = TelegramConditionUtils.getUserId(text);
		Optional<Format> format = TelegramConditionUtils.getFormat(text);
		Optional<Source> source = TelegramConditionUtils.getSource(text);
		Optional<String> otherTags = TelegramConditionUtils.getOtherTags(text);

		// count
		AtomicLong requestCount = new AtomicLong();
		AtomicLong filteredCount = new AtomicLong();

		// count requests by status
		Map<RequestStatus, AtomicLong> requestByStatus = new EnumMap<>(RequestStatus.class);
		// count requests by format
		Map<Format, AtomicLong> requestByFormat = new EnumMap<>(Format.class);
		// count requests by source
		Map<Source, AtomicLong> requestBySource = new EnumMap<>(Source.class);

		// count requests by language
		Map<String, AtomicLong> requestByLanguage = new HashMap<>();

		// count requests by group
		Map<Long, String> groupNames = groupService.findAll().stream()
				.collect(Collectors.toMap(Group::getId, Group::getName));
		Map<Long, AtomicLong> requestByGroup = new HashMap<>();

		// top contributors with count
		Map<Long, String> adminNames = adminService.findAll().stream()
				.collect(Collectors.toMap(Admin::getId, Admin::getName));
		Map<Long, AtomicLong> requestByContributors = new HashMap<>();

		// request&fulfillment per day
		Map<LocalDate, MultipleCounter> requestAndFulfillmentPerDay = new HashMap<>();

		// check all requests
		int page = 0;
		int size = 100;
		boolean keep = true;
		while (keep) {
			Page<Request> requestPage = requestManagementService.findAll(page, size);
			List<Request> requests = requestPage.toList();

			for (Request request : requests) {
				requestCount.incrementAndGet();

				if (checkFilters(request, group, user, format, source, otherTags)) {
					filteredCount.incrementAndGet();

					// status
					increaseCount(requestByStatus, request.getStatus());
					// format
					increaseCount(requestByFormat, request.getFormat());
					// source
					increaseCount(requestBySource, request.getSource());

					// language
					increaseCount(requestByLanguage, request.getOtherTags());

					// group
					increaseCount(requestByGroup, request.getId().getGroupId());

					// day
					increaseMultipleCount(requestAndFulfillmentPerDay, request.getRequestDate().toLocalDate(),
							MultipleCounter::incrementFirst);
					if (request.getResolvedDate() != null) {
						increaseMultipleCount(requestAndFulfillmentPerDay, request.getResolvedDate().toLocalDate(),
								MultipleCounter::incrementSecond);
					}

					// contributors
					if (request.getContributor() != null && request.getStatus() == RequestStatus.RESOLVED) {
						increaseCount(requestByContributors, request.getContributor());
					}
				}
			}

			keep = requestPage.hasNext();
			page++;
		}

		// send stats
		if (filteredCount.get() != requestCount.get()) {
			String filters = getFilters(group, user, format, source, otherTags);
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("Sendind stats with the following filters.");
			stringBuilder.append(filters).append("\n");
			stringBuilder.append("<code>Matching requests</code>: ").append(filteredCount.get()).append(" / ")
					.append(requestCount.get());
			sendMessage(chatId, stringBuilder.toString());
		}
		sendStats(chatId, "Requests by Status", requestByStatus, r -> StringUtils.firstToUpperCase(r.getDescription()),
				filteredCount);
		sendStats(chatId, "Requests by Format", requestByFormat, Format::name, filteredCount);
		sendStats(chatId, "Requests by Source", requestBySource, Source::name, filteredCount);

		sendStats(chatId, "Requests by Language", requestByLanguage, StringUtils::firstToUpperCase, filteredCount);
		sendStats(chatId, "Requests by Group", requestByGroup, groupNames::get, filteredCount);
		sendStats(chatId, "Contributions", requestByContributors, adminNames::get, filteredCount);

		sendStatsByDate(chatId, requestAndFulfillmentPerDay);
	}

	private <K> void sendStats(Long chatId, String title, Map<K, AtomicLong> map, Function<K, String> keyToString,
			AtomicLong totalRequests) {

		StringBuilder headerBuilder = new StringBuilder();
		headerBuilder.append("<b>").append(title).append("</b>\n\n");
		String header = headerBuilder.toString();

		StringBuilder builder = new StringBuilder(header);

		List<Entry<K, AtomicLong>> orderedEntries = getOrderedEntries(map);
		for (int i = 0; i < orderedEntries.size(); i++) {
			Entry<K, AtomicLong> entry = orderedEntries.get(i);

			StringBuilder requestBuilder = new StringBuilder();
			requestBuilder.append("<code>").append(keyToString.apply(entry.getKey())).append("</code>: ");
			requestBuilder.append(entry.getValue().get()).append(" / ").append(totalRequests.get());
			requestBuilder.append(" (").append(getPercentage(entry.getValue().get(), totalRequests.get())).append("%")
					.append(")");
			requestBuilder.append("\n");

			String requestText = requestBuilder.toString();

			// if length is > message limit, send current text
			if (builder.length() + requestText.length() > 4096) {
				sendMessage(chatId, builder.toString());
				builder = new StringBuilder(header);
			}
			builder.append(requestText);
			// send last message
			if (i == orderedEntries.size() - 1) {
				sendMessage(chatId, builder.toString());
			}
		}
	}

	private void sendStatsByDate(Long chatId, Map<LocalDate, MultipleCounter> map) {

		StringBuilder headerBuilder = new StringBuilder();
		headerBuilder.append("<b>").append("Requests requested/fulfilled per day").append("</b>\n\n");
		String header = headerBuilder.toString();

		StringBuilder builder = new StringBuilder(header);

		List<Entry<LocalDate, MultipleCounter>> entrySet = new ArrayList<>(map.entrySet());
		entrySet.sort((a, b) -> b.getKey().compareTo(a.getKey()));

		Iterator<Entry<LocalDate, MultipleCounter>> iterator = entrySet.iterator();
		while (iterator.hasNext()) {
			Entry<LocalDate, MultipleCounter> entry = iterator.next();

			StringBuilder requestBuilder = new StringBuilder();
			requestBuilder.append("<code>").append(DateUtils.formatDate(entry.getKey())).append("</code>: ");
			requestBuilder.append(entry.getValue().getFirst()).append(" / ").append(entry.getValue().getSecond());
			requestBuilder.append("\n");

			String requestText = requestBuilder.toString();

			// if length is > message limit, send current text
			if (builder.length() + requestText.length() > 4096) {
				sendMessage(chatId, builder.toString());
				builder = new StringBuilder(header);
			}
			builder.append(requestText);
			// send last message
			if (!iterator.hasNext()) {
				sendMessage(chatId, builder.toString());
			}
		}

	}

	private <K> List<Entry<K, AtomicLong>> getOrderedEntries(Map<K, AtomicLong> map) {
		List<Entry<K, AtomicLong>> entryList = new ArrayList<>(map.entrySet());

		entryList.sort((a, b) -> {
			Long first = a.getValue().get();
			Long second = b.getValue().get();

			return second.compareTo(first);
		});

		return entryList;
	}

	private BigDecimal getPercentage(long count, long total) {
		double percentage = count * 100d / total;

		return BigDecimal.valueOf(percentage).setScale(2, RoundingMode.HALF_UP);
	}

	private <K> void increaseCount(Map<K, AtomicLong> map, K key) {
		AtomicLong count = null;
		if (map.containsKey(key)) {
			count = map.get(key);
		} else {
			count = new AtomicLong();
			map.put(key, count);
		}

		count.incrementAndGet();
	}

	private <K> void increaseMultipleCount(Map<K, MultipleCounter> map, K key, Consumer<MultipleCounter> consumer) {
		MultipleCounter multipleCounter = null;
		if (map.containsKey(key)) {
			multipleCounter = map.get(key);
		} else {
			multipleCounter = new MultipleCounter();
			map.put(key, multipleCounter);
		}

		consumer.accept(multipleCounter);
	}

}
