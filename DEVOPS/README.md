# YBrainy DevOps — Status & Guide

**Student:** Mohamed Aziz Selmi  
**Branch:** `3la-5atr-houssem`  
**Platform:** WSL2 (Ubuntu 24.04) on Windows 11, kubeadm single-node cluster

---

## What Was Built

This sprint covers the full DevOps stack for the YBrainy e-learning platform:
4 Spring Boot microservices fully containerized, deployed to Kubernetes, with automated CI/CD, code quality scanning, and real-time monitoring — all running locally in WSL2.

---

## Architecture Overview

```
Windows 11
└── WSL2 (Ubuntu 24.04) — the entire DevOps stack lives here
    ├── Jenkins  (port 8080)          — CI/CD server
    ├── Docker Engine                 — builds container images
    └── kubeadm single-node cluster
        ├── namespace: ybrainy        — microservices
        │   ├── course-service    :30082
        │   ├── lesson-service    :30084
        │   ├── quiz-service      :30083
        │   ├── enrollment-service:30085
        │   ├── mysql             (internal)
        │   └── rabbitmq          (internal)
        ├── namespace: default
        │   └── sonarqube         :30900  ← also forwarded to localhost:9000 for Jenkins
        └── namespace: monitoring
            ├── prometheus        :30090
            └── grafana           :30300
```

**Key design decisions:**
- `imagePullPolicy: Never` — images are built locally and imported directly into containerd (`docker save | ctr images import`), no Docker registry needed
- SonarQube is port-forwarded to `localhost:9000` via a systemd service so Jenkins can reach it without going through NodePort
- `EUREKA_CLIENT_ENABLED=false` and no `SPRING_CONFIG_IMPORT` override — services start standalone without needing a Service Discovery server or Config Server

---

## CI/CD Pipeline Structure

### Jenkins (primary — course, lesson, quiz, enrollment)

Each microservice has two separate pipelines following the split CI/CD pattern:

```
Jenkinsfile-CI                         Jenkinsfile-CD
──────────────────                     ──────────────────────────────────
1. Build & Test                        1. Docker Build
   mvn clean verify                       docker build -t ybrainy/<svc>:$BUILD
   JUnit test reports              ──→  2. Import to containerd
2. SonarQube Analysis                     docker save | ctr images import -
   mvn sonar:sonar                    3. Deploy to Kubernetes
   JaCoCo coverage report               kubectl apply -f k8s/
3. Trigger CD  ──────────────────→
   (only runs if CI succeeds)
```

**CD is automatically triggered by CI** — if tests or SonarQube fail, the deploy never runs.

Jenkins jobs (all in Jenkins at http://<WSL2-IP>:8080):

| Job | Type | Last Build |
|-----|------|-----------|
| course-service-CI | Pipeline | #10 SUCCESS |
| course-service-CD | Pipeline | triggered by CI |
| lesson-service-CI | Pipeline | configured |
| lesson-service-CD | Pipeline | configured |
| quiz-service-CI | Pipeline | configured |
| quiz-service-CD | Pipeline | configured |
| enrollment-service-CI | Pipeline | configured |
| enrollment-service-CD | Pipeline | configured |

### GitHub Actions (secondary — quiz, enrollment)

Two additional GitHub Actions workflows exist for quiz-service and enrollment-service:
- `.github/workflows/quiz-service-ci.yml`
- `.github/workflows/enrollment-service-ci-cd.yml`

These trigger on push to `main` or `3la-5atr-houssem`, run `mvn clean verify`, build and push Docker images to Docker Hub. They do **not** include SonarQube (that's handled by Jenkins).

**To show GitHub Actions:** go to the GitHub repo → **Actions** tab → select the workflow.

---

## SonarQube — Code Quality

**URL:** `http://<WSL2-IP>:30900` (login: `admin` / `admin`)

SonarQube is integrated in every Jenkins CI pipeline:
- Maven JaCoCo plugin generates coverage reports (`target/site/jacoco/jacoco.xml`)
- `mvn sonar:sonar` pushes results to SonarQube
- SonarQube shows: bugs, code smells, vulnerabilities, test coverage %, duplications

**Important note:** SonarQube runs in Kubernetes with an H2 embedded database. H2 does not persist data across pod restarts. If the pod restarts, previous analysis results are wiped. After every PC restart, you need to re-run the CI pipelines to repopulate SonarQube.

**For the professor presentation (before/after):**
1. Run ALL four CI pipelines once → this creates the "current state" in SonarQube
2. Take screenshots of each project's dashboard (bugs, smells, coverage)
3. Make a small code improvement in one service (fix a code smell, add a test)
4. Push the change → CI pipeline re-runs automatically → new scan appears
5. Screenshot the improved metrics = "after refactoring" state

---

## Monitoring

| Tool | URL | What it shows |
|------|-----|--------------|
| Prometheus | `http://<WSL2-IP>:30090` | Raw metrics scraping: all 4 services + Jenkins + SonarQube |
| Grafana | `http://<WSL2-IP>:30300` | Dashboards for JVM metrics, HTTP requests, pod health |

Prometheus is configured to scrape `/actuator/prometheus` on all 4 services. The Prometheus config lives in `DEVOPS/monitoring/prometheus.yaml`.

---

## Kubernetes

Deployed with **kubeadm** on WSL2 (single-node, Ubuntu 24.04).

**Known WSL2 issue and permanent fix:**  
WSL2 uses cgroup v2. containerd defaults to the `cgroupfs` cgroup driver but kubelet requires `systemd`. This mismatch causes pause containers (pod sandboxes) to be killed, which triggers a cascade of pod restarts. Fixed permanently by `DEVOPS/fix-containerd-cgroup.sh` which sets `SystemdCgroup = true` in `/etc/containerd/config.toml`.

**After every PC restart:**
```bash
sudo bash /mnt/c/Users/azizs/Downloads/ESPRIT-PI-4SAE4-2025-2026-Ybrainy/DEVOPS/startup.sh
```
This script checks the cgroup fix, waits for K8s, distributes kubeconfig, starts Jenkins and SonarQube port-forward, and prints all URLs.

---

## What the Professor Will Look At

### 1. CI/CD Pipelines
Open Jenkins at `http://<WSL2-IP>:8080`  
Show: course-service-CI → stages (Build & Test → SonarQube Analysis → Trigger CD) → then course-service-CD running automatically  
Key: CD is a **separate job triggered by CI** — this demonstrates the split pipeline pattern

### 2. GitHub Actions (bonus)
Open GitHub repo → Actions tab  
Show: quiz-service-ci.yml and enrollment-service-ci-cd.yml runs

### 3. SonarQube
Open `http://<WSL2-IP>:30900`  
Show: project dashboards with bug count, code smells, test coverage %  
Before/after screenshots required — **run CI pipelines first**

### 4. Kubernetes
Show: `kubectl get pods -n ybrainy` — all 4 services Running  
Show: the K8s YAML manifests in `DEVOPS/<service>/k8s/deployment.yaml`  
Mention: kubeadm setup, not Docker Desktop Kubernetes

### 5. Monitoring
Open Prometheus at `:30090` → Status → Targets → show all services UP  
Open Grafana at `:30300` → show dashboards with live JVM and HTTP metrics

---

## Current Status vs Requirements

| Requirement | Status | Notes |
|-------------|--------|-------|
| CI pipeline per microservice | DONE | Jenkins CI for all 4 |
| CD pipeline per microservice | DONE | Jenkins CD for all 4, triggered by CI |
| CD auto-triggers after CI | DONE | `build job: 'course-service-CD'` in CI post stage |
| Unit tests in CI | DONE | `mvn clean verify` in all CI pipelines |
| SonarQube integration | DONE | `mvn sonar:sonar` + JaCoCo in all CIs |
| Test coverage visible | DONE | JaCoCo XML → SonarQube (repopulate after restart) |
| Before/after screenshots | TODO | Run CI pipelines, screenshot, improve, re-run |
| Kubernetes with kubeadm | DONE | All 6 pods Running |
| Monitoring (DevOps tools) | DONE | Prometheus scrapes Jenkins + SonarQube |
| Monitoring (backend apps) | DONE | Prometheus scrapes all 4 services via actuator |
| Docker (no Docker Desktop) | DONE | Standalone docker-ce on WSL2 Ubuntu |
| Frontend CI/CD | NOT THIS STUDENT | Handled by another team member |

---

## File Structure

```
DEVOPS/
├── README.md                      ← this file
├── startup.sh                     ← run after every PC restart
├── fix-containerd-cgroup.sh       ← permanent K8s cgroup fix
├── fix-control-plane.sh           ← clears stale K8s checkpoints
├── course-service/
│   ├── Jenkinsfile-CI
│   ├── Jenkinsfile-CD
│   └── k8s/deployment.yaml
├── lesson-service/
│   ├── Jenkinsfile-CI
│   ├── Jenkinsfile-CD
│   └── k8s/deployment.yaml
├── quiz-service/
│   ├── Jenkinsfile-CI
│   ├── Jenkinsfile-CD
│   └── k8s/deployment.yaml
├── enrollment-service/
│   ├── Jenkinsfile-CI
│   ├── Jenkinsfile-CD
│   └── k8s/deployment.yaml
├── infrastructure/
│   ├── mysql.yaml
│   ├── rabbitmq.yaml
│   └── sonarqube.yaml
└── monitoring/
    ├── prometheus.yaml
    └── grafana.yaml
```

---

## Quick Commands

```bash
# Check all pods
kubectl get pods -n ybrainy && kubectl get pods -n default && kubectl get pods -n monitoring

# Trigger a pipeline build (replace JOB with job name)
# Do this from Jenkins UI or via Jenkins CLI

# Check course-service logs
kubectl logs -n ybrainy -l app=course-service --tail=50

# Re-deploy a service after building image
kubectl rollout restart deployment/course-service -n ybrainy

# Check SonarQube projects via API
curl -s -u admin:admin http://<WSL2-IP>:30900/api/projects/search | python3 -m json.tool
```
