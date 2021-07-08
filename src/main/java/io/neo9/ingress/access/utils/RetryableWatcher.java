package io.neo9.ingress.access.utils;

import java.util.function.BiFunction;

import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RetryableWatcher<T> implements Watcher<T> {

	private final String retryKey;

	private final RetryContext retryContext;

	private final Runnable reconnect;

	private final BiFunction<Action, T, Void> onEventReceived;

	public RetryableWatcher(RetryContext retryContext, String retryKey, Runnable reconnect, BiFunction<Action, T, Void> onEventReceived) {
		this.retryContext = retryContext;
		this.retryKey = retryKey;
		this.reconnect = reconnect;
		this.onEventReceived = onEventReceived;
	}

	@Override
	public void eventReceived(final Action action, final T resource) {
		retryContext.reset(retryKey);
		onEventReceived.apply(action, resource);
	}

	@Override
	public void onClose(final WatcherException cause) {
		if (cause != null) {
			log.warn("Watch connection is closed. Try to re-watch {}", retryKey, cause);
			retryContext.retry(retryKey, reconnect);
		}
	}

}

