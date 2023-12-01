package io.neo9.ingress.access.customresources;

import java.util.Collections;
import java.util.List;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;
import io.neo9.ingress.access.customresources.spec.V1VisitorGroupSpec;
import io.neo9.ingress.access.customresources.spec.V1VisitorGroupSpecSources;

import static org.apache.commons.lang3.ObjectUtils.anyNull;

@Group("ingress.neo9.io")
@Version("v1")
@ShortNames("vg")
public class VisitorGroup extends CustomResource<V1VisitorGroupSpec, Void> {

	public List<V1VisitorGroupSpecSources> extractSpecSources() {
		if (anyNull(getSpec(), getSpec().getSources())) {
			return Collections.emptyList();
		}
		return getSpec().getSources();
	}

}
