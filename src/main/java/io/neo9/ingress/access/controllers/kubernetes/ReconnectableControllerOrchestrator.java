package io.neo9.ingress.access.controllers.kubernetes;

import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

/**
 * When we meet an exception (#25), some listeners are in a blocked state and cannot
 * recover by itself.
 *
 * The goal of this class is to centralize stop/start listen action.
 */
@Component
@Slf4j
public class ReconnectableControllerOrchestrator {

	private final List<ReconnectableSingleWatcher> watchers;

	public ReconnectableControllerOrchestrator(List<ReconnectableSingleWatcher> watchers) {
		this.watchers = watchers;
	}

	@PostConstruct
	public void startOrRestartWatch() {
		log.info("start or restart all watchers");
		watchers.forEach(w -> w.startWatch(this));
	}

	@PreDestroy
	public void stopWatch() {
		log.info("stop all watchers");
		watchers.forEach(ReconnectableSingleWatcher::stopWatch);
	}

}
