package io.neo9.ingress.access.controllers.kubernetes;

public interface ReconnectableWatcher {

	void startWatch(ReconnectableControllerOrchestrator reconnectableControllerOrchestrator);

	void stopWatch();

}
