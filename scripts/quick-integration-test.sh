#!/bin/bash

set -x

if [ ! -z "$1" ]; then
    kubeContextArgs="--context=$1"
fi

function checkFilterValue() {
    ingressName=$1
    expectation=$2
    ips=$(kubectl ${kubeContextArgs} get ingress ${ingressName} -o yaml | grep 'whitelist-source-range' | grep -v 'f:' | awk '{print $2}')
    if [ ${ips} == ${expectation} ]; then
        echo "assertion ok"
    else
        echo "Unexpected value"
        echo "${ips}"
        echo "${expectation}"
        exit 1
    fi
}

kubectl ${kubeContextArgs} apply -f ../example-conf/
sleep 3
checkFilterValue "demoingress2" "10.1.1.1/32,10.1.1.2/32,10.1.1.3/32,10.1.2.1/32,10.1.2.2/32"

kubectl ${kubeContextArgs} apply -f ./test-patch/patch-visitorgroup-customer.yaml
sleep 3
checkFilterValue "demoingress2" "10.1.1.1/32,10.1.1.2/32,10.1.1.3/32,10.1.2.1/32,10.1.2.3/32"

kubectl ${kubeContextArgs} apply -f test-patch/patch-ingress-2g.yaml
sleep 3
checkFilterValue "demoingress2" "10.1.2.1/32,10.1.2.3/32"

