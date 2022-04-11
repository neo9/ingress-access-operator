package io.neo9.ingress.access.controllers.kubernetes;

import java.util.function.BiFunction;
import java.util.function.Predicate;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher.Action;
import io.fabric8.kubernetes.client.dsl.FilterWatchListDeletable;
import io.neo9.ingress.access.utils.retry.RetryContext;
import io.neo9.ingress.access.utils.retry.RetryableWatcher;
import lombok.extern.slf4j.Slf4j;

import static java.util.Objects.nonNull;

@Slf4j
public abstract class ReconnectableSingleWatcher<Kind extends HasMetadata, KindList> {

	private final RetryContext retryContext = new RetryContext();

	private final String uniqueWatcherIdentifier;

	private final BiFunction<Action, Kind, Void> onEventReceived;

	private final FilterWatchListDeletable<Kind, KindList> filterWatch;

	private final Predicate<Kind> eventFilter;

	private final boolean active;

	private Watch watch;

	protected ReconnectableSingleWatcher(FilterWatchListDeletable<Kind, KindList> filterWatch,
			BiFunction<Action, Kind, Void> onEventReceived) {
		this(true, filterWatch, kind -> true, onEventReceived);
	}

	protected ReconnectableSingleWatcher(boolean active, FilterWatchListDeletable<Kind, KindList> filterWatch,
			BiFunction<Action, Kind, Void> onEventReceived) {
		this(active, filterWatch, kind -> true, onEventReceived);
	}

	protected ReconnectableSingleWatcher(boolean active, FilterWatchListDeletable<Kind, KindList> filterWatch,
			Predicate<Kind> eventFilter, BiFunction<Action, Kind, Void> onEventReceived) {
		this.active = active;
		this.uniqueWatcherIdentifier = this.getClass().getCanonicalName();
		this.filterWatch = filterWatch;
		this.eventFilter = eventFilter;
		this.onEventReceived = onEventReceived;

	}

	public void startWatch(ReconnectableControllerOrchestrator reconnectableControllerOrchestrator) {
		if (!active) {
			return;
		}
		stopWatch(); // make sur only one watch is open at any time
		log.info("starting watch loop {}", uniqueWatcherIdentifier);
		watch = filterWatch.watch(new RetryableWatcher<>(retryContext, String.format("%s", uniqueWatcherIdentifier),
				reconnectableControllerOrchestrator::startOrRestartWatch, eventFilter, onEventReceived));
	}

	public void stopWatch() {
		if (nonNull(watch)) {
			log.info("closing watch loop {}", uniqueWatcherIdentifier);
			watch.close();
			watch = null;
		}
		retryContext.shutdown();
	}

}
