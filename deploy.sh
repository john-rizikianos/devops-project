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
nohup kubectl port-forward svc/keycloak 8087:8080 > /dev/null 2>&1 &
nohup kubectl port-forward svc/mailhog 8026:8025 > /dev/null 2>&1 &

echo "Deployment complete! Port forwarding active in background."
