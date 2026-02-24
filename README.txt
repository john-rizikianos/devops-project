author: ioannis rizikianos
date: 11 Feb 2026

#start all services:
docker compose up
minikube start
minikube get pods -w

#stop all services
minikube stop
docker compose down

docker urls:
http://localhost:8081/products
http://localhost:8025/
http://localhost:8180/

k8s urls:
https://bookstore.uat/
https://bookstore.uat/products
https://auth.bookstore.uat/
