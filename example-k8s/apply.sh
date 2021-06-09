#!/bin/bash

kubectl apply -f base-manifest.yaml

helm --namespace gatekeeper diff upgrade gatekeeper neo9charts/n9-api  --install --values ./values.yaml
sleep 10
helm --namespace gatekeeper      upgrade gatekeeper neo9charts/n9-api --install --values ./values.yaml

