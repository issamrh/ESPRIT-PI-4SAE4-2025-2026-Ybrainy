# YBrainy DevOps Sprint 3 — Master Guide
**Branch:** `3la-5atr-houssem` | **Last updated:** 2026-05-07

---

## SECTION 0: TOOLS AND TECHNOLOGY STACK

### Jenkins

Jenkins is an open-source automation server that runs your code pipeline automatically. Instead of manually compiling code, running tests, building Docker images, and deploying — Jenkins does all of that in sequence, automatically, every time code is pushed. It is the central orchestrator of our entire CI/CD process.

**In our setup:** Jenkins runs as a systemd service **inside WSL Ubuntu** (`/usr/bin/jenkins`, port 8086). It is accessible at `http://localhost:8086` from Windows because WSL forwards the port. Jenkins runs as the `jenkins` OS user inside WSL.

**Jobs we have and why:**

| Job | Purpose |
|-----|---------|
| `course-service-CI` | Compiles code, runs 91 unit tests, sends analysis to SonarQube |
| `course-service-CD` | Builds Docker image, imports to containerd, deploys to K8s |
| `enrollment-service-CI` | Same — 48 tests |
| `enrollment-service-CD` | Same — deploy |
| `lesson-service-CI` | Same — 63 tests |
| `lesson-service-CD` | Same — deploy |
| `quiz-service-CI` | Same — 62 tests |
| `quiz-service-CD` | Same — deploy |
| `ml-service-CI` | Runs pytest, sonar-scanner, builds Flask Docker image |
| `ml-service-CD` | Imports to containerd, deploys Flask ML pod |
| `frontend-CI` | Multi-stage Docker build (Node build + nginx serve) |
| `frontend-CD` | Imports to containerd, deploys Angular pod |

**How Jenkins connects to other tools:**
- **GitHub**: polls every minute via SCM trigger (`* * * * *`). When new commits are found it pulls the Jenkinsfile and executes it. (True webhook delivery is not possible since Jenkins is on localhost — polling is the equivalent.)
- **Docker**: Jenkins runs in WSL and calls `/usr/bin/docker` which talks to WSL's own Docker Engine socket (`unix:///var/run/docker.sock`). **NOT Docker Desktop.**
- **SonarQube**: CI pipelines run `mvn sonar:sonar` or `sonar-scanner` pointing at `http://localhost:9000` (which is kubectl port-forwarded from the sonarqube K8s pod).
- **Kubernetes**: CD pipelines call `kubectl` with a kubeconfig credential stored in Jenkins. The kubeconfig points at the K8s API server inside WSL (`https://172.22.108.68:6443`).

---

### Docker Engine (WSL native, NOT Docker Desktop)

Docker is a tool that packages an application and all its dependencies into a single portable unit called a **container**. The container runs identically on any machine, eliminating "works on my machine" problems.

**Docker Desktop vs Docker Engine — why we use Engine, not Desktop:**

Docker Desktop is a Windows GUI application that ships its own Linux VM to run containers. It has licensing restrictions for commercial use and is a heavyweight application. Our professor explicitly forbids it.

Docker Engine is the raw Docker daemon (`dockerd`) installed natively inside WSL Ubuntu via `apt install docker-ce`. It runs as a systemd service (`systemctl status docker`) and listens on `/var/run/docker.sock`. When Jenkins (which also runs in WSL) calls `docker build`, it talks directly to this socket — Docker Desktop is completely out of the execution path.

**Verification evidence:**
```bash
# Inside WSL as jenkins user:
docker context ls
# → NAME: default  ENDPOINT: unix:///var/run/docker.sock  (WSL's own dockerd)
# Docker Desktop context "desktop-linux" is NOT active for Jenkins

docker info | grep "Server Version"
# → Server Version: 29.0.1 (docker-ce, installed via apt)

systemctl is-active docker
# → active
```

**What a Dockerfile does:** A Dockerfile is a recipe. It starts from a base image, runs commands to install dependencies and compile code, and declares what command to run when the container starts. Our Spring Boot services use a single-stage Dockerfile with `maven:3.9-eclipse-temurin-21` as the base. The Angular frontend uses a **multi-stage** Dockerfile: Stage 1 uses Node 20 to run `ng build`, Stage 2 copies only the compiled output into a tiny nginx image.

**Where images are stored:** Images are stored locally inside WSL (`/var/lib/docker/`). After building, our CD pipeline also imports each image directly into containerd's namespace (`k8s.io`) so Kubernetes can use it with `imagePullPolicy: Never` without needing a registry.

**Naming convention:** All images follow `ybrainy/<service-name>:<build-number>`, e.g. `ybrainy/course-service:33`.

---

### Kubernetes and kubeadm — Full Explanation

**What is Kubernetes?**
Kubernetes (K8s) is a container orchestration platform. When you have 6 microservices (course, lesson, enrollment, quiz, ml-service, frontend), each packaged as a Docker container, you need something to: start them, restart them if they crash, connect them to each other via DNS, expose them on network ports, and manage resource limits. Kubernetes does all of this declaratively — you describe what you want in YAML files, and K8s figures out how to make it happen.

**What is kubeadm specifically?**
`kubeadm` is the official Kubernetes tool for bootstrapping a real cluster from scratch. It:
- Generates all TLS certificates for secure cluster communication
- Starts the control plane (API server, controller manager, scheduler, etcd)
- Configures RBAC and kubeconfig
- Joins worker nodes to the cluster

This is different from:
- **Minikube**: a single-node local cluster designed for development, simplified, not production-grade
- **kind**: runs K8s nodes as Docker containers — good for testing but not real infrastructure
- **k3s**: a lightweight K8s for edge/IoT
- **Managed K8s** (EKS, GKE, AKS): cloud-provider-managed clusters

We used `kubeadm` because it produces a real, production-grade cluster. The professor expects a serious setup, not a toy. Evaluators know the difference.

**How our cluster is structured:**
- **Single-node cluster**: 1 WSL Ubuntu instance serves as both the control plane AND the worker node
- **Control plane components** (all running as K8s static pods in `kube-system`):
  - `etcd`: the database that stores all cluster state
  - `kube-apiserver`: the REST API that kubectl and all other tools talk to
  - `kube-controller-manager`: ensures desired state matches actual state (restarts crashed pods)
  - `kube-scheduler`: decides which node runs each pod
- **Flannel**: the CNI (Container Network Interface) plugin that gives each pod an IP address and allows pods to talk to each other across the cluster

**Core Kubernetes concepts with our actual examples:**

| Concept | What it is | Our example |
|---------|-----------|------------|
| **Pod** | The smallest deployable unit. One or more containers that run together. | `course-service-86b466ddd6-27hmr` — one container, one JVM, port 8082 |
| **Deployment** | Manages replicas of a pod. If a pod crashes, Deployment restarts it. | `deployment.apps/course-service` — ensures 1 replica always runs |
| **Service** | A stable network endpoint for a set of pods. Gives pods a fixed DNS name. | `course-service.ybrainy.svc.cluster.local:8082` — other pods use this to talk to course-service |
| **ConfigMap** | Stores configuration as key-value pairs, mounted into pods as files | `prometheus-config` — the prometheus.yml scrape config |
| **Namespace** | A virtual cluster partition to group related resources | `ybrainy` — all our app services |

**The `ybrainy` namespace:**
We created a dedicated namespace to separate our app services from the monitoring stack (`monitoring` namespace) and system pods (`kube-system`). This is best practice: it makes `kubectl get pods -n ybrainy` show only app services, not K8s internals.

**How Kubernetes pulls Docker images:**
Our Jenkinsfile CD pipeline runs `docker save ... | ctr -n k8s.io images import -`. This imports the image directly into containerd's internal storage under the `k8s.io` namespace. Our K8s deployment YAMLs specify `imagePullPolicy: Never`, meaning K8s uses the locally imported image without attempting to pull from a registry. This is ideal for our local setup.

**How we deploy to K8s from Jenkins (the CD pipeline flow):**
```
Jenkins CD runs:
  1. docker build -t ybrainy/course-service:33 .
  2. docker save ybrainy/course-service:33 | sudo ctr -n k8s.io images import -
  3. kubectl apply -f deployment.yaml   (using kubeconfig credential)
  4. kubectl rollout status deployment/course-service --timeout=120s
```

**How services communicate inside K8s (DNS):**
Every Service gets a DNS name: `<service-name>.<namespace>.svc.cluster.local`. Our ML service uses `COURSE_SERVICE_URL=http://course-service.ybrainy.svc.cluster.local:8082`. This is how microservices find each other without hardcoded IPs.

**How we access services from outside the cluster (NodePort):**
Each service is exposed via NodePort — a fixed port on the WSL host that forwards traffic into the cluster:
```
Frontend:          172.22.108.68:30080  →  pod:80
Course Service:    172.22.108.68:30082  →  pod:8082
Quiz Service:      172.22.108.68:30083  →  pod:8083
Lesson Service:    172.22.108.68:30084  →  pod:8084
Enrollment:        172.22.108.68:30085  →  pod:8085
ML Service:        172.22.108.68:30086  →  pod:5000
Prometheus:        172.22.108.68:30090  →  pod:9090
SonarQube:         172.22.108.68:30900  →  pod:9000
Grafana:           172.22.108.68:30300  →  pod:3000
```

**What happens when a pod crashes:**
The Deployment controller detects the pod is dead and starts a new one automatically. Kubernetes has both `livenessProbe` (kills and restarts unhealthy containers) and `readinessProbe` (stops sending traffic until container is ready). Our services define both.

**Essential kubectl commands:**
```bash
kubectl get pods -n ybrainy                         # list all app pods
kubectl get pods -A                                  # all pods in all namespaces
kubectl describe pod <name> -n ybrainy              # diagnose a failing pod
kubectl logs <pod-name> -n ybrainy                  # see container logs
kubectl rollout restart deployment/<name> -n ybrainy # force redeploy
kubectl apply --validate=false -f deployment.yaml   # deploy a manifest
kubectl rollout status deployment/<name> -n ybrainy # wait for rollout to complete
```

---

### SonarQube

SonarQube is a static code analysis platform. It reads your source code without running it, and reports:
- **Bugs**: code that will likely cause a runtime error
- **Code smells**: code that works but is hard to maintain
- **Security hotspots**: patterns that could be exploited
- **Coverage**: what percentage of your code is executed by unit tests

**Integration with Jenkins:** Every CI pipeline runs `mvn sonar:sonar` (Spring Boot services) or `sonar-scanner` (ML service) as a pipeline stage. This sends analysis results to the SonarQube server at `http://localhost:9000` (port-forwarded from the K8s pod). Results are linked to the project by `sonar.projectKey`.

**Quality gates:** A quality gate is a pass/fail threshold. All 4 of our services pass the quality gate (status: OK), meaning they meet the minimum thresholds for bugs, coverage, and duplications.

**Code coverage (JaCoCo):** JaCoCo is a Java code coverage library. During `mvn clean verify`, JaCoCo instruments the bytecode and tracks which lines are executed by unit tests. It outputs a `jacoco.xml` report that SonarQube reads. Coverage means: "of all code lines, what % were exercised by at least one test."

**Current verified metrics (2026-05-07):**

| Service | Gate | Coverage | Tests | Bugs | Code Smells |
|---------|------|----------|-------|------|-------------|
| course-service | ✅ OK | 16.3% | 91 | 0 | 232 |
| enrollment-service | ✅ OK | 45.9% | 48 | 0 | 13 |
| lesson-service | ✅ OK | 32.1% | 63 | 0 | 0 |
| quiz-service | ✅ OK | 42.6% | 62 | 0 | 2 |

---

### Prometheus

Prometheus is a time-series metrics database. It works by **scraping** (HTTP GET) a `/metrics` endpoint on each service every 10-15 seconds, and storing the collected data as timestamped numbers. You can then query the data with PromQL to understand service health over time.

**How it collects metrics from our services:**
- Spring Boot services expose `/actuator/prometheus` automatically when `spring-boot-starter-actuator` and `micrometer-registry-prometheus` are on the classpath
- The Flask ML service exposes `/metrics` via `prometheus_flask_exporter`
- Prometheus's `prometheus.yml` lists every scrape target by DNS name

**Our scrape configuration (7 targets, all UP):**
```yaml
- course-service.ybrainy.svc.cluster.local:8082  /actuator/prometheus
- enrollment-service.ybrainy.svc.cluster.local:8085  /actuator/prometheus
- lesson-service.ybrainy.svc.cluster.local:8084  /actuator/prometheus
- quiz-service.ybrainy.svc.cluster.local:8083  /actuator/prometheus
- ml-service.ybrainy.svc.cluster.local:5000  /metrics
- 172.22.108.68:8086  /prometheus  (Jenkins)
- localhost:9090  /metrics  (Prometheus itself)
```

**Alert rules we configured (in `prometheus.yaml` ConfigMap, file: `rules.yml`):**
| Alert | Condition | Severity |
|-------|----------|----------|
| ServiceDown | `up{job=~"course|lesson|quiz|enrollment"} == 0` for 1 min | critical |
| HighMemoryUsage | JVM heap > 85% for 2 min | warning |
| HighErrorRate | HTTP 5xx rate > 0.1/s for 2 min | warning |
| SlowResponseTime | p95 latency > 2s for 3 min | warning |

**Note about the bug we fixed:** The original `prometheus.yaml` had `rule_files: - /etc/prometheus/rules/*.yml` but the ConfigMap mounts the file at `/etc/prometheus/rules.yml` (no subdirectory). This meant **zero alert rules were loading**. Fixed in commit `e558cb2` by changing the path to `/etc/prometheus/rules.yml`, then hot-reloading Prometheus via `POST /-/reload`.

---

### Grafana

Grafana is a visualization layer on top of Prometheus. Prometheus stores the data; Grafana draws the graphs. Grafana does not collect data itself — it queries Prometheus via PromQL.

**How it connects to Prometheus:** A provisioned datasource points at `http://prometheus.monitoring.svc.cluster.local:9090` — using K8s internal DNS. The datasource UID is `PBFA97CFB590B2093`.

**Dashboards we have (2 provisioned via ConfigMap):**

**Dashboard 1: "YBrainy Platform — Services Overview" (uid: ybrainy-main)**

| Panel | Query | What it shows |
|-------|-------|--------------|
| Services Up | `count(up{job=~"course\|lesson\|quiz\|enrollment"} == 1)` | Live count of running services (should be 4) |
| HTTP Requests/min | `sum(rate(http_server_requests_seconds_count{...}[1m])) * 60` | Total API traffic across all services |
| Error Rate | `sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))` | 5xx errors per second |
| Active Connections | `sum(tomcat_connections_current_connections{...})` | Live TCP connections |
| Request Rate graph | Per-service time series | Traffic breakdown by service over time |
| JVM Heap Memory | Per-service time series | Memory usage trends |
| p95 Latency | `histogram_quantile(0.95, rate(...[5m]))` | 95th percentile response time |
| JVM Threads | Per-service live threads | Thread pool usage |
| Service Health Table | `up{job=~"..."}` | Green/Red status for each service |

**Dashboard 2: "YBrainy ML Service — Predictions & Models" (uid: ybrainy-ml)**

| Panel | Query | What it shows |
|-------|-------|--------------|
| Total ML Predictions | `sum(flask_http_request_total{job='ml-service', status='200'})` | Cumulative successful predictions |
| Request Rate / min | Rate of ML API calls | Traffic to the ML service |
| p95 Latency | Histogram quantile of ML requests | How long predictions take |
| ML Service Status | `up{job='ml-service'}` | UP/DOWN live indicator |
| Predictions per Endpoint | Rate per path (/predict/conversion, /recommend, etc.) | Which ML models are being called |
| ML Latency per Endpoint | p95 per path | Which models are slow |

**Grafana alert rules (2 managed alerts, created via provisioning API):**
1. **"Service Pod Down"** — fires if `min(up{job=~"course|lesson|quiz|enrollment"}) < 1` for 1 minute. Severity: critical.
2. **"ML Service Down"** — fires if `up{job='ml-service'} < 1` for 1 minute. Severity: critical.

---

### Flask ML Service

The ML service is a Python Flask application serving 4 machine learning models:

| Model | Dataset | Input | Output |
|-------|---------|-------|--------|
| **DSO1** `POST /predict/conversion` | Student engagement data | Time spent, completion rate, quiz scores, videos watched, logins, forum reads | Conversion probability (LOW/MEDIUM/HIGH) + percentage |
| **DSO2** `POST /recommend` | Course catalog + KNN | Category, level, topN | Top N course recommendations with match scores |
| **DSO3** `POST /predict/quality` | Course structural features | Num lectures, duration, variety, rating | Quality label (HIGH/LOW) + factor breakdown + improvement tips |
| **DSO4** `GET /forecast/demand` | Enrollment time series | Steps (months) | Demand forecast with confidence intervals (SARIMA/ARIMA) |

**Why it has its own DevOps pipeline:** The ML service is Python (not Java/Maven) so it requires different tools: `pytest` for tests, `sonar-scanner` for analysis, `pip` for dependencies. It cannot share the Spring Boot CI pipeline.

**How prometheus_flask_exporter works:** Two lines of code:
```python
from prometheus_flask_exporter import PrometheusMetrics
metrics = PrometheusMetrics(app)
```
This automatically instruments every Flask route. Every HTTP request is recorded with: method, path, status code, and duration. The data is exposed at `/metrics` in Prometheus exposition format. Prometheus scrapes this endpoint every 10 seconds.

---

## SECTION 1: WHAT MUST BE RUNNING BEFORE THE PROFESSOR DEMO

| # | Service | Where it runs | Check command | Start command | Browser URL |
|---|---------|--------------|---------------|---------------|-------------|
| 1 | WSL Ubuntu | Windows | `wsl --list --running` | `wsl -d Ubuntu` | — |
| 2 | Docker Engine | WSL systemd | `wsl -u root bash -c "systemctl is-active docker"` | `wsl -u root bash -c "systemctl start docker"` | — |
| 3 | Kubernetes kubelet | WSL systemd | `wsl -u root bash -c "KUBECONFIG=/etc/kubernetes/admin.conf kubectl get nodes"` | `wsl -u root bash -c "systemctl restart kubelet"` | — |
| 4 | Jenkins | WSL systemd | `wsl -u root bash -c "systemctl is-active jenkins"` | `wsl -u root bash -c "systemctl start jenkins"` | http://localhost:8086 |
| 5 | All K8s pods | K8s ybrainy ns | `wsl -u root bash -c "KUBECONFIG=/etc/kubernetes/admin.conf kubectl get pods -n ybrainy"` | Pods auto-restart | — |
| 6 | SonarQube | K8s default ns | Same as above for `default` ns | Pod auto-restarts | http://172.22.108.68:30900 |
| 7 | Prometheus | K8s monitoring ns | `wsl -u root bash -c "KUBECONFIG=/etc/kubernetes/admin.conf kubectl get pods -n monitoring"` | Pod auto-restarts | http://172.22.108.68:30090 |
| 8 | Grafana | K8s monitoring ns | Same monitoring ns | Pod auto-restarts | http://172.22.108.68:30300 |

**One-click startup:** Run `.\START-EVERYTHING.ps1` from PowerShell as Administrator. It does all of the above and prints a health table.

---

## SECTION 2: PROFESSOR VALIDATION WALKTHROUGH

> **Computer just turned on. Nothing is running. Follow these steps exactly.**

---

### Step 1 — Open PowerShell as Administrator
Right-click the Start menu → "Windows PowerShell (Admin)" or "Terminal (Admin)"

---

### Step 2 — Run the startup script
**Terminal:** PowerShell (Admin)
```powershell
cd C:\Users\azizs\Downloads\ESPRIT-PI-4SAE4-2025-2026-Ybrainy
.\START-EVERYTHING.ps1
```
**Expected:** Table showing all services UP. Wait for it to complete (~60 seconds).
**Why:** This script starts Docker Engine inside WSL, verifies K8s is ready, checks Jenkins, and reloads Prometheus config.
**If it fails:** Check which service shows DOWN, then follow per-service steps below.

---

### Step 3 — Verify Docker Engine is WSL-native (not Docker Desktop)
**Terminal:** PowerShell
```powershell
wsl -u root bash -c "docker context ls"
```
**Expected output:**
```
NAME        DESCRIPTION                               DOCKER ENDPOINT
default *   Current DOCKER_HOST based configuration   unix:///var/run/docker.sock
```
**Why:** Proves the active Docker context for Jenkins is the WSL native socket, not Docker Desktop (`desktop-linux`). The `*` shows `default` is active for the jenkins user.

---

### Step 4 — Verify K8s cluster
**Terminal:** PowerShell
```powershell
wsl -u root bash -c "KUBECONFIG=/etc/kubernetes/admin.conf kubectl get nodes"
```
**Expected:**
```
NAME   STATUS   ROLES           AGE   VERSION
msi    Ready    control-plane   6d    v1.29.x
```
**If NotReady:** `wsl -u root bash -c "systemctl restart kubelet && sleep 15 && KUBECONFIG=/etc/kubernetes/admin.conf kubectl get nodes"`

---

### Step 5 — Show all running pods
**Terminal:** PowerShell
```powershell
wsl -u root bash -c "KUBECONFIG=/etc/kubernetes/admin.conf kubectl get pods -A"
```
**Expected:** Every pod `Running` or `Completed`. Key pods:
- `ybrainy`: course, enrollment, lesson, quiz, ml-service, frontend, mysql, rabbitmq
- `monitoring`: prometheus, grafana, alertmanager
- `default`: sonarqube

---

### Step 6 — Open Jenkins and show all green pipelines
**URL:** http://localhost:8086 (admin/admin)  
**Navigate to:** Dashboard. Show all 12 jobs (course/enrollment/lesson/quiz/ml-service/frontend × CI+CD), all green.  
Click `course-service-CI` → Last Build → Click a build number → Show "Build & Test", "SonarQube Analysis", "Trigger CD" stages all blue.  
**To trigger a live demo:** Click "Build Now" on any CI job, then watch it progress through stages.

---

### Step 7 — Open SonarQube and show quality gates
**URL:** http://172.22.108.68:30900 (admin/admin)  
**Navigate:** Projects → click each of your 4 services. Show:
- Quality Gate: Passed (green checkmark)
- Coverage percentage
- 0 bugs for all services
- **Note:** course-service has 232 code smells (style issues, not bugs — quality gate still passes)

---

### Step 8 — Open Prometheus and show targets + alert rules
**URL:** http://172.22.108.68:30090  
**Navigate:** Status → Targets. Show all 7 targets UP (green).  
**Then:** Alerts. Show the 4 alert rules loaded (ServiceDown, HighMemoryUsage, HighErrorRate, SlowResponseTime), all `inactive` (which is correct — means services are healthy).

---

### Step 9 — Open Grafana and show live dashboards
**URL:** http://172.22.108.68:30300 (admin/ybrainy2026)  
**Navigate:** Dashboards → "YBrainy Platform — Services Overview"  
- **"Services Up" panel** should show `4` (green background)
- **JVM Heap Memory graph** should show real lines, not "No data"
- **Service Health Table** should show 4 green UP rows  

**Then:** Dashboards → "YBrainy ML Service — Predictions & Models"  
- **ML Service Status** shows UP  
- Request rate panels may show low values (Kubernetes liveness probes hitting /health every 30s means 295+ requests already)

**Show alert rules:** Left sidebar → Alerting → Alert rules. Show "Service Pod Down" and "ML Service Down" rules.

---

### Step 10 — Show ML service responding
**URL:** http://172.22.108.68:30086/health
**Expected response:**
```json
{"models": {"dso1_conversion": true, "dso2_recommendations": true, "dso3_quality": true, "dso4_forecast": true}, "status": "ok"}
```
All 4 models loaded and ready.

**Show /metrics endpoint:** http://172.22.108.68:30086/metrics  
Prometheus-format text with `flask_http_request_total`, `flask_http_request_duration_seconds_*`, etc.

---

### Step 11 — Show Frontend
**URL:** http://172.22.108.68:30080  
Angular SPA should load. Nginx serves static files from the container.

---

## SECTION 3: PIPELINE STAGES EXPLAINED

### Spring Boot Services (course / enrollment / lesson / quiz)

**CI Pipeline stages (Jenkinsfile-CI):**

| Stage | Tool | What it does |
|-------|------|-------------|
| **Build & Test** | Maven `mvn clean verify` | Compiles Java, runs all JUnit tests with JaCoCo coverage, fails pipeline if any test fails. Publishes JUnit XML to Jenkins test report. |
| **SonarQube Analysis** | `mvn sonar:sonar` | Sends compiled bytecode, test results, and JaCoCo XML to SonarQube. Analyzes code for bugs, smells, security issues. |
| **Trigger CD** | Jenkins `build job:` | Asynchronously starts the CD pipeline, passing `BUILD_TAG=${BUILD_NUMBER}` as parameter. Pipeline does NOT wait for CD to finish. |

**CD Pipeline stages (Jenkinsfile-CD):**

| Stage | Tool | What it does |
|-------|------|-------------|
| **Docker Build** | `docker build` | Builds the Docker image from the service's Dockerfile, tags it with both the build number and `latest`. Runs inside WSL using WSL's Docker Engine. |
| **Import Image to Containerd** | `docker save ... \| ctr -n k8s.io images import -` | Saves the image as a tar stream and pipes it directly into containerd's `k8s.io` namespace. This makes it available to Kubernetes without a registry. |
| **Deploy to Kubernetes** | `kubectl apply` + `kubectl rollout status` | Applies the deployment YAML, then waits up to 120 seconds for the new pod to become Ready. If rollout fails, the stage fails. |

---

### ML Service (Jenkinsfile-CI / Jenkinsfile-CD)

**CI Pipeline stages:**

| Stage | Tool | What it does |
|-------|------|-------------|
| **Python Tests** | `pip3 install --break-system-packages` + `pytest` | Installs test dependencies into the system Python (Jenkins host has Python but no venv), runs all tests in `/tests/`, publishes JUnit XML. |
| **SonarQube Analysis** | `sonar-scanner` + `coverage` | Runs pytest with coverage tracking, exports XML, then scans `app.py` with sonar-scanner. |
| **Docker Build** | `docker build` | Builds the multi-layer Python image (python:3.11-slim base + all pip deps). |
| **Import Image to Containerd** | `ctr import` | Same as Spring Boot services. |
| **Trigger CD** | Jenkins `build job:` | Fires ml-service-CD. |

**CD Pipeline stages:**

| Stage | Tool | What it does |
|-------|------|-------------|
| **Import Image to Containerd** | `ctr import` | Re-imports (idempotent if already imported). |
| **Deploy to Kubernetes** | `kubectl apply` + `set image` + `rollout status` | Applies the manifest and forces rollout of the new image tag. |

---

### Frontend Angular (Jenkinsfile-CI / Jenkinsfile-CD)

**CI Pipeline stages:**

| Stage | Tool | What it does |
|-------|------|-------------|
| **Docker Build** | `docker build` (multi-stage) | Stage 1: `node:20-alpine` runs `npm install` + `ng build --configuration production`. Stage 2: copies `dist/angular-app/browser` into `nginx:1.27-alpine`. The final image is ~50MB. |
| **Import Image to Containerd** | `ctr import` | Same pattern. |
| **Trigger CD** | Jenkins `build job:` | Fires frontend-CD. |

**CD Pipeline stages:**

| Stage | Tool | What it does |
|-------|------|-------------|
| **Import Image to Containerd** | `ctr import` | Makes image available to K8s. |
| **Deploy to Kubernetes** | `kubectl apply` + `rollout status` | Deploys the nginx pod serving the compiled Angular app. |

---

## SECTION 4: EXCELLENCE WORK — HOW TO EXPLAIN AND DEMONSTRATE IT

### A. ML Service Full DevOps Stack

The ML service is a Python Flask REST API serving 4 trained machine learning models. It was fully integrated into the DevOps pipeline:

1. **Dockerized**: Multi-layer Dockerfile (`python:3.11-slim` base) builds in ~2 min. Image: `ybrainy/ml-service:latest`
2. **Deployed to K8s**: Pod `ml-service-5cdb9798d5-m4p5h` in `ybrainy` namespace, Running with liveness/readiness probes on `/health`
3. **Monitored**: `prometheus_flask_exporter` auto-instruments all routes. Prometheus scrapes `/metrics` every 10s.

**Show it live:**
```
http://172.22.108.68:30086/health            → all 4 models loaded
http://172.22.108.68:30086/metrics           → Prometheus metrics
http://172.22.108.68:30086/predict/conversion → POST with JSON body
http://172.22.108.68:30086/recommend          → POST for recommendations
```

**Sample request to show a prediction:**
```bash
curl -X POST http://172.22.108.68:30086/predict/conversion \
  -H "Content-Type: application/json" \
  -d '{"timeSpentOnCourse":120,"completionRate":0.8,"quizScores":85,"numberOfVideosWatched":15,"numLogins":20,"forumReads":5}'
```
**Expected response:**
```json
{"conversionLabel":"HIGH","conversionProbability":0.87,"percentage":87.0}
```

---

### B. Prometheus Alerting Rules

**URL:** http://172.22.108.68:30090/alerts  
**Navigation:** Status → Alerts

**4 rules, all currently `inactive` (services are healthy):**

| Rule | Fires when | Why it matters |
|------|-----------|----------------|
| **ServiceDown** | Any Spring Boot service is unreachable for > 1 min | The most critical alert — customer-facing service is down |
| **HighMemoryUsage** | JVM heap > 85% for > 2 min | Warns before OOM crash |
| **HighErrorRate** | HTTP 5xx > 0.1/s for > 2 min | API is returning errors to users |
| **SlowResponseTime** | p95 latency > 2s for > 3 min | User experience is degrading |

**3-sentence professor explanation:** "We added 4 Prometheus alerting rules that continuously evaluate PromQL expressions against live metrics from all 4 services. We fixed a misconfiguration where the rule_files path pointed to a non-existent subdirectory — rules were not loading at all before our fix. Once fixed and hot-reloaded via the Prometheus lifecycle API, all 4 rules became active and are routed through Alertmanager."

---

### C. Grafana Alert Rules

**URL:** http://172.22.108.68:30300  
**Navigation:** Alerting (bell icon) → Alert rules

**2 Grafana managed alerts:**
1. **"Service Pod Down"** — evaluates `min(up{job=~'course|lesson|quiz|enrollment'}) < 1` every minute
2. **"ML Service Down"** — evaluates `up{job='ml-service'} < 1` every minute

These are Grafana's own managed alerts (separate from Prometheus rules) — they appear in Grafana's alerting UI and can be linked to notification channels.

---

### D. Grafana Dashboards

**URL:** http://172.22.108.68:30300  
**Navigation:** Dashboards → Browse → YBrainy Platform folder

1. **YBrainy Platform — Services Overview**: 9 panels showing live JVM metrics, HTTP request rates, latency, connection counts, and service status for all 4 Spring Boot services.

2. **YBrainy ML Service — Predictions & Models**: 6 panels showing ML-specific metrics — prediction counts, request rate, p95 latency per endpoint, ML service status.

**All panels return real data** — verified by querying Prometheus directly:
- `count(up{job=~'...'} == 1)` → 4 ✅
- `jvm_memory_used_bytes{job='course-service'}` → 86,255,312 bytes ✅
- `up{job='ml-service'}` → 1 ✅

---

### E. ML Service Monitoring

**How prometheus_flask_exporter works:**
```python
from prometheus_flask_exporter import PrometheusMetrics
metrics = PrometheusMetrics(app)
```
These 2 lines automatically wrap every Flask request handler. After a request completes, the library increments `flask_http_request_total{method, path, status}` and records the duration in `flask_http_request_duration_seconds`. The `/metrics` endpoint is added automatically.

**Show data flowing live:**
1. Terminal: `curl http://172.22.108.68:30086/health` (makes a request)
2. Browser: `http://172.22.108.68:30086/metrics` — see counter increase
3. Browser: Prometheus `http://172.22.108.68:30090` → Graph → query `flask_http_request_total{job='ml-service'}` → see data points
4. Browser: Grafana ML dashboard → "ML Service Status" shows UP, request rate graphs update

---

## SECTION 5: SONARQUBE BEFORE/AFTER

**Verified current metrics (queried live from SonarQube API on 2026-05-07):**

| Service | Gate | Coverage | Tests | Pass Rate | Bugs | Code Smells | Security |
|---------|------|----------|-------|-----------|------|-------------|---------|
| **course-service** | ✅ OK | **16.3%** | 91 | 100% | 0 | 232 | 2 hotspots |
| **enrollment-service** | ✅ OK | **45.9%** | 48 | 100% | 0 | 13 | 0 |
| **lesson-service** | ✅ OK | **32.1%** | 63 | 100% | 0 | 0 | 0 |
| **quiz-service** | ✅ OK | **42.6%** | 62 | 100% | 0 | 2 | 1 hotspot |

**How to read the SonarQube dashboard:**
1. Open http://172.22.108.68:30900 (admin/admin)
2. Click "Projects" → click a service name
3. Main view shows: Quality Gate (top left), Coverage %, Bugs, Vulnerabilities, Code Smells
4. Click "Coverage" → see file-by-file breakdown of which lines are covered
5. Click "Code Smells" → see each issue with description and location

**Note on course-service 232 code smells:** These are style issues (e.g., long methods, unused variables) not bugs. The quality gate passes because the threshold is set to 0 bugs, not 0 smells. This is intentional — smells can be addressed over time.

---

## SECTION 6: GITHUB ACTIONS — DO I USE THEM?

**YES — 2 GitHub Actions workflows exist:**

**File 1:** `.github/workflows/quiz-service-ci.yml`
- Trigger: push to `main` or `3la-5atr-houssem` branches, or PR, when Quiz service files change
- Jobs: `ci` (JDK 17 setup → `mvn clean verify` → Docker build+push to `swaggyo/quiz-service` on Docker Hub) and `cd` (kubectl deploy — runs only on push to `main`)
- **Note:** The CD job targets the public cluster but requires `KUBECONFIG_B64` and `DOCKERHUB_TOKEN` secrets to be set in GitHub repository settings.

**File 2:** `.github/workflows/enrollment-service-ci-cd.yml`
- Trigger: push to `main` or `3la-5atr-houssem` branches when Enrollment service files change
- Jobs: Same pattern — CI builds and pushes to `swaggyo/enrollment-service` on Docker Hub, CD deploys to K8s
- **Note:** Same secret requirements.

**These GitHub Actions are complementary to Jenkins, not replacements.** The Jenkins pipelines are the primary CI/CD system used for the professor demo. The GitHub Actions workflows run on GitHub's cloud runners (`ubuntu-latest`) and would push to Docker Hub — useful if the cluster had internet access for image pulling.

---

## SECTION 7: FREQUENTLY ASKED QUESTIONS

**Q: What is the difference between your CI pipeline and your CD pipeline?**

A: CI (Continuous Integration) is about verifying the code is correct: it compiles the code, runs all unit tests, and sends analysis to SonarQube. If any test fails, the pipeline stops and nothing gets deployed. CD (Continuous Deployment) is triggered only after CI succeeds: it builds the Docker image, imports it into containerd, and updates the Kubernetes deployment with the new image. Separating them means a failing test never reaches production.

**Q: Why did you use Jenkins and not GitHub Actions?**

A: GitHub Actions runs on GitHub's cloud runners, which cannot access our local Kubernetes cluster (running inside WSL on a laptop). Jenkins runs inside WSL alongside the cluster, so it has direct access to the Docker socket, kubectl, and the K8s API server. We actually have both — 2 GitHub Actions workflows exist for quiz-service and enrollment-service — but Jenkins is the primary system for demo because it can actually deploy to our local cluster.

**Q: Why Docker Engine in WSL instead of Docker Desktop?**

A: Our professor explicitly forbade Docker Desktop. Beyond that, Docker Desktop is a heavyweight Windows application with commercial licensing restrictions. Docker Engine installed natively via `apt install docker-ce` inside WSL is lighter, runs as a standard Linux systemd service, and is what you would use in production Linux environments. Jenkins in WSL uses the local Docker socket (`unix:///var/run/docker.sock`) — Docker Desktop is not in the execution path at all.

**Q: How does SonarQube integrate with your Jenkins pipeline?**

A: Every CI Jenkinsfile has a "SonarQube Analysis" stage. For Spring Boot services, this runs `mvn sonar:sonar` with the SonarQube server URL and authentication token. Maven compiles the code, JaCoCo generates a coverage XML report, and then the sonar-scanner (embedded in the Maven plugin) uploads all results to the SonarQube server. For the ML service, we run `python -m coverage xml` then call `sonar-scanner` directly.

**Q: What happens when you push code to GitHub?**

A: Jenkins polls GitHub every minute (the SCM trigger is set to `* * * * *`). When it detects new commits on branch `3la-5atr-houssem`, it pulls the Jenkinsfile and runs the pipeline automatically. Within ~1 minute of a push, the CI pipeline starts. If CI passes (tests green, sonar sends), it triggers the CD pipeline which builds a new Docker image and deploys to Kubernetes. Additionally, if the push includes changes to the Quiz or Enrollment service paths, the corresponding GitHub Actions workflow also triggers on GitHub's cloud.

**Q: How does Kubernetes know to pull the new Docker image after a CD pipeline runs?**

A: We don't pull from a registry — we import directly. The CD pipeline runs `docker save <image> | ctr -n k8s.io images import -` which loads the image into containerd's storage on the node. Then `kubectl set image deployment/<name> <container>=<image>:<tag>` tells Kubernetes to roll out the new version. Because `imagePullPolicy: Never` is set in the deployment YAML, Kubernetes uses the locally stored image without trying to contact any registry.

**Q: What is kubeadm and why did you use it?**

A: `kubeadm` is the official Kubernetes tool for bootstrapping a production-grade cluster. It handles generating TLS certificates, starting all control plane components, and configuring networking. We used it because it creates a real cluster, not a development toy. Minikube or kind would work locally but they're simplified — kubeadm is what's used in real deployments, and professors evaluating serious DevOps projects expect it.

**Q: How is kubeadm different from minikube?**

A: Minikube runs a pre-configured, simplified single-node cluster optimized for local development — it manages its own VM/container, auto-configures everything, and has shortcuts for common tasks. Kubeadm is the production bootstrapper: it doesn't simplify anything, it sets up each component exactly as it would run in production, requiring you to configure networking (we installed Flannel manually), configure kubelet, and manage your own kubeconfig. Minikube is to kubeadm what XAMPP is to manually configuring Apache.

**Q: How many nodes does your K8s cluster have and where do they run?**

A: One node: the WSL Ubuntu instance, which serves as both the control plane and the worker node. This is a valid single-node kubeadm setup. In production you'd have separate control plane and worker nodes for high availability, but for our project one node is sufficient. The node is named `msi` and runs all 19 pods (including system pods).

**Q: What is Prometheus and what does it actually monitor in your project?**

A: Prometheus is a time-series database that collects metrics by periodically sending HTTP GET requests to `/actuator/prometheus` (Spring Boot) or `/metrics` (Flask) endpoints. For our project it monitors: JVM heap memory, HTTP request rate and latency, thread counts, active connections, and service uptime for all 4 Spring Boot services plus the ML Flask service. It also scrapes Jenkins itself for build metrics.

**Q: What is Grafana and what can you see on your dashboards?**

A: Grafana queries Prometheus's data and draws it as graphs and gauges in a web UI. Our "Services Overview" dashboard shows: how many services are currently up, total HTTP traffic across all services, 5xx error rate, JVM memory trends, p95 response latency, and a service health table with green/red status. Our "ML Service" dashboard shows prediction volume, request rates per ML endpoint, and response time for each model.

**Q: What are your Prometheus alerting rules and what do they alert on?**

A: We have 4 Prometheus alert rules and 2 Grafana managed alerts. The Prometheus rules fire for: service down (any Spring Boot service unreachable for >1 min), high JVM heap (>85% for >2 min), high HTTP error rate (>0.1 errors/sec for >2 min), and slow response time (p95 >2s for >3 min). The Grafana rules fire for: any service pod down, and the ML service down specifically. All 6 are currently inactive because all services are healthy.

**Q: What is the ML service and why does it have its own DevOps pipeline?**

A: The ML service is a Python Flask API serving 4 trained scikit-learn/statsmodels models: student conversion prediction (DSO1), course recommendation (DSO2), course quality prediction (DSO3), and enrollment demand forecasting (DSO4). It has its own CI/CD pipeline because it's a different technology stack — Python instead of Java, pytest instead of JUnit, sonar-scanner directly instead of Maven plugin, and a different Dockerfile pattern.

**Q: What does prometheus_flask_exporter do and how did you add it?**

A: It's a Python library that automatically instruments a Flask app for Prometheus. Two lines of code add it: `from prometheus_flask_exporter import PrometheusMetrics` and `metrics = PrometheusMetrics(app)`. It hooks into Flask's request lifecycle and records HTTP method, path, status code, and duration for every request. It adds a `/metrics` endpoint that returns all collected data in Prometheus exposition format. We added it to `requirements.txt` and the `app.py` already includes it.

**Q: What is a Docker image and where are your images stored?**

A: A Docker image is a read-only, layered file system snapshot. Each `RUN` instruction in a Dockerfile adds a layer. When you run a container from an image, a writable layer is added on top. Our images are stored in two places: locally in WSL's Docker daemon storage (`/var/lib/docker`), and imported into containerd (`/var/lib/containerd`) for Kubernetes to use.

**Q: What is a Kubernetes namespace and why do you use ybrainy?**

A: A namespace is a virtual partition inside a Kubernetes cluster. Resources in different namespaces are isolated — pods in `ybrainy` can't accidentally affect pods in `monitoring`. We use three namespaces: `ybrainy` for all application services, `monitoring` for Prometheus/Grafana/Alertmanager, and `kube-system` for K8s internals. This makes `kubectl get pods -n ybrainy` show only our app, not system noise.

**Q: How do your microservices communicate inside Kubernetes?**

A: Kubernetes gives every Service a DNS name: `<service-name>.<namespace>.svc.cluster.local`. The ML service uses `COURSE_SERVICE_URL=http://course-service.ybrainy.svc.cluster.local:8082` to call the course API. This DNS resolution is handled by CoreDNS (the K8s DNS server). Services use ClusterIP type for internal communication — they don't need to be exposed on a NodePort for inter-pod traffic.

**Q: What would happen if one of your pods crashed? How would you know?**

A: Kubernetes's Deployment controller detects the pod is gone and immediately schedules a replacement. Typically the pod restarts within 5-10 seconds. You would know because: (1) Prometheus detects `up == 0` and fires the `ServiceDown` alert after 1 minute, (2) Grafana's "Service Pod Down" alert rule fires, (3) the "Services Up" dashboard panel drops from 4 to 3. You can see restarts in `kubectl get pods -n ybrainy` — non-zero RESTARTS column.

**Q: What is code coverage and why is yours at X%?**

A: Code coverage measures what percentage of your production code is exercised by at least one unit test. JaCoCo instruments the compiled bytecode and tracks which lines execute during tests. Our coverage ranges from 16% (course-service, which has complex legacy code and many endpoints) to 46% (enrollment-service). We added comprehensive test suites in Sprint 3 — before that, coverage was below 10%. The quality gate passes with these values. Higher coverage is always better but takes proportionally more time to write.

**Q: How did you fix the CrashLoopBackOff on enrollment and lesson services?**

A: The root cause was a cgroup driver mismatch: containerd was configured for `cgroupfs` but kubelet expected `systemd`. This meant containers couldn't start correctly. Fix: modified `/etc/containerd/config.toml` to set `SystemdCgroup = true`, then restarted containerd and kubelet. The fix script is at `DEVOPS/fix-containerd-cgroup.sh`.

**Q: What is a Webhook and do you use real webhooks or polling? Why?**

A: A webhook is when GitHub sends an HTTP POST to your server the instant a push happens. Polling is when Jenkins asks GitHub "anything new?" every N seconds. We use **SCM polling every 1 minute** (`* * * * *`), not real webhooks. The reason: Jenkins runs at `localhost:8086` which is not publicly accessible from GitHub's servers — there's no way for GitHub to reach it. Polling achieves the same result with at most 1 minute delay, which is acceptable for a local development environment.

**Q: What excellence work did you do beyond the requirements?**

A: 1) Full DevOps stack for the Flask ML service (Dockerfile, K8s deployment, Jenkins CI+CD, Prometheus metrics, Grafana ML dashboard with 6 panels). 2) Fixed a Prometheus misconfiguration where alert rules were not loading at all (wrong rule_files path). 3) Created Grafana managed alerting rules via the provisioning API. 4) Deployed the Angular frontend to Kubernetes with a multi-stage Docker build and nginx reverse proxy with K8s DNS resolver fix. 5) Comprehensive alert coverage: 4 Prometheus rules + 2 Grafana alerts covering 6 failure scenarios.

---

## SECTION 8: GRILLE D'ÉVALUATION — HONEST SELF-ASSESSMENT

### GROUP (10 points)

**Jenkins & Webhooks — /3**

| Criterion | State | Evidence | Score | Risk |
|-----------|-------|----------|-------|------|
| CI pipeline triggers on push | ✅ SCM polling every 1 min | `* * * * *` SCM trigger in all CI job configs | ~2.5/3 | Polling ≠ webhook. State clearly it's polling due to localhost constraint. |
| CD auto-triggers after CI | ✅ `build job: 'X-CD'` in every CI | All CI Jenkinsfiles have Trigger CD stage | ✅ included above | |
| All pipelines green | ✅ 12/12 green | Verified via Jenkins API | ✅ included above | |

**Likely score: 2.5–3/3**. Professor may deduct 0.5 for polling vs real webhook. Explain the technical reason.

---

**Docker — /1.5**

| Criterion | State | Evidence | Score |
|-----------|-------|----------|-------|
| Backend images built & in K8s | ✅ course, enrollment, lesson, quiz, ml-service all Running | `kubectl get pods -n ybrainy` shows 5 app pods Running | 0.75/0.75 |
| Frontend image built & in K8s | ✅ Angular multi-stage build, nginx pod Running | `frontend-857d6f67c6-qlkcj 1/1 Running` | 0.75/0.75 |

**Likely score: 1.5/1.5** — both backend and frontend images built and deployed.

---

**Kubernetes Orchestration — /2.5**

| Criterion | State | Evidence |
|-----------|-------|----------|
| All pods Running in ybrainy | ✅ 8 pods Running, 0 restarts | `kubectl get pods -n ybrainy` |
| Monitoring stack running | ✅ Prometheus, Grafana, Alertmanager | `kubectl get pods -n monitoring` |
| kubeadm used | ✅ Real cluster bootstrapped | `kubeadm` is the tool, cluster has full control plane |

**Likely score: 2.5/2.5**

---

**SonarQube — /1.5**

| Criterion | State | Evidence |
|-----------|-------|----------|
| Sonar stage in Jenkinsfile | ✅ All 5 CI pipelines | `mvn sonar:sonar` or `sonar-scanner` stage |
| Quality gates passing | ✅ All 4 services OK | Verified via API |
| Coverage meaningful | 16–46% | Verified via API, 100% test pass rate |

**Likely score: 1.25–1.5/1.5**. Course-service 16.3% is low — be ready to explain why (large codebase with complex controllers).

---

**Grafana/Prometheus — /1.5**

| Criterion | State | Evidence |
|-----------|-------|----------|
| Prometheus targets UP | ✅ 7/7 UP | `/api/v1/targets` |
| Dashboards with live data | ✅ Both dashboards return real data | Verified via PromQL queries |
| Alert rules | ✅ 4 Prometheus + 2 Grafana | `/api/v1/rules` + Grafana provisioning API |

**Likely score: 1.5/1.5** — fixed the rule_files bug, data is live, alerts configured.

---

### INDIVIDUAL (10 points)

**My Pipeline + Tests — /4**

| Service | CI Job | Last Build | Tests | Pass Rate |
|---------|--------|-----------|-------|-----------|
| course-service | ✅ #33 SUCCESS | ✅ | 91 | 100% |
| enrollment-service | ✅ #16 SUCCESS | ✅ | 48 | 100% |
| lesson-service | ✅ #18 SUCCESS | ✅ | 63 | 100% |
| quiz-service | ✅ #17 SUCCESS | ✅ | 62 | 100% |

**Likely score: 3.5–4/4** — all pipelines green, all tests passing. Ready to explain any test, stage, or config.

**Maitrise Q&A — /4**

Use Section 7 (FAQ) to prepare answers to every likely professor question. Recommended points to emphasize:
- The Docker Desktop vs Docker Engine distinction — this is a specific constraint and showing you understand it earns points
- Why kubeadm over minikube — use the XAMPP analogy
- How the CI→CD chain works and what stops a broken build from reaching K8s
- The Prometheus rule_files bug you found and fixed — shows debugging skill

**Likely score: depends on answers. Target: 3.5–4/4** with Section 7 preparation.

**Excellence — /2**

| Item | State | Score impact |
|------|-------|-------------|
| ML service full DevOps stack | ✅ Running, monitored | +0.75 |
| Prometheus alert rules (fixed bug) | ✅ 4 rules active | +0.25 |
| Grafana managed alerts | ✅ 2 rules | +0.25 |
| Angular frontend K8s deploy | ✅ Running | +0.5 |
| Grafana ML dashboard | ✅ 6 panels, live data | +0.25 |

**Likely score: 1.5–2/2**

---

### ESTIMATED TOTAL

| Category | Estimated | Max |
|----------|-----------|-----|
| Group — Jenkins & Webhooks | 2.5 | 3 |
| Group — Docker | 1.5 | 1.5 |
| Group — Kubernetes | 2.5 | 2.5 |
| Group — SonarQube | 1.25 | 1.5 |
| Group — Grafana/Prometheus | 1.5 | 1.5 |
| **Group Total** | **9.25** | **10** |
| Individual — Pipeline + Tests | 3.75 | 4 |
| Individual — Q&A | 3.5 | 4 |
| Individual — Excellence | 1.75 | 2 |
| **Individual Total** | **9.0** | **10** |
| **COMBINED ESTIMATE** | **~18/20** | **20** |

> Ranges to +/- 1.5 depending on how the professor weighs webhooks vs polling and Q&A performance.
