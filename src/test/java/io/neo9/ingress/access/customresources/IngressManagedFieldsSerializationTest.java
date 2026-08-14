package io.neo9.ingress.access.customresources;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.FieldsV1;
import io.fabric8.kubernetes.api.model.ManagedFieldsEntryBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.kubernetes.client.utils.Serialization;

class IngressManagedFieldsSerializationTest {

	@Test
	void clonesIngressWithServerSideApplyManagedFields() {
		FieldsV1 fieldsV1 = new FieldsV1();
		Map<String, Object> metadataFields = new LinkedHashMap<>();
		metadataFields.put("f:annotations", new LinkedHashMap<>());
		fieldsV1.setAdditionalProperty("f:metadata", metadataFields);

		Ingress ingress = new IngressBuilder().withNewMetadata().withNamespace("validation").withName("v2-api-aqua-fr")
				.withManagedFields(new ManagedFieldsEntryBuilder().withManager("nginx-ingress").withOperation("Apply")
						.withApiVersion("networking.k8s.io/v1").withFieldsV1(fieldsV1).build())
				.endMetadata().build();

		Ingress cloned = assertDoesNotThrow(() -> Serialization.clone(ingress));
		assertNotNull(cloned);
		assertNotNull(cloned.getMetadata().getManagedFields());
	}

}
