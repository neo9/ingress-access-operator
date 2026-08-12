package io.neo9.ingress.access.customresources.external.nginx;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Version;
import io.neo9.ingress.access.customresources.external.nginx.spec.NginxPolicySpec;

@Group("k8s.nginx.org")
@Version("v1")
@Kind("Policy")
@Plural("policies")
public class NginxPolicy extends CustomResource<NginxPolicySpec, Void> implements Namespaced {

}
