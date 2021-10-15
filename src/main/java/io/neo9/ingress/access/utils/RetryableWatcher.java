package io.neo9.ingress.access.utils;

import java.util.function.BiFunction;
import java.util.function.Predicate;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import lombok.extern.slf4j.Slf4j;

import static io.neo9.ingress.access.utils.KubernetesUtils.getResourceNamespaceAndName;

@Slf4j
public class RetryableWatcher<T extends HasMetadata> implements Watcher<T> {

	private final String retryKey;

	private final RetryContext retryContext;

	private final Runnable reconnect;

	private final BiFunction<Action, T, Void> onEventReceived;

	private final Predicate<T> eventFilter;

	public RetryableWatcher(RetryContext retryContext, String retryKey, Runnable reconnect, Predicate<T> eventFilter, BiFunction<Action, T, Void> onEventReceived) {
		this.retryContext = retryContext;
		this.retryKey = retryKey;
		this.reconnect = reconnect;
		this.onEventReceived = onEventReceived;
		this.eventFilter = eventFilter;
	}

	@Override
	public void eventReceived(final Action action, final T resource) {
		retryContext.reset(retryKey);
		if (eventFilter.test(resource)) {
			onEventReceived.apply(action, resource);
		}
		else {
			String ingressNamespaceAndName = getResourceNamespaceAndName(resource);
			log.debug("won't apply reconciliation on {} because it does not match with predicate", ingressNamespaceAndName);
		}
	}

	@Override
	public void onClose(final WatcherException cause) {
		if (cause != null) {
			log.warn("Watch connection is closed. Try to re-watch {}", retryKey, cause);
			retryContext.retry(retryKey, reconnect);
		}
	}

}
