CLUSTER_NAME=ingress-access-operator-test
CLUSTER_VERSION=v1.21.3-k3s1
#CLUSTER_VERSION=v1.22.1-rc1-k3s1

setup-kube-for-test:
	k3d cluster create $(CLUSTER_NAME) --image rancher/k3s:$(CLUSTER_VERSION)
	k3d kubeconfig merge $(CLUSTER_NAME)

delete-kube-for-test:
	k3d cluster delete $(CLUSTER_NAME)

run-integration-tests:
	./run-integration-test.sh k3d-$(CLUSTER_NAME)

apply-crd:
	kubectl --context=k3d-$(CLUSTER_NAME) apply -f ../crds/
	kubectl --context=k3d-$(CLUSTER_NAME) create namespace nginx-istio-ingress
	kubectl --context=k3d-$(CLUSTER_NAME) apply -f https://raw.githubusercontent.com/istio/istio/1.11.2/manifests/charts/base/crds/crd-all.gen.yaml

generate-authentication-file:
	mkdir -p /tmp/kube/ || true
	kubectl --context=k3d-$(CLUSTER_NAME) config view --raw --minify > /tmp/kube/config

start-operator-locally:
	docker run -d --rm \
		--network host \
		--name ingress-access-operator-test \
		-e EXTENSION_WATCH_INGRESS_ANNOTATIONS_ENABLED=true \
		-e EXTENSION_UPDATE_ISTIO_INGRESS_SIDECAR_ENABLED=true \
		-e EXTENSION_EXPOSER_ENABLED=true \
		-v /tmp/kube/:/conf:ro \
		-e KUBECONFIG=/conf/config \
		docker.io/neo9sas/ingress-access-operator:latest

stop-operator-locally:
	docker stop ingress-access-operator-test

local-prepare: setup-kube-for-test apply-crd generate-authentication-file

local-clean:
	k3d cluster delete $(CLUSTER_NAME) || true
	docker stop ingress-access-operator-test || true

local-test-only: start-operator-locally run-integration-tests

full-local-integration-tests-run: local-prepare local-test-only local-clean
