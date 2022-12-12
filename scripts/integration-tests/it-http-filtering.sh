#!/bin/bash

box $1

kubernetesMajorMinorVersion=$(kubectl version --short | grep 'Server Version' | awk -F':' '{print $2}' | sed 's/.*v\([0-9]*\)\.\([0-9]*\)\.\([0-9]*\).*/\1\2/')

checkWhitelistValue "demoingress1" "10.1.1.1/32,10.1.1.2/32,10.1.1.3/32"
haveAnnotation "ingress" "default" "demoingress1" '"forecastle.stakater.com/network-restricted":"true"'

checkWhitelistValue "demoingress1legacy" "10.1.1.1/32,10.1.1.2/32,10.1.1.3/32"
doNotHaveAnnotation "ingress" "default" "demoingress1legacy" "forecastle.stakater.com/network-restricted"

checkWhitelistValue "public" "0.0.0.0/0"
haveAnnotation "ingress" "default" "public" '"forecastle.stakater.com/network-restricted":"false"'

checkWhitelistValue "demoingress2" "10.1.1.1/32,10.1.1.2/32,10.1.1.3/32,10.1.2.1/32,10.1.2.2/32"

checkAlbWhitelistValue "albingress" "10.1.1.1/32,10.1.1.2/32,10.1.1.3/32,10.1.2.1/32,10.1.2.2/32"

kubectl ${kubeContextArgs} apply -f ./test-patch/visitorgroups/patch-visitorgroup-customer.yaml
sleep 3
checkWhitelistValue "demoingress2" "10.1.1.1/32,10.1.1.2/32,10.1.1.3/32,10.1.2.1/32,10.1.2.3/32"

kubectl ${kubeContextArgs} apply -f ./test-patch/ingresses/patch-ingress-2g.yaml
sleep 3
checkWhitelistValue "demoingress2" "10.1.2.1/32,10.1.2.3/32"

checkWhitelistValue "with-hardcoded-filter" "10.1.9.1/32"

checkWhitelistValue "with-hardcoded-filter-and-groups" "10.1.9.1/32"

checkWhitelistValue "without-indication" "10.1.3.1/32"

if [ ${kubernetesMajorMinorVersion} -lt 122 ]; then
    checkWhitelistValue "ingress-v1beta1" "10.1.3.1/32"
else
    # kubernetes should have rejected resource creation
    checkIfNotExists "ingress" "default" "ingress-v1beta1"
fi
