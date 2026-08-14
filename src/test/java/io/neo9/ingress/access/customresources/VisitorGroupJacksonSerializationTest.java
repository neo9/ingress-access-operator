package io.neo9.ingress.access.customresources;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.neo9.ingress.access.customresources.status.V1VisitorGroupStatus;

class VisitorGroupJacksonSerializationTest {

	@Test
	void serializesVisitorGroupStatusForJosdkClone() {
		ObjectMapper mapper = new ObjectMapper();
		VisitorGroup visitorGroup = new VisitorGroup();
		visitorGroup.setStatus(new V1VisitorGroupStatus());
		visitorGroup.getStatus().setObservedGeneration(1L);

		assertDoesNotThrow(() -> mapper.writeValueAsString(visitorGroup));
	}

}
