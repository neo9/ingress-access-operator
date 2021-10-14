package io.neo9.ingress.access.customresources.external.istio;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Version;
import io.neo9.ingress.access.customresources.external.istio.spec.SidecarSpec;

@Group("networking.istio.io")
@Version("v1alpha3")
@Plural("sidecars")
public class Sidecar extends CustomResource<SidecarSpec, Void> implements Namespaced {
}
