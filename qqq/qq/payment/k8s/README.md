# Kubernetes (local/dev)

These manifests deploy the **Cart / Payment / Finance** services plus **MySQL**, **RabbitMQ**, **Eureka**, and **API Gateway**.

Notes:
- Images are referenced as `:latest` and use `imagePullPolicy: IfNotPresent` for local clusters (minikube / kind).
- Build/load images into your cluster before applying (or update `image:` to point to your registry).

Apply:
1) `kubectl apply -f k8s/namespace.yaml`
2) `kubectl apply -f k8s/mysql.yaml`
3) `kubectl apply -f k8s/rabbitmq.yaml`
4) `kubectl apply -f k8s/jaeger-zipkin.yaml`
5) `kubectl apply -f k8s/eureka.yaml`
6) `kubectl apply -f k8s/api-gateway.yaml`
7) `kubectl apply -f k8s/cart.yaml`
8) `kubectl apply -f k8s/payment.yaml`
9) `kubectl apply -f k8s/finance.yaml`

---

# Jenkins (WSL) pipeline

This repo includes a Jenkins pipeline that:
- Builds Docker images for `cart-service`, `payment-service`, `finance-service`
- Optionally loads images into `kind`/`minikube` (for local clusters)
- Applies the manifests in `k8s/` and waits for rollouts

Files:
- `k8s/Jenkinsfile` (Jenkins declarative pipeline)
- `k8s/ci/build-images.sh`
- `k8s/ci/deploy.sh`

## 1) Jenkins prerequisites (inside WSL)

Install on WSL (Ubuntu recommended):
- Docker Engine (or Docker Desktop + WSL integration)
- `kubectl`
- `kind` or `minikube` (if you use `CLUSTER_TYPE=kind|minikube`)

## 2) Run Jenkins in Docker (recommended)

From WSL:
- Start Jenkins (mount Docker socket so the job can run `docker build`, and mount kubeconfig so it can run `kubectl`):

  ```bash
  docker volume create jenkins_home
  docker run -d --name jenkins \
    -p 8080:8080 -p 50000:50000 \
    -v jenkins_home:/var/jenkins_home \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -v $HOME/.kube:/var/jenkins_home/.kube:ro \
    jenkins/jenkins:lts-jdk17
  ```

- Open Jenkins UI: `http://localhost:8080`
- Get the initial admin password:

  ```bash
  docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
  ```

Install plugins when prompted:
- `Pipeline`
- `AnsiColor` (used by `k8s/Jenkinsfile`)

## 3) Create the pipeline job (where to put the pipeline)

In Jenkins:
1) **Dashboard** → **New Item**
2) Name: `payment-k8s` (any name is fine)
3) Type: **Pipeline**
4) In **Pipeline** section choose one of:
   - **Pipeline script from SCM** (recommended)
     - SCM: `Git`
     - Repository URL: your repo
     - Script Path: `k8s/Jenkinsfile`
   - **Pipeline script**
     - Paste the contents of `k8s/Jenkinsfile`

Then **Save**.

## 4) Run the job

Click **Build with Parameters** and set:
- `CLUSTER_TYPE`
  - `minikube` if you run minikube on the same machine as Jenkins (recommended default)
  - `kind` if you run kind on the same machine as Jenkins
  - `none` if images are already in a registry (set `DOCKER_REGISTRY`) or the cluster can pull them
- `IMAGE_TAG` (default `latest`)
- `DOCKER_REGISTRY` (optional)
  - Leave empty for local `kind`/`minikube` image loading
  - Set to something like `ghcr.io/<org>` (requires docker login on the Jenkins agent)
- `DOCKER_REGISTRY_CREDENTIALS_ID` (optional)
  - Create a **Username with password** credential in Jenkins (**Dashboard → Manage Jenkins → Credentials**) and put its `credentialsId` here to let the pipeline run `docker login` automatically.

## 5) Notes / common gotchas

- `k8s/eureka.yaml` and `k8s/api-gateway.yaml` reference images (`ybrainy-eureka:latest`, `ybrainy-api-gateway:latest`) that are not built by this repo’s pipeline. Make sure those images exist in your cluster (loaded) or in your registry.
- If Jenkins runs in Docker and cannot reach your cluster, verify the mounted kubeconfig (`$HOME/.kube`) has the right context and certificates.
