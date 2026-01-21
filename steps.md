#####Step 1: Create Dockerfile
FROM maven:3.9.6-eclipse-temurin-17
WORKDIR /app

COPY . .
RUN mvn clean package -DskipTests

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "target/app.jar"]

####Step 2: Start Minikube
->Start Minikube
minikube start

# Verify it's running
minikube status

# Set Docker environment to use Minikube's Docker daemon
eval $(minikube docker-env)

# Verify you're using Minikube's Docker
docker ps
Important: The eval $(minikube docker-env) command makes your terminal use Minikube's Docker, so images you build are directly available in Minikube!

Step 4: Build Docker Image
bash# Build the image (in Minikube's Docker)
docker build -t springboot-mongodb:1.0.0 .

# Verify image is built
docker images | grep springboot-mongodb

Step 5: Create Helm Chart Structure
bash# Create chart directory
mkdir -p springboot-mongodb-helm
cd springboot-mongodb-helm

# Create subdirectories
mkdir templates

# Create files
touch Chart.yaml values.yaml
touch templates/deployment.yaml
touch templates/service.yaml
touch templates/mongodb-deployment.yaml
touch templates/mongodb-service.yaml

Step 6: Create Helm Chart Files
Chart.yaml
yamlapiVersion: v2
name: springboot-mongodb
description: Spring Boot MongoDB Application
type: application
version: 1.0.0
appVersion: "1.0.0"

values.yaml
yaml# Application settings
app:
  name: springboot-mongodb
  replicaCount: 1

  image:
    repository: springboot-mongodb
    tag: "1.0.0"
    pullPolicy: Never  # Never pull, use local image

  service:
    type: NodePort
    port: 8083
    targetPort: 8083
    nodePort: 30083

  resources:
    limits:
      cpu: 500m
      memory: 512Mi
    requests:
      cpu: 250m
      memory: 256Mi

# MongoDB settings
mongodb:
  enabled: true
  name: mongodb
  image: mongo:latest
  port: 27017
  database: demo

  service:
    type: ClusterIP
    port: 27017

templates/deployment.yaml
yamlapiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ .Values.app.name }}
  labels:
    app: {{ .Values.app.name }}
spec:
  replicas: {{ .Values.app.replicaCount }}
  selector:
    matchLabels:
      app: {{ .Values.app.name }}
  template:
    metadata:
      labels:
        app: {{ .Values.app.name }}
    spec:
      containers:
      - name: {{ .Values.app.name }}
        image: "{{ .Values.app.image.repository }}:{{ .Values.app.image.tag }}"
        imagePullPolicy: {{ .Values.app.image.pullPolicy }}
        ports:
        - containerPort: {{ .Values.app.service.targetPort }}
        env:
        - name: SPRING_DATA_MONGODB_URI
          value: "mongodb://{{ .Values.mongodb.name }}-service:{{ .Values.mongodb.port }}/{{ .Values.mongodb.database }}"
        - name: SPRING_DATA_MONGODB_DATABASE
          value: {{ .Values.mongodb.database }}
        resources:
          limits:
            cpu: {{ .Values.app.resources.limits.cpu }}
            memory: {{ .Values.app.resources.limits.memory }}
          requests:
            cpu: {{ .Values.app.resources.requests.cpu }}
            memory: {{ .Values.app.resources.requests.memory }}
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: {{ .Values.app.service.targetPort }}
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: {{ .Values.app.service.targetPort }}
          initialDelaySeconds: 30
          periodSeconds: 5

templates/service.yaml
yamlapiVersion: v1
kind: Service
metadata:
  name: {{ .Values.app.name }}-service
  labels:
    app: {{ .Values.app.name }}
spec:
  type: {{ .Values.app.service.type }}
  ports:
  - port: {{ .Values.app.service.port }}
    targetPort: {{ .Values.app.service.targetPort }}
    nodePort: {{ .Values.app.service.nodePort }}
    protocol: TCP
    name: http
  selector:
    app: {{ .Values.app.name }}

templates/mongodb-deployment.yaml
yaml{{- if .Values.mongodb.enabled }}
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ .Values.mongodb.name }}
  labels:
    app: {{ .Values.mongodb.name }}
spec:
  replicas: 1
  selector:
    matchLabels:
      app: {{ .Values.mongodb.name }}
  template:
    metadata:
      labels:
        app: {{ .Values.mongodb.name }}
    spec:
      containers:
      - name: {{ .Values.mongodb.name }}
        image: {{ .Values.mongodb.image }}
        ports:
        - containerPort: {{ .Values.mongodb.port }}
        volumeMounts:
        - name: mongodb-data
          mountPath: /data/db
      volumes:
      - name: mongodb-data
        emptyDir: {}
{{- end }}

templates/mongodb-service.yaml
yaml{{- if .Values.mongodb.enabled }}
apiVersion: v1
kind: Service
metadata:
  name: {{ .Values.mongodb.name }}-service
  labels:
    app: {{ .Values.mongodb.name }}
spec:
  type: {{ .Values.mongodb.service.type }}
  ports:
  - port: {{ .Values.mongodb.service.port }}
    targetPort: {{ .Values.mongodb.port }}
    protocol: TCP
  selector:
    app: {{ .Values.mongodb.name }}
{{- end }}

Step 7: Add Health Check to Spring Boot
Make sure your pom.xml has actuator:
xml<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
Add to application.properties:
propertiesmanagement.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always

Step 8: Rebuild Docker Image
bash# Go back to project root
cd ..

# Rebuild with health check changes
docker build -t springboot-mongodb:1.0.0 .

Step 9: Install Application with Helm
bash# Install the Helm chart
helm install springboot-mongodb ./springboot-mongodb-helm

# Check installation
helm list

# Watch pods starting
kubectl get pods -w

Step 10: Verify Deployment
bash# Check all resources
kubectl get all

# Check pods are running
kubectl get pods

# Check services
kubectl get services

# View logs
kubectl logs -l app=springboot-mongodb

# Check MongoDB is running
kubectl logs -l app=mongodb

Step 11: Access the Application
Option 1: Using NodePort
bash# Get Minikube IP
minikube ip

# Access Swagger UI
# Replace <MINIKUBE_IP> with actual IP
echo "http://$(minikube ip):30083/swagger-ui/index.html"

# Open in browser
open "http://$(minikube ip):30083/swagger-ui/index.html"
Option 2: Using Minikube Service (Easier)
bash# This automatically opens the service in your browser
minikube service springboot-mongodb-service

# Or get the URL
minikube service springboot-mongodb-service --url
Option 3: Port Forwarding
bash# Forward port 8083 from pod to localhost:8083
kubectl port-forward service/springboot-mongodb-service 8083:8083

# Access at: http://localhost:8083/swagger-ui/index.html

Step 12: Test the API
Once you have the URL, test it:
bash# Get the URL
URL=$(minikube service springboot-mongodb-service --url)

# Test health endpoint
curl $URL/actuator/health

# Open Swagger UI in browser
open "$URL/swagger-ui/index.html"
```

---

## **Complete File Structure**
```
your-project/
├── src/
├── pom.xml
├── Dockerfile
├── .dockerignore
└── springboot-mongodb-helm/
    ├── Chart.yaml
    ├── values.yaml
    └── templates/
        ├── deployment.yaml
        ├── service.yaml
        ├── mongodb-deployment.yaml
        └── mongodb-service.yaml

Troubleshooting Commands
bash# Check pod status
kubectl get pods

# Describe pod (see events/errors)
kubectl describe pod <pod-name>

# View logs
kubectl logs <pod-name>

# Follow logs in real-time
kubectl logs -f <pod-name>

# Get into container shell
kubectl exec -it <pod-name> -- /bin/bash

# Check MongoDB connection from app pod
kubectl exec -it <app-pod-name> -- curl mongodb-service:27017

# Delete and reinstall
helm uninstall springboot-mongodb
helm install springboot-mongodb ./springboot-mongodb-helm

Common Issues and Fixes
Issue 1: ImagePullBackOff
bash# Make sure you're using Minikube's Docker
eval $(minikube docker-env)

# Rebuild image
docker build -t springboot-mongodb:1.0.0 .

# Verify pullPolicy is "Never" in values.yaml
Issue 2: CrashLoopBackOff
bash# Check logs for errors
kubectl logs <pod-name>

# Usually MongoDB connection issue
# Verify MongoDB is running
kubectl get pods | grep mongodb
Issue 3: Can't Access Swagger
bash# Check service
kubectl get svc springboot-mongodb-service

# Try port-forward instead
kubectl port-forward svc/springboot-mongodb-service 8083:8083

# Then access: http://localhost:8083/swagger-ui/index.html

Cleanup
bash# Uninstall Helm release
helm uninstall springboot-mongodb

# Verify everything is deleted
kubectl get all

# Stop Minikube
minikube stop

# Delete Minikube cluster
minikube delete

Quick Start Script
Create deploy.sh:
bash#!/bin/bash

echo "🚀 Starting deployment..."

# Use Minikube Docker
eval $(minikube docker-env)

# Build image
echo "📦 Building Docker image..."
docker build -t springboot-mongodb:1.0.0 .

# Install with Helm
echo "⚓ Installing with Helm..."
helm install springboot-mongodb ./springboot-mongodb-helm

# Wait for pods
echo "⏳ Waiting for pods to be ready..."
kubectl wait --for=condition=ready pod -l app=springboot-mongodb --timeout=120s

# Get URL
echo "✅ Deployment complete!"
echo "🌐 Access Swagger UI at:"
minikube service springboot-mongodb-service --url
echo "/swagger-ui/index.html"
Make it executable and run:
bashchmod +x deploy.sh
./deploy.sh

Follow these steps in order, and your Spring Boot app will be running on Minikube! Let me know which step you're on if you hit any issues! 🚀