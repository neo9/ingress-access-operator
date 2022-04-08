#!/bin/bash

set -x

if [ ! -z "$1" ]; then
    kubeContextArgs="--context=$1"
fi

source ./integration-tests/utils.sh

for f in $(ls ../example-conf/); do
    kubectl ${kubeContextArgs} apply -f ../example-conf/$f
done

sleep 10

. ./integration-tests/it-http-filtering.sh "http-filtering"

. ./integration-tests/it-istio-sidecar.sh "istio-sidecar"

. ./integration-tests/it-exposer.sh "ingress-exposer"

. ./integration-tests/it-tcp-filtering.sh "tcp-filtering"
