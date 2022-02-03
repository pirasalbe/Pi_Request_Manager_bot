package com.pirasalbe.services;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

/**
 * Service that manages the admin table
 *
 * @author pirasalbe
 *
 */
@Component
public class SchedulerService {

	public ScheduledFuture<Object> schedule(Runnable runnable, long timeout, TimeUnit timeUnit) {
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		return scheduler.schedule(() -> runnable, timeout, timeUnit);
	}

}
