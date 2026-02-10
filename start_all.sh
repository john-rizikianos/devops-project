#!/bin/bash
docker compose up
minikube start
kubectl apply -f k8s/deployment.yaml
kubectl port-forward svc/mailhog 8026:8025 >> /dev/null

