#!/bin/bash

helm repo add neo9charts https://charts.neo9.pro
helm repo update

# preview
helm --namespace gatekeeper diff upgrade gatekeeper neo9charts/n9-api --install --values ./values.yaml
sleep 10

# deploy
kubectl apply -f ../crds/
helm --namespace gatekeeper      upgrade gatekeeper neo9charts/n9-api --install --values ./values.yaml --create-namespace
