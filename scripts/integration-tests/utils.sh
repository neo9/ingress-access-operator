#!/bin/bash

function box(){
	t="$1xxxx";c=${2:-=}; echo ${t//?/$c}; echo "$c $1 $c"; echo ${t//?/$c};
}

function checkWhitelistValue() {
    ingressName=$1
    expectation=$2
    ips=$(kubectl ${kubeContextArgs} get ingress ${ingressName} -o yaml | grep 'whitelist-source-range' | grep -v 'f:' | awk '{print $2}')
    if [ ${ips} == ${expectation} ]; then
        echo "assertion ok"
    else
        echo "Unexpected value for ingress ${ingressName}"
        echo "${ips}"
        echo "${expectation}"
        exit 1
    fi
}

function checkServiceWhitelistValue() {
    serviceName=$1
    expectation=$2
    ips=$(kubectl ${kubeContextArgs} get service ${serviceName} -o=jsonpath="{.spec.loadBalancerSourceRanges}")
    if [ ${ips} == ${expectation} ]; then
        echo "assertion ok"
    else
        echo "Unexpected value for services ${serviceName}"
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

function doNotHaveAnnotation() {
    kind=$1
    namespace=$2
    name=$3
    annotation=$4
    annotations=$(kubectl ${kubeContextArgs} -n ${namespace} get ${kind} ${name} -o jsonpath='{.metadata.annotations}')
    if echo "$annotations" | grep -vq "$annotation";then
        echo "assertion ok"
    else
        echo "Found annotation"
        exit 1
    fi
}

function haveFieldWithValue() {
  kind=$1
  namespace=$2
  name=$3
  fieldPath=$4
  expectedValue=$5
  currentValue=$(kubectl ${kubeContextArgs} -n ${namespace} get ${kind} ${name} -o jsonpath="$fieldPath")
  if [ "${expectedValue}" == "${currentValue}" ]; then
      echo "assertion ok"
  else
      echo "Found not expected value for field $fieldPath"
      echo "$currentValue instead of $expectedValue"
      exit 1
  fi
}
