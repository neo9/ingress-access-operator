Ingress access operator
=======================

Goal
----

The goal of this operator is to easily manage resources relative to
kubernetes ingress. It has three main features :

* easily whitelist IPs which can enter by a kubernetes ingress, and think it globally (note by ingress). More
than a cluster view, you can share the `VisitorGroup` definition across cluster.  More concretely, it manages the nginx `nginx.ingress.kubernetes.io/whitelist-source-range` annotation by allowing a list of CIDR, which are store in a CRD (`VisitorGroup`).

* Keep up to date Istio ingress Sidecar with namespaces watched by istiod (configure the ingress sidecar to route traffic 
to services in the mesh).

* Expose a service by generating the associated Ingress (like [xposer](https://github.com/stakater/Xposer), but compatible with newest kubernetes versions). It also auto-configure tls if cert-manager annotations are detected.

Compatibility
-------------

Tested with kubernetes from version 1.19 to 1.22.
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
* label `ingress.neo9.io/access-filtered: "true"` (or the legacy version : `ingress.neo9.io/access-operator-enabled: "true"`), to allow the operator to control that resource
* annotation `ingress.neo9.io/allowed-visitors: neo9,customer` which contains the list of authorized group of visitors (comma separated)
```
apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  labels:
    ingress.neo9.io/access-filtered: "true"
  annotations:
    ingress.neo9.io/allowed-visitors: neo9
    ...
  name: demoingress
  namespace: default
spec:
  ...
```

The operator will autofill nginx whitelist annotation.

By default :
1. `ingress.neo9.io/allowed-visitors` is prioritized
2. If the annotation is not present, default groups are applied (visitor groups with label `ingress.neo9.io/category: default`)
3. If there is no default Visitor Groups, the whitelist 0.0.0.0/0 is applied


**Filtering by default**

These feature is disabled by default, and can be activated by setting an
environment variable : `EXTENSION_DEFAULT_FILTERING_ENABLED` with value `true`.

All `VisitorGroupe` labeled with the `ingress.neo9.io/category: default` will
be applied to ALL ingresses on your cluster. For example :

```
apiVersion: "ingress.neo9.io/v1"
kind: VisitorGroup
metadata:
  name: self
  labels:
    ingress.neo9.io/category: default
spec:
  ...
```

If there is already an existing filtering, it will not be overridden, unless the operator label is present. To 
save which ingress have been updated by operator, it adds an annotation (`filtering-managed-by`) in ingress annotations.

Here is rules which are applied in order to update whitelist (you should only manage default filtering option and operator label).

| Default filtering is enabled | Operator label is present |   Operator annotation is present   |      Whitelist is updated/Override       | Annotation is added |
|:----------------------------:|:-------------------------:|:----------------------------------:|:----------------------------------------:|:-------------------:|
|              No              |            No             |                 No                 |                    No                    |         No          |
|              No              |            No             |       Yes (should not occur)       |                    No                    |         No          |
|              No              |            Yes            |                 No                 |                   Yes                    |         No          |
|              No              |            Yes            |       Yes (should not occur)       |                   Yes                    |         No          |
|             Yes              |            No             |                 No                 | Only if there is not already a whitelist |         Yes         |
|             Yes              |            No             | Yes (added by the operator itself) |                   Yes                    |         Yes         |
|             Yes              |            Yes            |                 No                 |                   Yes                    |         No          |
|             Yes              |            Yes            |       Yes (should not occur)       |                   Yes                    |         No          |


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
      ingress.neo9.io/access-filtered: "true"
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

*Format code*

```
./gradlew format
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

