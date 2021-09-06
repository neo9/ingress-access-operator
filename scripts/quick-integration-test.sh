#!/bin/bash

set -x

if [ ! -z "$1" ]; then
    kubeContextArgs="--context=$1"
fi

function checkWhitelistValue() {
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

function checkIfExists() {
  resourceType=$1
  namespace=$2
  name=$3
  existingResource=$(kubectl get $resourceType --no-headers $name -n $namespace -o custom-columns=":metadata.name")
  if [ "${existingResource}" == "${name}" ]; then
      echo "assertion ok"
  else
      echo "Unexpected value for type $resourceType"
      echo "${existingResource}"
      echo "${name}"
      exit 1
  fi
}

function checkIfNotExists() {
  resourceType=$1
  namespace=$2
  name=$3
  existingResource=$(kubectl get $resourceType --no-headers $name -n $namespace -o custom-columns=":metadata.name")
  if [ "${existingResource}" != "${name}" ]; then
      echo "assertion ok"
  else
      echo "Unexpected value for type $resourceType"
      echo "${existingResource}"
      echo "${name}"
      exit 1
  fi
}

# operator filtering

kubectl ${kubeContextArgs} apply -f ../example-conf/
sleep 3
checkWhitelistValue "demoingress2" "10.1.1.1/32,10.1.1.2/32,10.1.1.3/32,10.1.2.1/32,10.1.2.2/32"

kubectl ${kubeContextArgs} apply -f ./test-patch/patch-visitorgroup-customer.yaml
sleep 3
checkWhitelistValue "demoingress2" "10.1.1.1/32,10.1.1.2/32,10.1.1.3/32,10.1.2.1/32,10.1.2.3/32"

kubectl ${kubeContextArgs} apply -f ./test-patch/patch-ingress-2g.yaml
sleep 3
checkWhitelistValue "demoingress2" "10.1.2.1/32,10.1.2.3/32"

checkWhitelistValue "xposer-demoingress" "10.1.1.1/32,10.1.1.2/32,10.1.1.3/32"

checkWhitelistValue "xposer-not-watched-ingress" "10.1.9.1/32"

checkWhitelistValue "not-watched-ingress" "10.1.9.1/32"

# istio sidecar

checkIfExists "sidecar" "nginx-istio-ingress" "ingress"

# ingress

checkIfExists "ingress" "default" "service-to-expose"
checkIfNotExists "ingress" "default" "service-not-to-expose"

kubectl ${kubeContextArgs} delete -f ../example-conf/service-to-expose.yaml
sleep 3

checkIfNotExists "ingress" "default" "service-to-expose"
