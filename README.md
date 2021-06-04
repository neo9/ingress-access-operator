Gatekeeper operator
-------------------

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
apiVersion: "mutable.neo9.io/v1"
kind: VisitorGroup
metadata:
name: neo9
spec:
  sources:
  - name: Paris 1
    cidr: 10.1.1.1/32
  - name: Lyon
    cidr: 10.1.1.2/32
```
The name is only here for information purpose.

Then, create an `Ingress` with :
* label `mutation.neo9.io/mutable: "true"`, to allow the operator to control that resource
* annotation `mutation.neo9.io/ingress-allowed-visitors: neo9,customer` which contains the list of authorized group of CIDR (comma separated)
```
apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  labels:
    mutation.neo9.io/mutable: "true"
  annotations:
    mutation.neo9.io/ingress-allowed-visitors: neo9
    ...
  name: demoingress
  namespace: default
spec:
  ...
```

The operator will auto fill nginx whitelist annotation.


Test docker image locally
-------------------------

```
mkdir /tmp/kube
kubectl config view --raw --minify | sed 's/cmd-path.*/cmd-path: gcloud/' > /tmp/kube/config

./gradlew bootBuildImage
docker run -v /tmp/kube/:/conf:ro -e KUBECONFIG=/conf/config docker.io/library/gatekeeper-operator:0.0.1-SNAPSHOT
```


Generation reflect source for spring native image
-------------------------------------------------

Native image is unused for now.

*Note* : I know this is not an optimized way !

```
for jar in ~/Téléchargements/kubernetes-model-common-5.4.1.jar ~/Téléchargements/kubernetes-model-core-5.4.1.jar ~/Téléchargements/kubernetes-model-networking-5.4.1.jar; do
    unzip -l ${jar} | grep '/model' | awk '{print $4}' | sed 's/.class$//' | tr '/' '.' | while read l; do echo "{\"name\": \"$l\", \"allDeclaredFields\": true, \"allDeclaredMethods\": true, \"allPublicConstructors\": true},"; done >> src/main/resources/META-INF/native-image/reflect-config.json
done
```
