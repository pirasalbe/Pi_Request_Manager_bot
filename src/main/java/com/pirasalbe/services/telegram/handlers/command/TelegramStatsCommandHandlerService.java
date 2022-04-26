package com.pirasalbe.services.telegram.handlers.command;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat.Type;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pirasalbe.models.MultipleCounter;
import com.pirasalbe.models.UserRole;
import com.pirasalbe.models.database.Admin;
import com.pirasalbe.models.database.Group;
import com.pirasalbe.models.database.Request;
import com.pirasalbe.models.request.Format;
import com.pirasalbe.models.request.RequestStatus;
import com.pirasalbe.models.request.Source;
import com.pirasalbe.models.telegram.handlers.TelegramHandler;
import com.pirasalbe.services.AdminService;
import com.pirasalbe.services.RequestManagementService;
import com.pirasalbe.services.telegram.handlers.AbstractTelegramHandlerService;
import com.pirasalbe.utils.DateUtils;
import com.pirasalbe.utils.StringUtils;
import com.pirasalbe.utils.TelegramConditionUtils;

/**
 * Service to manage /me
 *
 * @author pirasalbe
 *
 */
@Component
public class TelegramStatsCommandHandlerService extends AbstractTelegramHandlerService implements TelegramHandler {

	public static final String COMMAND = "/stats";

	public static final UserRole ROLE = UserRole.CONTRIBUTOR;

	@Autowired
	private AdminService adminService;

	@Autowired
	private RequestManagementService requestManagementService;

	@Override
	public void handle(TelegramBot bot, Update update) {
		// delete command
		deleteMessage(bot, update.message(), update.message().chat().type() != Type.Private);

		Long chatId = update.message().chat().id();

		// filters
		String text = update.message().text();

		boolean isPrivate = update.message().chat().type() == Type.Private;
		Optional<Long> group = getGroup(chatId, text, isPrivate);

		// check if the context is valid, either enabled group or PM
		if (groupService.existsById(chatId) || isPrivate) {
			SendMessage sendMessage = new SendMessage(chatId, "Preparing stats..");
			sendMessageAndDelete(bot, sendMessage, 10, TimeUnit.SECONDS);

			getAndSendStats(chatId, group, text);
		}
	}

	private void getAndSendStats(Long chatId, Optional<Long> group, String text) {

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

					// link
					if (group.isEmpty() && request.getStatus() == RequestStatus.PENDING) {
						addRequest(requestsByLink, request.getLink(), request);
					}

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

		if (group.isEmpty() && !requestsByLink.isEmpty()) {
			sendMultipleRequests(chatId, requestsByLink, groupNames);
		}
	}

	private boolean checkFilters(Request request, Optional<Long> group, Optional<Long> user, Optional<Format> format,
			Optional<Source> source, Optional<String> otherTags) {
		boolean valid = true;

		if (group.isPresent()) {
			valid = request.getId().getGroupId().equals(group.get());
		}
		if (valid && user.isPresent()) {
			valid = request.getUserId().equals(user.get());
		}
		if (valid && format.isPresent()) {
			valid = request.getFormat().equals(format.get());
		}
		if (valid && source.isPresent()) {
			valid = request.getSource().equals(source.get());
		}
		if (valid && otherTags.isPresent()) {
			valid = request.getOtherTags().equals(otherTags.get());
		}

		return valid;
	}

	private String getFilters(Optional<Long> group, Optional<Long> user, Optional<Format> format,
			Optional<Source> source, Optional<String> otherTags) {
		StringBuilder filters = new StringBuilder();
		if (group.isPresent()) {
			Long groupId = group.get();
			Optional<Group> groupOptional = groupService.findById(groupId);
			filters.append("\nGroup [").append(groupOptional.orElseThrow().getName()).append(" (<code>").append(groupId)
					.append("</code>)]");
		}
		if (user.isPresent()) {
			filters.append("\nUser [<code>").append(user.get()).append("</code>]");
		}
		if (format.isPresent()) {
			filters.append("\nFormat [").append(format.get()).append("]");
		}
		if (source.isPresent()) {
			filters.append("\nSource [").append(source.get()).append("]");
		}
		if (otherTags.isPresent()) {
			filters.append("\nOther [").append(otherTags.get()).append("]");
		}

		return filters.toString();
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

	private void sendMultipleRequests(Long chatId, Map<String, List<Request>> map, Map<Long, String> groupNames) {

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
			}

			// send last message
			keep = iterator.hasNext() && entry.getValue().size() > 1;
		}

	}

	private void sendMessage(Long chatId, String message) {
		SendMessage sendMessage = new SendMessage(chatId, message);
		sendMessage.parseMode(ParseMode.HTML);
		sendMessage.disableWebPagePreview(true);

		botQueue.add(bot -> bot.execute(sendMessage));
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
