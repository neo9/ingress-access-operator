Ingress access operator
=======================

Goal
----

The goal of this operator is to easily manage resources relative to
kubernetes ingress. It have three main features :

* easily whitelist IPs which can enter by a kubernetes ingress, and think it globally (note by ingress). More
than a cluster view, you can share the `VisitorGroup` definition across cluster.  More concretely, it manages the nginx `nginx.ingress.kubernetes.io/whitelist-source-range` annotation by allowing a list of CIDR, which are store in a CRD (`VisitorGroup`).

* Keep up to date Istio ingress Sidecar with namespaces watched by istiod (configure the ingress sidecar to route traffic 
to services in the mesh).

* Expose a service by generating the associated Ingress (like [xposer](https://github.com/stakater/Xposer), but compatible with newest kubernetes versions). It also auto-configure tls if cert-manager annotations are detected.

Compatibility
-------------

Tested with kubernetes from version 1.19 to 1.22+.
The istio sidecar generation was tested with istio 1.10 and 1.11.

Concepts and usage
------------------

You can have a look to example directory.

**IP filtering**

A list of CIDR attached to the same person/company are forming a `VisitorGroup` :
```
apiVersion: "ingress.neo9.io/v1"
kind: VisitorGroup
metadata:
name: neo9
spec:
  sources:
    - name: Paris
      cidr: 10.1.1.1/32
    - name: Lyon
      cidr: 10.1.1.2/32
```
The name is only here for information purpose.

Then, create an `Ingress` with :
* label `ingress.neo9.io/access-operator-enabled: "true"`, to allow the operator to control that resource
* annotation `ingress.neo9.io/allowed-visitors: neo9,customer` which contains the list of authorized group of visitors (comma separated)
```
apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  labels:
    ingress.neo9.io/access-operator-enabled: "true"
  annotations:
    ingress.neo9.io/allowed-visitors: neo9
    ...
  name: demoingress
  namespace: default
spec:
  ...
```

The operator will auto fill nginx whitelist annotation.

_Extension : watch ingress on annotations_

Some tool may not allow to configure ingress labels, that's why
you can also customize watcher rules. To be sur you are not editing a resource you don't want to, 
you have to place operator `label` value in `annotations` block (see examples). That's a way to 
bypass label placing limitation, but it does not takes all advantages of kubernetes.

These feature is disabled by default, and can be activated by setting an environment variable : `EXTENSION_WATCH_INGRESS_ANNOTATIONS_ENABLED` with value `true`.


**Istio ingress sidecar autoconfiguration**

This will create a `Sidecar` resource, in the ingress controller namespace.

These feature is disabled by default, and can be activated by setting an environment variable : `EXTENSION_UPDATE_ISTIO_INGRESS_SIDECAR_ENABLED` with value `true`.

Have a look to `application.yaml` to see possible configurations.


**Service exposer**

This will create a `Ingress` resource, in the namespace where
is located the service. The ingress will have the same name.

These feature is disabled by default, and can be activated by setting an
environment variable : `EXTENSION_EXPOSER_ENABLED` with value `true`.

Have a look to `application.yaml` to see possible configurations.

Here is an example of service configuration :
```
kind: Service
apiVersion: v1
metadata:
  name: service-to-expose
  namespace: default
  labels:
    ingress.neo9.io/expose: "true"
  annotations:
    # ingress.neo9.io/expose-hostname: "{{name}}.{{namespace}}.{{domain}}"
    ingress.neo9.io/expose-labels: |-
      ingress.neo9.io/access-operator-enabled: "true"
    ingress.neo9.io/expose-annotations: |-
      kubernetes.io/ingress.class: nginx
      cert-manager.io/cluster-issuer: letsencrypt-dns-staging-gcp
      external-dns.alpha.kubernetes.io/ttl: "5"
      ingress.neo9.io/allowed-visitors: neo9
      nginx.ingress.kubernetes.io/ssl-redirect: "true"
spec:
  ...
```

Installation
------------

Have a look in `values` to see how you can deploy ingress access operator with helm.


Local development
-----------------

*Deactivate native image*

You may want to deactivate native image during development.
That can easily be done by defining an environment variable `NATIVE_IMAGE` with value `false`.

*Quick development start*
```
./gradlew bootRun --args='--spring.profiles.active=dev'
```

*Test docker image*
```
mkdir /tmp/kube
kubectl --context=neokube get pods # refresh token
kubectl --context=neokube config view --raw --minify | sed 's/cmd-path.*/cmd-path: gcloud/' > /tmp/kube/config

./gradlew bootBuildImage
docker run -v /tmp/kube/:/conf:ro -e KUBECONFIG=/conf/config docker.io/neo9sas/ingress-access-operator:latest
```

Remote development
------------------

A deployment made with garden is configured to not use native image

*Remote debug*

You can connect remote debugger on port 8082.
```
kubectl -n dev-$user port-forward svc/dev-$user-ingress-access-operator 9082:8082
```
And then connect your debugger localhost 9082.

Tools
------

*Basic integration test with k3s*
```
cd ./scripts
make full-local-integration-tests-run
```

*How can I make a release ?*
```
./gradlew release -Prelease.versionIncrementer=incrementMinor
```
Or simply add git tag, it will generate a release with the tag version.

Generation reflect source for spring native image
-------------------------------------------------

To generate the reflection configuration file, use the appropriate script :
```
cd scripts && ./generate-reflect-config.sh && cd ..
```

