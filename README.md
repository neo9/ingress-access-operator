Ingress access operator
-----------------------

Goal
----

The goal of this operator is to easily whitelist IPs which can enter by a kubernetes ingress, and think it
globally (note by ingress). More than a cluster view, you can share the `VisitorGroup` definition across cluster.

More concretely, it manages the nginx `nginx.ingress.kubernetes.io/whitelist-source-range` annotation by allowing 
a list of CIDR, which are store in a CRD (`VisitorGroup`).


Concepts and usage
------------------

You can have a look to example directory.

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


Installation
------------

Have a look in `example-k8s` to see how you can deploy ingress access operator with helm.


Local development
-----------------

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

*Basic integration test with k3s*
```
cd ./scripts
make full-local-integration-tests-run
```

Tools
------

*How can I make a release ?*
```
./gradlew release -Prelease.versionIncrementer=incrementMinor
```

Generation reflect source for spring native image
-------------------------------------------------

To generate the reflection config file, use the appropriate script :
```
cd scripts && ./generate-reflect-config.sh && cd ..
```
