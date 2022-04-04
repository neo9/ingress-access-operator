#!/bin/bash

box $1

checkIfExists "ingress" "default" "service-to-expose"
doNotHaveAnnotation "ingress" "default" "service-to-expose" "kubernetes.io/ingress.class"
haveFieldWithValue "ingress" "default" "service-to-expose" '{.spec.ingressClassName}' "nginx"

checkIfNotExists "ingress" "default" "service-not-to-expose"

kubectl ${kubeContextArgs} delete -f ../example-conf/services/service-to-expose.yaml
sleep 3

checkIfNotExists "ingress" "default" "service-to-expose"
