#!/bin/bash
cd /home/johnuntu/Documents/hua/devops/project-devops

echo "Stopping old containers..."
docker compose down

echo "Pulling latest image..."
docker compose pull


echo "Starting new version..."
docker compose up -d

# clean up
docker image prune -f
#port forwarding
kubectl port-forward svc/keycloak 8087:8080 &
kubectl port-forward svc/mailhog 8026:8025 &
