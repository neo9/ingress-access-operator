#!/bin/bash

box $1

kubernetesMajorMinorVersion=$(kubectl version --short | grep 'Server Version' | awk -F':' '{print $2}' | sed 's/.*v\([0-9]*\)\.\([0-9]*\)\.\([0-9]*\).*/\1\2/')

sleep 3
checkWhitelistValue "demoingress2" "10.1.1.1/32,10.1.1.2/32,10.1.1.3/32,10.1.2.1/32,10.1.2.2/32"

kubectl ${kubeContextArgs} apply -f ./test-patch/visitorgroups/patch-visitorgroup-customer.yaml
sleep 3
checkWhitelistValue "demoingress2" "10.1.1.1/32,10.1.1.2/32,10.1.1.3/32,10.1.2.1/32,10.1.2.3/32"

kubectl ${kubeContextArgs} apply -f ./test-patch/ingresses/patch-ingress-2g.yaml
sleep 3
checkWhitelistValue "demoingress2" "10.1.2.1/32,10.1.2.3/32"

checkWhitelistValue "watch-on-annotation" "10.1.1.1/32,10.1.1.2/32,10.1.1.3/32"

checkWhitelistValue "not-watched-ingress-with-annotations" "10.1.9.1/32"

checkWhitelistValue "not-watched-ingress" "10.1.9.1/32"

if [ ${kubernetesMajorMinorVersion} -lt 122 ]; then
    checkWhitelistValue "ingress-v1beta1" "0.0.0.0/0"
else
    # kubernetes should have rejected resource creation
    checkIfNotExists "ingress" "default" "ingress-v1beta1"
fi