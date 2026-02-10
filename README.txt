jenkins admin user:
jenkadmin
jenkadmin

app page:
http://localhost:8081/products

start kubernetes:
# 1. Apply the Database (needs to start first)
kubectl apply -f k8s/database.yaml

# 2. Apply the Support Services
kubectl apply -f k8s/mailhog.yaml
kubectl apply -f k8s/keycloak.yaml

# 3. Apply the Main App
kubectl apply -f k8s/deployment.yaml


# get url
minikube service tomcat-service --url

#create self signed ssl certificate for app access from browser
minikube addons enable ingress

#generate cert
openssl req -x509 -newkey rsa:4096 -sha256 -nodes \
  -keyout tls.key -out tls.crt \
  -subj "/CN=bookstore.local" -days 365
  
  # upload file to kubernetes
  kubectl create secret tls bookstore-tls-secret \
  --cert=tls.crt \
  --key=tls.key
  
  #apply ingress.yaml
  kubectl apply -f k8s/ingress.yaml
  
  # View MailHog UI
  kubectl port-forward svc/mailhog 8026:8025
  #access:
  http://localhost:8026
