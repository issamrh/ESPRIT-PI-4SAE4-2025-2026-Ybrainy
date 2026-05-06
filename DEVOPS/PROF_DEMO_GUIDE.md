# YBrainy DevOps — Professor Demo Guide
# "I just opened my PC. Show me the full pipeline."

This guide assumes you have **just turned on your PC and nothing is running yet**.
Follow every step in the exact order written. Do not skip any step.

---

## QUICK REFERENCE (read this first)

| Tool | URL | Login |
|------|-----|-------|
| Jenkins | `http://localhost:8086` | user: `aziz` / pass: `Joncina85738573` |
| SonarQube | `http://localhost:9000` | user: `admin` / token in guide |
| Grafana | `http://localhost:30300` | user: `admin` / pass: `ybrainy2026` |
| Prometheus | `http://localhost:30090` | no login |
| WSL2 Ubuntu terminal | Windows Search → "Ubuntu" | — |

---

## PHASE 1 — START THE ENVIRONMENT (always do this first after PC boot)

### Step 1.1 — Open a WSL2 Ubuntu terminal

1. Press `Windows key`
2. Type: `Ubuntu`
3. Click the **Ubuntu** app (the orange circle icon)
4. A black terminal window opens. You are now in Ubuntu (Linux) inside Windows.
5. You should see a prompt like: `azizs@LAPTOP-XXX:~$`

### Step 1.2 — Run the startup script

In the Ubuntu terminal, type **exactly** this command (one line, then press Enter):

```bash
sudo bash /mnt/c/Users/azizs/Downloads/ESPRIT-PI-4SAE4-2025-2026-Ybrainy/DEVOPS/startup.sh
```

When prompted for a password, type your **Windows/Ubuntu user password** (the sudo password for `azizs`) and press Enter.

**What to expect:** The script takes 2–4 minutes. You will see output like:

```
=============================================
 YBrainy DevOps Startup
=============================================

[1/6] Checking containerd cgroup driver...
  → containerd config OK
[2/6] Starting kubelet...
[3/6] Waiting for Kubernetes API server...
  ...waiting (5/120s)
  ...waiting (10/120s)
  → API server ready after 30s
[4/6] Distributing kubeconfig...
  → kubeconfig copied to azizs and jenkins
  → WSL2 IP updated to 172.22.x.x in kubeconfigs
[5/6] Starting Jenkins and SonarQube port-forward...
  → Jenkins restarted
  → SonarQube port-forward active (localhost:9000)
[6/6] Waiting for pods (30s)...
```

Then it prints a cluster status table and the service URLs.

**If the script prints "ERROR: K8s API server did not become ready"**, run this recovery script instead:

```bash
sudo bash /mnt/c/Users/azizs/Downloads/ESPRIT-PI-4SAE4-2025-2026-Ybrainy/DEVOPS/fix-containerd-cgroup.sh
```

Wait 60 seconds, then re-run the startup.sh command above.

### Step 1.3 — Wait for SonarQube to be fully ready

SonarQube starts slowly inside Kubernetes. After the startup script finishes, wait **1–2 more minutes** before trying to access `http://localhost:9000`.

To check if SonarQube is ready (run this in the same Ubuntu terminal):

```bash
curl -s http://localhost:9000/api/system/status | python3 -m json.tool
```

You should see `"status": "UP"`. If you see `"status": "STARTING"`, wait 30 more seconds and try again.

---

## PHASE 2 — VERIFY EVERYTHING IS UP

### Step 2.1 — Verify Kubernetes pods (Ubuntu terminal)

```bash
kubectl get pods -n ybrainy
kubectl get pods -n default
kubectl get pods -n monitoring
```

Expected output for `-n ybrainy` (after at least one pipeline has run):

```
NAME                                  READY   STATUS    RESTARTS
course-service-xxxx-xxxx              1/1     Running   0
lesson-service-xxxx-xxxx              1/1     Running   0
quiz-service-xxxx-xxxx                1/1     Running   0
enrollment-service-xxxx-xxxx          1/1     Running   0
```

Expected output for `-n default` (SonarQube, MySQL, RabbitMQ):

```
NAME                          READY   STATUS    RESTARTS
sonarqube-xxxx-xxxx           1/1     Running   0
mysql-xxxx-xxxx               1/1     Running   0
rabbitmq-xxxx-xxxx            1/1     Running   0
```

If a pod shows `CrashLoopBackOff` or `Pending`, run `startup.sh` again.

### Step 2.2 — Verify Jenkins is up (Windows browser)

1. Open **Google Chrome** (or Edge)
2. Go to: `http://localhost:8086`
3. You should see the Jenkins login page
4. Log in with: user `aziz`, password `Joncina85738573`
5. You should see the Jenkins dashboard with the pipeline jobs listed

> Note: The startup.sh output message says port 8080 — that is incorrect in the script's output. The actual Jenkins port is **8086**.

### Step 2.3 — Verify SonarQube is up (Windows browser)

1. Open a new browser tab
2. Go to: `http://localhost:9000`
3. You should see the SonarQube login page
4. Log in with user `admin` — if you don't know the password, use the system passcode `ybrainy2026!` which only works at the health endpoint (not web login). Instead, use the API token directly when needed.

> SonarQube token for API calls: `squ_1eaf5b5f45bfd14d1555034739b5755091e7f08a`

---

## PHASE 3 — RUN THE PIPELINES (for the professor)

There are 4 services. Each has a **CI job** (Build + Test + SonarQube) and a **CD job** (Docker Build + Deploy to K8s). CI automatically triggers CD on success.

### Step 3.1 — Trigger the course-service pipeline

In the Jenkins dashboard at `http://localhost:8086`:

1. Click on **course-service-CI** in the list
2. Click **"Build Now"** in the left sidebar
3. You will see a new build appear in the **"Build History"** section on the left (e.g., `#31`)
4. Click on the build number link (e.g., `#31`)
5. Click **"Console Output"** in the left sidebar
6. Watch the logs in real time

**What the professor will see in Console Output:**

```
[Pipeline] stage (Checkout)
Cloning repository https://github.com/...
[Pipeline] stage (Build & Test)
[INFO] Tests run: 14, Failures: 0, Errors: 0
[Pipeline] stage (SonarQube Analysis)
INFO: Analysis report uploaded
[Pipeline] stage (Docker Build)
Successfully built ybrainy/course-service:31
[Pipeline] stage (Deploy to Kubernetes)
deployment.apps/course-service image updated
Waiting for deployment "course-service" rollout to finish...
deployment "course-service" successfully rolled out
Finished: SUCCESS
```

### Step 3.2 — Trigger the other 3 services (repeat for each)

Go back to the Jenkins dashboard and repeat Step 3.1 for:
- **lesson-service-CI**
- **quiz-service-CI**
- **enrollment-service-CI**

You can trigger all 4 at the same time by clicking "Build Now" on each before the first one finishes.

### Step 3.3 — Show the pipeline stages visually

After a build succeeds:
1. Click the build number (e.g., `#31`)
2. The **Blue Ocean** view (if installed) shows colored stage blocks
3. Or use the regular Jenkins view: you will see the stages listed — `Checkout → Build & Test → SonarQube Analysis → Docker Build → Deploy to Kubernetes`

---

## PHASE 4 — SHOW SONARQUBE QUALITY GATE

This is one of the most important things to demonstrate.

### Step 4.1 — View the quality gate result

1. Open browser tab to `http://localhost:9000`
2. Click on **"Projects"** in the top menu
3. Click on **"course-service"** (or whichever service just ran CI)
4. The main project page shows:
   - A **Quality Gate** badge — should say **"Passed"** in green
   - **Coverage** percentage (currently ~82%)
   - **Code Smells**, **Bugs**, **Vulnerabilities** counts

### Step 4.2 — Show the "New Code" view

On the project page, click the **"New Code"** tab. This shows metrics specifically for the code changed in recent commits:
- `new_violations = 0` (no new violations introduced)
- `new_coverage ≥ 50%` (test coverage on new code)
- `new_duplicated_lines_density ≤ 3%`

All three conditions must pass for the quality gate to pass.

### Step 4.3 — Show the quality gate configuration

1. Click **"Quality Gates"** in the top navigation of SonarQube
2. Click on **"YBrainy Gate"**
3. Show the professor the 3 conditions configured:
   - `On New Code: Coverage is greater than 50%`
   - `On New Code: New Violations is 0`
   - `On New Code: Duplicated Lines (%) is less than 3%`

### Step 4.4 — Show SonarQube integration in Jenkins

Go back to Jenkins at `http://localhost:8086`.
1. Open any finished build of **course-service-CI**
2. Click **"Console Output"**
3. Scroll down to find the SonarQube analysis section showing:
   ```
   INFO: ANALYSIS SUCCESSFUL, you can find the results at: http://localhost:9000/dashboard?id=course-service
   INFO: Quality Gate status: PASSED
   ```

---

## PHASE 5 — SHOW KUBERNETES DEPLOYMENT

### Step 5.1 — Show running pods (Ubuntu terminal)

In the Ubuntu terminal:

```bash
kubectl get pods -n ybrainy -o wide
```

This shows all 4 microservices running as pods with their IPs.

### Step 5.2 — Show deployment rollout history

```bash
kubectl rollout history deployment/course-service -n ybrainy
```

This shows that each pipeline run created a new rollout (rolling update).

### Step 5.3 — Show services and NodePorts

```bash
kubectl get services -n ybrainy
```

Shows the NodePort services. Services are accessible from Windows browser at:
- Course service: `http://172.22.108.68:30082/api/courses`
- Lesson service: `http://172.22.108.68:30084/api/lessons`
- Quiz service: `http://172.22.108.68:30083/api/quizzes`
- Enrollment service: `http://172.22.108.68:30085/api/enrollments`

> To get your current WSL2 IP (it may change after restart), run: `hostname -I | awk '{print $1}'`

### Step 5.4 — Test a live API endpoint

Open a new browser tab and go to: `http://172.22.108.68:30082/api/courses`

You should see a JSON response (empty array `[]` or course data). This proves the service is deployed and reachable.

Or use curl in the Ubuntu terminal:

```bash
WSL_IP=$(hostname -I | awk '{print $1}')
curl -s http://$WSL_IP:30082/api/courses | python3 -m json.tool
```

### Step 5.5 — Show K8s deployment YAML (the infrastructure as code)

In the Ubuntu terminal:

```bash
kubectl describe deployment course-service -n ybrainy
```

Or point to the file in VS Code:
**Path:** `DEVOPS/course-service/k8s/deployment.yaml`

---

## PHASE 6 — SHOW GRAFANA MONITORING (if asked)

### Step 6.1 — Open Grafana

1. Open browser tab: `http://localhost:30300`
2. Log in: user `admin`, password `ybrainy2026`
3. If no dashboards are imported yet, click **"+"** → **"Import"**

### Step 6.2 — Import a JVM dashboard

1. Click **"+"** in the left sidebar → **"Import dashboard"**
2. In the "Import via grafana.com" field, type: `4701`
3. Click **"Load"**
4. Select the Prometheus data source
5. Click **"Import"**

This shows JVM heap memory, GC, threads for the Spring Boot services.

### Step 6.3 — Show Prometheus is scraping

Open: `http://localhost:30090`
Click **"Status"** → **"Targets"**
You should see the microservice endpoints listed as "UP".

---

## PHASE 7 — IF SOMETHING GOES WRONG

### Problem: Jenkins page doesn't load at localhost:8086

**Fix:** Run this in Ubuntu terminal:
```bash
sudo systemctl restart jenkins
sleep 10
sudo systemctl status jenkins
```

### Problem: SonarQube page doesn't load at localhost:9000

**Fix:** The port-forward service may have stopped. Run in Ubuntu terminal:
```bash
sudo systemctl restart sonarqube-portforward
sleep 5
sudo systemctl status sonarqube-portforward
```

If the SonarQube pod itself crashed:
```bash
kubectl get pods -n default
kubectl delete pod -n default -l app=sonarqube
# Wait 60 seconds for it to restart, then restart the port-forward service again
```

### Problem: kubectl command not found

Run this in Ubuntu terminal:
```bash
export KUBECONFIG=/etc/kubernetes/admin.conf
kubectl get nodes
```

### Problem: A pipeline build fails

1. Click on the failed build number in Jenkins
2. Click "Console Output"
3. Scroll to the bottom — the error message is there
4. Common fix: run `sudo bash .../startup.sh` again to refresh kubeconfig

### Problem: Docker build fails with "permission denied"

Run in Ubuntu terminal:
```bash
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins
```

---

## WHAT THE PROFESSOR WILL SEE — SUMMARY TABLE

| Demo Item | Where to show | Expected result |
|-----------|--------------|-----------------|
| CI pipeline triggered | Jenkins → course-service-CI → Build Now | Green "SUCCESS" |
| All 4 services have CI | Jenkins dashboard | 4 CI jobs + 4 CD jobs |
| Tests pass | Console Output or Jenkins test results | 14 tests, 0 failures |
| SonarQube quality gate | http://localhost:9000 → Projects | "Passed" green badge |
| Coverage ≥ 50% (new code) | SonarQube → course-service → New Code | ~82% coverage |
| Docker images built | Console Output shows "Successfully built" | Image ybrainy/course-service:N |
| K8s pods running | `kubectl get pods -n ybrainy` | 4 pods STATUS=Running |
| Live API endpoint | Browser http://WSL_IP:30082/api/courses | JSON response |
| Grafana monitoring | http://localhost:30300 | Dashboard with metrics |
| Infrastructure as code | Show DEVOPS/ folder structure | YAML files for each service |

---

## WHAT STILL NEEDS TO BE DONE (priority order)

### HIGH PRIORITY — Before the demo

**1. GitHub Webhook (auto-trigger on git push) — 3 pts**

This is worth the most points and is NOT done yet. Currently pipelines must be triggered manually.

To add it:
1. Go to GitHub repo → Settings → Webhooks → Add webhook
   - Payload URL: `http://<your-ngrok-or-public-IP>:8086/github-webhook/`
   - Content type: `application/json`
   - Events: Just the push event
2. In Jenkins → course-service-CI → Configure → Build Triggers → check "GitHub hook trigger for GITScm polling"
3. Repeat for all 4 CI jobs

> Problem: Jenkins is on WSL2 behind NAT. GitHub cannot reach `localhost:8086`. You need either:
> - `ngrok http 8086` to expose it temporarily (install ngrok on Windows, then run from PowerShell)
> - Or demonstrate it with a local git push while the professor watches

**Fastest approach for the demo:**
```bash
# In Windows PowerShell, install ngrok then run:
ngrok http 8086
```
Copy the `https://xxxx.ngrok.io` URL → paste it in GitHub webhook as `https://xxxx.ngrok.io/github-webhook/`
Then do a `git push origin 3la-5atr-houssem` while the professor watches — Jenkins will auto-trigger.

**2. Docker Registry Push — 1.5 pts**

Currently Docker images are built but NOT pushed to a registry. You need to push to Docker Hub or a local registry.

Quick fix — add to Jenkinsfile after the Docker Build stage:
```groovy
stage('Push to Registry') {
    steps {
        withCredentials([usernamePassword(credentialsId: 'dockerhub-creds',
                         usernameVariable: 'DOCKER_USER',
                         passwordVariable: 'DOCKER_PASS')]) {
            sh """
                echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin
                docker push ${IMAGE_NAME}:${IMAGE_TAG}
                docker push ${IMAGE_NAME}:latest
            """
        }
    }
}
```

You need to:
1. Create a Docker Hub account at hub.docker.com (free)
2. In Jenkins → Credentials → Add a "Username with password" credential with ID `dockerhub-creds`
3. Add the stage above to all 4 Jenkinsfiles

**3. SonarQube Before/After Screenshots — Required for presentation**

Take screenshots now (before any more changes) to show the "before" state.
The "after" state shows violations = 0 and coverage passing.
Save both in `DEVOPS/sonarqube-before-pictures/` (that folder already exists in your repo).

### MEDIUM PRIORITY — For completeness

**4. Verify all 4 service pods are Running in K8s**

Run `kubectl get pods -n ybrainy` and make sure all 4 are Running. If any are in `ImagePullBackOff` or `Pending`, trigger their CI pipeline first so the Docker image gets built and deployed.

**5. Grafana dashboard showing live data**

Import dashboard ID `4701` (JVM metrics) from Grafana. Make sure Prometheus is scraping the `/actuator/prometheus` endpoint of each service.

Check if Spring Boot actuator Prometheus endpoint is enabled in each service:
In `src/main/resources/application.properties` or `application.yml`:
```properties
management.endpoints.web.exposure.include=health,info,prometheus
management.endpoint.prometheus.enabled=true
```

**6. Jenkins + SonarQube integration screenshot**

Make sure you have at least one screenshot showing:
- Jenkins build marked SUCCESS
- SonarQube quality gate showing PASSED (same build)

This is the key deliverable the professor looks for.

### LOW PRIORITY — Nice to have

**7. Kubernetes resource limits**

The YAML files should have `resources.requests` and `resources.limits` set. Check `DEVOPS/course-service/k8s/deployment.yaml`.

**8. Liveness/readiness probes in K8s**

Each K8s deployment should have a `livenessProbe` and `readinessProbe` pointing to `/actuator/health`.

**9. Jenkins shared pipeline library**

If 4 Jenkinsfiles are nearly identical, refactor into a shared library in Jenkins → Manage Jenkins → Configure System → Global Pipeline Libraries.

---

## FILE STRUCTURE — Show this to the professor

```
DEVOPS/
├── startup.sh                      ← Run after every PC restart
├── PROF_DEMO_GUIDE.md              ← This file
├── README.md                       ← Architecture overview
├── infrastructure/
│   ├── mysql.yaml                  ← MySQL K8s deployment
│   ├── rabbitmq.yaml               ← RabbitMQ K8s deployment
│   └── sonarqube.yaml              ← SonarQube K8s deployment
├── course-service/k8s/
│   └── deployment.yaml             ← K8s deployment for course-service
├── lesson-service/k8s/
│   └── deployment.yaml
├── quiz-service/k8s/
│   └── deployment.yaml
├── enrollment-service/k8s/
│   └── deployment.yaml
└── monitoring/
    ├── prometheus.yaml             ← Prometheus K8s deployment
    └── grafana.yaml                ← Grafana K8s deployment

PIDEV-.../Course/tp-foyer/
└── Jenkinsfile                     ← CI/CD pipeline definition (as code)
```

---

## EXACT SEQUENCE FOR THE DAY OF THE DEMO

```
T-0   PC boots
T+1   Open Ubuntu terminal → run startup.sh (sudo bash .../startup.sh)
T+4   Script finishes → K8s cluster is up
T+5   Open Chrome → go to localhost:8086 (Jenkins) → verify jobs are listed
T+6   Open new tab → go to localhost:9000 (SonarQube) → verify projects are listed
T+8   In Jenkins, click course-service-CI → Build Now
T+9   Show Console Output live to professor
T+13  Build succeeds → show blue "SUCCESS" → switch to SonarQube tab
T+14  Show Quality Gate PASSED, Coverage ~82%, New Violations = 0
T+15  Run kubectl get pods -n ybrainy → show all 4 Running
T+16  Open browser tab → http://$(WSL_IP):30082/api/courses → show live JSON
T+17  Show DEVOPS/ folder in VS Code to demonstrate infrastructure-as-code
T+20  Done. Answer questions.
```

> Tip: Have all 5 browser tabs pre-opened before the professor arrives:
> 1. `http://localhost:8086` — Jenkins
> 2. `http://localhost:9000` — SonarQube
> 3. `http://localhost:30300` — Grafana
> 4. `http://localhost:30090` — Prometheus
> 5. API endpoint tab (update IP with `hostname -I | awk '{print $1}'`)
