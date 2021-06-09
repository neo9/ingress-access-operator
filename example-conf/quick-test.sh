#!/bin/bash

function checkFilterValue() {
    ips=$(kubectl get ingress $1 -o yaml | grep -E 'whitelist' | awk '{print $2}')
    if [ $ips == $2 ]; then
        echo "assertion ok"
    else
        echo "Unexpected value"
        echo "$ips"
        echo "$2"
        exit 1
    fi
}

kubectl apply -f base/
sleep 3
checkFilterValue "demoingress2" "10.1.1.1/32,10.1.1.10/32,10.1.1.2/32,10.1.1.3/32,10.1.2.8/32,10.1.2.9/32"

kubectl apply -f ./test-patch/patch-visitorgroup-customer.yaml
sleep 3
checkFilterValue "demoingress2" "10.1.1.1/32,10.1.1.10/32,10.1.1.2/32,10.1.1.3/32,10.10.20.80/32,10.10.20.90/32"

kubectl apply -f test-patch/patch-ingress-2g.yaml
sleep 3
checkFilterValue "demoingress2" "10.10.20.80/32,10.10.20.90/32"

