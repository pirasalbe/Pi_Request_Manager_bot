package com.pirasalbe.services.telegram.handlers.command.stats;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.database.Request;
import com.pirasalbe.models.request.Format;
import com.pirasalbe.models.request.RequestStatus;
import com.pirasalbe.models.request.Source;
import com.pirasalbe.models.telegram.handlers.TelegramHandler;
import com.pirasalbe.utils.DateUtils;
import com.pirasalbe.utils.TelegramConditionUtils;

/**
 * Service to manage /trending
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramTrendingCommandHandlerService extends AbstractTelegramStatsCommandHandlerService
		implements TelegramHandler {

	public static final String COMMAND = "/trending";

	@Override
	protected void getAndSendStats(Long chatId, Optional<Long> group, String text) {

		Optional<Long> user = TelegramConditionUtils.getUserId(text);
		Optional<Format> format = TelegramConditionUtils.getFormat(text);
		Optional<Source> source = TelegramConditionUtils.getSource(text);
		Optional<String> otherTags = TelegramConditionUtils.getOtherTags(text);
		Optional<Long> limit = TelegramConditionUtils.getLimit(text);

		// count
		AtomicLong requestCount = new AtomicLong();
		AtomicLong filteredCount = new AtomicLong();

		// groups
		Map<Long, String> groupNames = groupService.findAll().stream()
				.collect(Collectors.toMap(Group::getId, Group::getName));

		// multiple requests
		Map<String, List<Request>> requestsByLink = new HashMap<>();

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

					// link
					if (request.getStatus() == RequestStatus.PENDING || request.getStatus() == RequestStatus.PAUSED) {
						addRequest(requestsByLink, request.getLink(), request);
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
			stringBuilder.append("Sendind trending requests with the following filters.");
			stringBuilder.append(filters).append("\n");
			stringBuilder.append("<code>Matching requests</code>: ").append(filteredCount.get()).append(" / ")
					.append(requestCount.get());
			sendMessage(chatId, stringBuilder.toString());
		}

		if (!requestsByLink.isEmpty()) {
			sendMultipleRequests(chatId, requestsByLink, groupNames, limit.orElse(-1l));
		}
	}

	private void sendMultipleRequests(Long chatId, Map<String, List<Request>> map, Map<Long, String> groupNames,
			long limit) {

		StringBuilder headerBuilder = new StringBuilder();
		headerBuilder.append("<b>").append("Links in multiple requests").append("</b>\n");
		sendMessage(chatId, headerBuilder.toString());

		List<Entry<String, List<Request>>> entrySet = new ArrayList<>(map.entrySet());
		entrySet.sort((a, b) -> {
			Integer aSize = a.getValue().size();
			Integer bSize = b.getValue().size();

			return bSize.compareTo(aSize);
		});

		LocalDateTime now = DateUtils.getNow();

		Iterator<Entry<String, List<Request>>> iterator = entrySet.iterator();
		boolean keep = iterator.hasNext();
		long count = 0l;
		while (keep) {
			Entry<String, List<Request>> entry = iterator.next();

			List<Request> value = entry.getValue();
			if (value.size() > 1) {

				StringBuilder builder = new StringBuilder();
				builder.append(value.get(0).getContent()).append("\n\n");
				builder.append("<b>Occurrences</b>:\n");

				for (int i = 0; i < value.size(); i++) {
					Request request = value.get(i);

					builder.append("- ").append(getRequestText(request, i, groupNames, now, true));
				}
				builder.append("\n");

				sendMessage(chatId, builder.toString());
				count++;
			}

			// continue if there are more links, the last link as at least 2 requests, and
			// it sent less than limit messages or the limit is negative
			keep = iterator.hasNext() && value.size() > 1 && (count < limit || limit < 0);
		}

	}

	private void addRequest(Map<String, List<Request>> map, String key, Request request) {
		List<Request> requests = null;
		if (map.containsKey(key)) {
			requests = map.get(key);
		} else {
			requests = new ArrayList<>();
			map.put(key, requests);
		}

		requests.add(request);
	}

}
