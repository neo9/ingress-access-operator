#!/bin/bash

helm repo add neo9charts https://charts.neo9.pro
helm repo update

# preview
helm --namespace ingress-acccess-operator diff upgrade ingress-acccess-operator neo9charts/n9-api --install --values ./default.yaml
sleep 10

# deploy
kubectl apply -f ../crds/
helm --namespace ingress-acccess-operator      upgrade ingress-acccess-operator neo9charts/n9-api --install --values ./default.yaml --create-namespace
