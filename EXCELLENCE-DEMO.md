# YBrainy — Excellence Items Demo Guide
> Branch: `3la-5atr-houssem` | Presenter: Mohamed Aziz Selmi

---

## Quick-Access URLs

| Tool | URL | Credentials |
|------|-----|-------------|
| Jenkins | http://localhost:8086 | admin / admin |
| SonarQube | http://172.22.108.68:30900 | admin / admin |
| Prometheus | http://172.22.108.68:30090 | — |
| Grafana | http://172.22.108.68:30300 | admin / ybrainy2026 |
| ML Service | http://172.22.108.68:30086/health | — |
| MLflow | http://172.22.108.68:30500 | — |
| ArgoCD | https://172.22.108.68:30808 | admin / QEU3s65pn4CTqTm6 |

---

## 1. ML Service Full DevOps Pipeline

### What it is
A complete CI/CD pipeline for a Python Flask service hosting 4 ML models (DSO1–DSO4). Jenkins builds, tests, scans, containerises, and deploys to Kubernetes automatically on every code push.

### Why it's different
Teammates handle Java microservices. This is the only ML/Python service in the platform — it runs real scikit-learn models (KNN, Random Forest, ARIMA/SARIMA) baked into the Docker image and served via REST.

### Live Demo Steps

1. **Show the pipeline in Jenkins:**
   - Open http://localhost:8086 → `ml-service-CI` → click latest build
   - Walk through stages: Python Tests → SonarQube Analysis → Register Models in MLflow → Docker Build → Import to Containerd → Trigger CD

2. **Show the deployed service is live:**
   ```
   curl http://172.22.108.68:30086/health
   ```
   Expected output:
   ```json
   {"models":{"dso1_conversion":true,"dso2_recommendations":true,"dso3_quality":true,"dso4_forecast":true},"status":"ok"}
   ```

3. **Make a live DSO1 prediction (student conversion):**
   ```
   curl -s -X POST http://172.22.108.68:30086/predict/conversion \
     -H "Content-Type: application/json" \
     -d '{"timeSpentOnCourse":120,"completionRate":0.85,"quizScores":78,"numberOfVideosWatched":15,"numLogins":12,"forumReads":5}'
   ```
   Expected:
   ```json
   {"conversionLabel":"HIGH","conversionProbability":0.89,"percentage":89.0}
   ```

4. **Make a live DSO3 quality prediction:**
   ```
   curl -s -X POST http://172.22.108.68:30086/predict/quality \
     -H "Content-Type: application/json" \
     -d '{"numLectures":25,"contentDuration":8.5,"lessonTypeVariety":3,"pctVideoLessons":0.7,"numLessons":25,"certEncoded":1,"levelEncoded":1,"rating":4.2,"ratingCount":45}'
   ```

5. **Show Prometheus metrics from ML service:**
   Open: http://172.22.108.68:30090/graph?g0.expr=flask_http_request_total{job="ml-service"}

### What the professor sees
- Jenkins stages turning green in real-time
- Live HTTP 200 from the ML service with meaningful ML output
- Prometheus scraping the Flask `/metrics` endpoint

### One-liner
"This is a real MLOps pipeline: code push → automated tests → quality gate → Docker build → Kubernetes deployment — entirely for a Python machine learning service with 4 separate prediction models."

### Likely Questions & Answers

**Q: Why Flask and not a Java service?**
A: The ML models are trained with scikit-learn (Python). Flask is the natural choice for serving Python ML models. A Java service would require a bridge or JNI, which adds unnecessary complexity.

**Q: How are models loaded?**
A: Models are serialised with `joblib` (joblib.dump) at training time and loaded at service startup. All 4 models are loaded once and cached in memory for fast prediction.

**Q: How does Prometheus scrape a Python service?**
A: `prometheus-flask-exporter` wraps Flask's request lifecycle and exposes `/metrics` automatically with per-route latency histograms and request counters.

---

## 2. MLflow Model Registry

### What it is
MLflow is an open-source MLOps platform. The Model Registry tracks every version of every ML model — who trained it, with what code, what its performance was, and which version is currently in production.

### Why it's different
Trivy scans Docker images. Terraform provisions infrastructure. Neither tracks what's running INSIDE the container. MLflow tracks the ML models specifically — it's the only MLOps tool in this project.

### Live Demo Steps

1. **Open MLflow UI:**
   Navigate to: http://172.22.108.68:30500

2. **Show the Model Registry:**
   - Click "Models" tab in the top navigation
   - You'll see 4 registered models:
     - `YBrainy-DSO1-ConversionPredictor`
     - `YBrainy-DSO2-CourseRecommender`
     - `YBrainy-DSO3-QualityPredictor`
     - `YBrainy-DSO4-DemandForecaster`
   - Each has a version number corresponding to the Jenkins build that registered it

3. **Show an experiment run:**
   - Click "Experiments" → select `dso1-model-registry`
   - You'll see a run with parameters: `build_number`, `model_file`, `description`
   - Click the run → see the logged model artifact

4. **Trigger a live prediction and show it in Experiments:**
   ```
   curl -s -X POST http://172.22.108.68:30086/predict/conversion \
     -H "Content-Type: application/json" \
     -d '{"timeSpentOnCourse":60,"completionRate":0.3,"quizScores":40,"numberOfVideosWatched":4,"numLogins":2,"forumReads":0}'
   ```
   Then refresh http://172.22.108.68:30500 → Experiments → `dso1-conversion-predictions`
   You'll see a new run with the input parameters and conversion probability logged.

5. **Show the K8s deployment:**
   ```
   wsl -u root -e bash -c "KUBECONFIG=/etc/kubernetes/admin.conf kubectl get pods -n ybrainy -l app=mlflow-tracking"
   ```

### What the professor sees
- MLflow UI at port 30500 with registered models named after YBrainy components
- Version history: each Jenkins CI build creates a new model version
- Live prediction → appears as a logged run seconds later
- Both Jenkins (model registration) and Flask service (prediction tracking) write to the same MLflow backend

### One-liner
"MLflow means we know exactly what model version is running in production, what parameters it was trained with, and how it's performing — instead of blindly deploying a pickle file with no version tracking."

### Likely Questions & Answers

**Q: What's the difference between an experiment and a registered model?**
A: An experiment is a collection of runs (each run = one execution: training, registration, or prediction). A registered model is a formal version-controlled artifact that can be staged for Production.

**Q: Could you do a blue/green deployment between model versions?**
A: Yes. MLflow has Production/Staging/Archived stages. You could register a new model version to Staging, validate it, then transition to Production. The Flask service would load by stage name, not version number.

**Q: Where is the MLflow data stored?**
A: SQLite database on a Kubernetes PersistentVolume at `/data/mlflow.db` inside the pod, backed by a HostPath volume at `/data/mlflow` on the node. Persists across pod restarts.

---

## 3. Argo CD — GitOps Continuous Deployment

### What it is
Argo CD is a GitOps operator for Kubernetes. It watches a Git repository and automatically applies any changes to Kubernetes. Git is the single source of truth for what runs in production.

### Why it's different
Jenkins CD does `kubectl apply` imperatively — it's a one-shot action. Argo CD does declarative reconciliation — it continuously compares desired state (Git) with actual state (K8s) and self-heals any drift. This is the industry standard for production Kubernetes.

### Live Demo Steps

1. **Open Argo CD UI:**
   Navigate to: https://172.22.108.68:30808
   - Accept the self-signed certificate warning
   - Login: `admin` / `QEU3s65pn4CTqTm6`

2. **Show the ml-service Application:**
   - You'll see the `ml-service` Application card
   - Status should be `Synced` and `Healthy`
   - Click on it → see the Deployment, Service, and ReplicaSet represented as a dependency graph

3. **Show what ArgoCD is watching:**
   "This Application watches a lightweight local git repo (best practice: separate manifests repo from application code). Whenever the k8s deployment YAML changes — image tag updated by Jenkins — ArgoCD detects it and applies it to the cluster automatically."

4. **Trigger a manual sync to show the reconciliation:**
   Click "Sync" → "Synchronize" in the UI.
   ArgoCD compares Git state with cluster state and confirms they match.

5. **Show self-healing (demonstrate drift detection):**
   ```
   wsl kubectl scale deployment ml-service -n ybrainy --replicas=0
   ```
   Wait 15-20 seconds. ArgoCD detects the drift and restores replicas to 1 automatically (selfHeal: true). Refresh the ArgoCD UI to see it self-correct.

6. **Show the GitOps flow:**
   "Git is the single source of truth. If someone manually changes anything in Kubernetes, ArgoCD reverts it to match Git within minutes. Every deployment is traceable to a git commit — who changed what, when, and why."

### What the professor sees
- Professional ArgoCD UI with application health tree
- Synced green status showing K8s matches Git
- Self-healing restoring killed pods automatically

### One-liner
"Argo CD means Git is the single source of truth — if someone manually changes something in Kubernetes, Argo CD reverts it to match what's in Git. This prevents configuration drift and gives complete auditability."

### Likely Questions & Answers

**Q: How is ArgoCD different from Jenkins CD?**
A: Jenkins CD is push-based and imperative — it runs `kubectl apply` once per pipeline. ArgoCD is pull-based and declarative — it continuously watches Git and reconciles state. Jenkins can't detect if someone manually modifies the cluster after deployment.

**Q: What is self-healing?**
A: With `selfHeal: true`, if the actual Kubernetes state drifts from Git (someone scales down, edits a config, deletes a pod), ArgoCD automatically restores it to match Git — without any human intervention.

**Q: Isn't ArgoCD redundant if we have Jenkins CD?**
A: They're complementary. Jenkins handles the build → test → push pipeline. ArgoCD handles the Git → K8s sync. In practice: Jenkins updates the image tag in Git, ArgoCD deploys it to K8s.

---

## 4. Prometheus Alerting Rules — Live Alert Firing

### What it is
Prometheus AlertManager fires alerts when service metrics cross defined thresholds. This is the difference between reactive (something broke, now I check) and proactive (Prometheus tells you before users notice).

### Live Demo Steps

1. **Show the 4 alert rules in Prometheus:**
   Navigate to: http://172.22.108.68:30090/alerts
   You'll see 4 rules:
   - `ServiceDown` — fires if any service pod disappears
   - `HighMemoryUsage` — fires if memory > 80% of limit
   - `HighErrorRate` — fires if HTTP 5xx rate > 10%
   - `SlowResponseTime` — fires if p99 response time > 2s

2. **Show rules loaded:**
   http://172.22.108.68:30090/api/v1/rules → count of rule groups should be 1 with 4 rules.

3. **Trigger a LIVE ServiceDown alert:**
   ```
   wsl -u root -e bash -c "KUBECONFIG=/etc/kubernetes/admin.conf kubectl scale deployment quiz-service -n ybrainy --replicas=0"
   ```
   Wait 1-2 minutes. Then go to: http://172.22.108.68:30090/alerts
   The `ServiceDown` alert will turn red (FIRING state).

4. **Show Alertmanager received it:**
   Navigate to: http://172.22.108.68 (Alertmanager port)
   ```
   wsl -u root -e bash -c "KUBECONFIG=/etc/kubernetes/admin.conf kubectl get svc -n monitoring --no-headers"
   ```
   Find the alertmanager NodePort and open it.

5. **Restore the pod:**
   ```
   wsl -u root -e bash -c "KUBECONFIG=/etc/kubernetes/admin.conf kubectl scale deployment quiz-service -n ybrainy --replicas=1"
   ```
   Alert clears after ~2 minutes.

6. **Show the HighMemoryUsage alert (currently PENDING):**
   http://172.22.108.68:30090/alerts — `HighMemoryUsage` will be in PENDING state because the ML service is using memory near the threshold.

### What the professor sees
- Alert rules visible in Prometheus UI
- Real-time PENDING → FIRING transition
- Alert clearing when service recovers

### One-liner
"Prometheus doesn't just collect metrics — it evaluates them against alert rules every 15 seconds and fires alerts through Alertmanager when thresholds are crossed. This is production-grade proactive monitoring."

### Likely Questions & Answers

**Q: Where are the alert rules defined?**
A: In a Kubernetes ConfigMap `prometheus-rules` in the `monitoring` namespace, mounted into the Prometheus pod at `/etc/prometheus/rules.yml`. They're version-controlled in `DEVOPS/monitoring/`.

**Q: What happens when an alert fires?**
A: Prometheus evaluates the rule expression. When it's true for `for: 1m` (1 minute), the alert transitions from PENDING to FIRING and Alertmanager sends a notification (email/Slack/webhook, configured in alertmanager.yaml).

**Q: What's the difference between PENDING and FIRING?**
A: PENDING means the condition is currently true but hasn't been true long enough (the `for:` duration). FIRING means it's been true long enough to be considered a real problem, not a transient spike.

---

## 5. Grafana Dashboards — Live Data

### What it is
Grafana visualises the Prometheus metrics as dashboards with graphs, gauges, and panels. It provides the "single pane of glass" view of the entire platform.

### Live Demo Steps

1. **Open Grafana:**
   Navigate to: http://172.22.108.68:30300
   Login: `admin` / `ybrainy2026`

2. **Show the YBrainy dashboard:**
   Click "Dashboards" → find the YBrainy dashboard
   Key panels to highlight:
   - HTTP request rate per service
   - Response time (p50, p95, p99)
   - Memory usage by pod
   - ML service prediction request rate

3. **Show live data updating:**
   Generate traffic:
   ```
   for i in $(seq 1 10); do curl -s http://172.22.108.68:30086/health > /dev/null; done
   ```
   Within 15 seconds, the ML service request rate panel will tick up.

4. **Show ML service predictions panel (if exists):**
   The ML service exposes `flask_http_request_total{endpoint="predict_conversion"}` — show this metric in Prometheus first:
   http://172.22.108.68:30090/graph?g0.expr=flask_http_request_total

5. **Show the Explore feature:**
   Click "Explore" → Prometheus datasource → type `up{namespace="ybrainy"}` → you'll see 1/0 for each service.

### What the professor sees
- Grafana dashboard with multiple panels showing live metrics
- Real-time graph updates as traffic is generated
- All 5 service pods + MLflow visible in metrics

### One-liner
"Grafana transforms raw Prometheus metrics into visual dashboards. This is what the on-call engineer looks at during an incident — they see which service is slow, which is consuming too much memory, and whether the issue is spreading."

### Likely Questions & Answers

**Q: How does Grafana know about Prometheus?**
A: The Prometheus datasource is configured in Grafana via provisioning (a ConfigMap in the monitoring namespace). Grafana connects to `http://prometheus:9090` inside the cluster using the ClusterIP service.

**Q: Can you add alerts in Grafana too?**
A: Yes, Grafana has its own alerting. In this setup, Prometheus owns the alerting rules because it's closer to the data source. Grafana alerts are better for complex multi-datasource conditions.

---

## 6. ML Service in SonarQube (Python Code Analysis)

### What it is
SonarQube analyses the ML service Python code for bugs, security vulnerabilities, code smells, and test coverage. The quality gate ensures code quality is maintained before deployment.

### Live Demo Steps

1. **Open SonarQube:**
   Navigate to: http://172.22.108.68:30900
   Login: `admin` / `admin`

2. **Open the ml-service project:**
   Click "Projects" → `YBrainy ML Service`
   Show:
   - Coverage % from pytest (14 tests passing)
   - 0 Bugs, 0 Vulnerabilities
   - Code smells count
   - Quality Gate status: PASSED

3. **Show the test coverage detail:**
   Click "Coverage" → see which lines of `app.py` are covered by the 14 tests

4. **Show this was run by Jenkins:**
   Go to http://localhost:8086 → `ml-service-CI` → latest build → SonarQube Analysis stage
   Show: "ANALYSIS SUCCESSFUL" in the Jenkins log

5. **Show the Python analysis is different from Java:**
   "The other 4 services use Maven's sonar plugin. For Python, we use the standalone sonar-scanner CLI, which I installed at `/opt/sonar-scanner/bin/`. Coverage is measured with Python's `coverage.py` and the XML report is fed to sonar-scanner."

### What the professor sees
- SonarQube project for the ML service with Python-specific metrics
- 0 bugs, 0 vulnerabilities from the analysis
- Coverage percentage from the 14 pytest cases
- The project linked to Jenkins pipeline

### One-liner
"SonarQube doesn't just work for Java — with sonar-scanner and coverage.py, we get the same quality metrics for our Python ML service: bug detection, vulnerability scanning, and line-by-line coverage tracking."

### Likely Questions & Answers

**Q: Why is coverage lower for the ML service than for Java services?**
A: The ML models themselves (DSO1–DSO4 prediction logic) are harder to unit-test because they depend on trained model files loaded from disk. The tests mock the model loading to cover the Flask routing and error handling logic.

**Q: What is sonar-scanner vs Maven sonar plugin?**
A: The Maven sonar plugin is built into the Java build lifecycle — it hooks into `mvn verify` and reports coverage from JaCoCo. For Python, there's no equivalent build tool, so we use the standalone `sonar-scanner` CLI with `coverage.py` for coverage reports.

---

## Pre-Demo Checklist (30 seconds before professor arrives)

```powershell
# From PowerShell as Administrator
.\START-EVERYTHING.ps1
```

Then start the git daemon for ArgoCD (if not already running):
```bash
# In WSL terminal
wsl bash -c "ss -tlnp | grep 9418 || (touch /tmp/ml-k8s.git/git-daemon-export-ok && git daemon --base-path=/tmp --export-all --reuseaddr --port=9418 &>/tmp/git-daemon.log &)"
```

Then verify these URLs load in browser tabs:
- [ ] http://localhost:8086 → Jenkins dashboard
- [ ] http://172.22.108.68:30900 → SonarQube projects list
- [ ] http://172.22.108.68:30090/alerts → Prometheus alerts (4 rules visible)
- [ ] http://172.22.108.68:30300 → Grafana dashboard
- [ ] http://172.22.108.68:30500 → MLflow UI (experiments + model registry)
- [ ] https://172.22.108.68:30808 → ArgoCD (ml-service synced)
- [ ] http://172.22.108.68:30086/health → ML service (all 4 models true)

---

## Self-Assessment Score

| Excellence Item | Implementation | Demo URL | Wow Factor |
|---|---|---|---|
| ML Service DevOps Pipeline | Full CI/CD: Jenkins × 2 pipelines, K8s pod Running, Prometheus scraping | http://localhost:8086 | 4/5 |
| MLflow Model Registry | 4 models registered, prediction tracking, K8s deployment | http://172.22.108.68:30500 | 5/5 |
| Argo CD GitOps | All pods Running, ml-service App Synced, self-heal enabled | https://172.22.108.68:30808 | 5/5 |
| Prometheus Alerts (live) | 4 rules loaded, ServiceDown can be triggered live in ~90s | http://172.22.108.68:30090/alerts | 4/5 |
| Grafana Dashboards | Live metrics from all 5 services + ML service | http://172.22.108.68:30300 | 3/5 |
| ML Service in SonarQube | Analysis successful, coverage measured with coverage.py | http://172.22.108.68:30900 | 4/5 |
