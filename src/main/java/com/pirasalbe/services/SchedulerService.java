package com.pirasalbe.services;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.pengrad.telegrambot.TelegramBot;
import com.pirasalbe.services.telegram.TelegramBotService;

/**
 * Service that manages the admin table
 *
 * @author pirasalbe
 *
 */
@Component
public class SchedulerService {

	private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerService.class);

	private TelegramBot bot;

	public SchedulerService(TelegramBotService telegramBotService) {
		this.bot = telegramBotService.getBot();
	}

	public <T> void schedule(BiConsumer<TelegramBot, T> consumer, T obj, long timeout, TimeUnit timeUnit) {
		ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
		scheduledExecutorService.schedule(() -> consumer.accept(bot, obj), timeout, timeUnit);
		scheduledExecutorService.shutdown();
	}

	public void schedule(Runnable runnable, long timeout, TimeUnit timeUnit) {
		ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
		scheduledExecutorService.schedule(getSafeRunnable(runnable), timeout, timeUnit);
		scheduledExecutorService.shutdown();
	}

	private Runnable getSafeRunnable(Runnable runnable) {
		return () -> {
			try {
				runnable.run();
			} catch (Exception e) {
				LOGGER.error("Unexpected error in scheduled task", e);
			}
		};
	}

}
