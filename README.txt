author: ioannis rizikianos
date: 11 Feb 2026

#start all services:
docker compose up
minikube start
minikube get pods -w

#stop all services
minikube stop
docker compose down
