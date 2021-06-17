CLUSTER_NAME=gatekeeper-operator-test-cluster

setup-kube-for-test:
	k3d cluster create $(CLUSTER_NAME)
	k3d kubeconfig merge $(CLUSTER_NAME)

delete-kube-for-test:
	k3d cluster delete $(CLUSTER_NAME)

run-integration-tests:
	cd ../example-conf/ && ./quick-test.sh k3d-$(CLUSTER_NAME) && cd -

apply-crd:
	kubectl --context=k3d-$(CLUSTER_NAME) apply -f ../crds/

generate-authentication-file:
	mkdir -p /tmp/kube/ || true
	kubectl --context=k3d-$(CLUSTER_NAME) config view --raw --minify > /tmp/kube/config

start-operator-locally:
	docker run -d --network host --name gatekeeper-operator-test --rm -v /tmp/kube/:/conf:ro -e KUBECONFIG=/conf/config docker.io/neo9sas/gatekeeper-operator:latest

stop-operator-locally:
	docker stop gatekeeper-operator-test

local-prepare: setup-kube-for-test apply-crd generate-authentication-file

local-clean:
	k3d cluster delete $(CLUSTER_NAME) || true
	docker stop gatekeeper-operator-test || true

local-test-only: start-operator-locally run-integration-tests

full-local-integration-tests-run: local-prepare local-test-only local-clean
