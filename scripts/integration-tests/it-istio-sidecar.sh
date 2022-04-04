#!/bin/bash

box $1

checkIfExists "sidecar" "nginx-istio-ingress" "ingress"
