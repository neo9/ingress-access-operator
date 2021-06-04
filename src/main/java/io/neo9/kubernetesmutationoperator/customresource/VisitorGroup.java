package io.neo9.kubernetesmutationoperator.customresource;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;
import io.neo9.kubernetesmutationoperator.customresource.generated.V1VisitorGroupSpec;

@Group("mutable.neo9.io")
@Version("v1")
@ShortNames("vg")
public class VisitorGroup extends CustomResource<V1VisitorGroupSpec, Void> {}

