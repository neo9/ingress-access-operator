package io.neo9.gatekeeper.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import lombok.extern.slf4j.Slf4j;

import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;
import org.springframework.util.backoff.ExponentialBackOff;

@Slf4j
public class RetryContext {

	private final BackOff backOff = new ExponentialBackOff();

	private final Map<String, BackOffExecution> backOffExecutions = new ConcurrentHashMap<>();

	private final AtomicReference<ScheduledExecutorService> executorReference = new AtomicReference<>();

	public void retry(final String backOffKey, final Runnable task) {
		BackOffExecution exec = backOffExecutions.computeIfAbsent(backOffKey, key -> backOff.start());
		long waitInterval = exec.nextBackOff();
		if (waitInterval != BackOffExecution.STOP) {
			ScheduledExecutorService service = getExecutorService();
			service.schedule(task, waitInterval, TimeUnit.MILLISECONDS);
		}
		else {
			log.error("Give up to re-watch: {}", backOffKey);
		}
	}

	public void reset(final String name) {
		backOffExecutions.remove(name);
	}

	public void shutdown() {
		backOffExecutions.clear();
		executorReference.updateAndGet(current -> {
			if (current != null) {
				current.shutdown();
			}
			return null;
		});
	}

	private ScheduledExecutorService getExecutorService() {
		return executorReference.updateAndGet(current -> {
			if (current != null) {
				return current;
			}
			return Executors.newSingleThreadScheduledExecutor();
		});
	}

}
