package io.neo9.ingress.access.customresources.status;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
		isGetterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY)
public class V1VisitorGroupStatus extends ObservedGenerationAwareStatus {

	@Override
	@JsonProperty("observedGeneration")
	public Long getObservedGeneration() {
		return super.getObservedGeneration();
	}

}
