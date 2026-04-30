# YBrainy DevOps — Personal Working Guide
> Written for Mohamed Aziz Selmi | Courses Microservice | Branch: `3la-5atr-houssem`

---

## Table of Contents
1. [What We Built & Where Things Are](#1-what-we-built--where-things-are)
2. [How to Start Everything from a Cold Desktop](#2-how-to-start-everything-from-a-cold-desktop)
3. [How to Trigger and Read the Pipeline](#3-how-to-trigger-and-read-the-pipeline)
4. [Progress Assessment vs. Professor Requirements](#4-progress-assessment-vs-professor-requirements)
5. [What Still Needs to Be Done](#5-what-still-needs-to-be-done)

---

## 1. What We Built & Where Things Are

### Your Jenkinsfile — the brain of the pipeline
```
DEVOPS/Jenkinsfile
```
This is the file Jenkins reads every time you run a build. It is the only Jenkinsfile
that your Jenkins job actually uses. There is a second copy at:
```
PIDEV-YBrainy-E-leraning-certifications-Platform/YBRAINY/Course/tp-foyer/Jenkinsfile
```
but that one is **not wired to any Jenkins job** — it was an earlier attempt and is
now just a backup. Ignore it.

### What the pipeline does (stage by stage)

```
Build & Test  →  SonarQube Analysis  →  Docker Build  →  Deploy to Kubernetes
     CI                  CI                   CD                   CD
```

| Stage | What it actually does | Why it matters |
|---|---|---|
| **Build & Test** | Runs `mvn clean verify` — compiles the code and runs all 21 unit tests | Proves the code works before anything else runs |
| **SonarQube Analysis** | Sends code + test coverage (JaCoCo XML) to SonarQube | Measures code quality, bugs, vulnerabilities, and test coverage % |
| **Docker Build** | Builds a Docker image tagged `ybrainy/course-service:<buildNumber>` | Packages the app into a container ready for deployment |
| **Deploy to Kubernetes** | Runs `kubectl set image` to update the running deployment | Delivers the new version to the cluster (currently set as UNSTABLE-safe so it won't fail the whole build if K8s isn't ready) |

### Key files and what they do

| File | Purpose |
|---|---|
| `DEVOPS/Jenkinsfile` | The active pipeline definition Jenkins reads |
| `PIDEV-.../Course/tp-foyer/pom.xml` | Maven build config — contains JaCoCo, Surefire, H2, build.dir trick |
| `PIDEV-.../Course/tp-foyer/Dockerfile` | Multi-stage Docker build (Maven build inside container → JRE runtime image) |
| `PIDEV-.../Course/tp-foyer/src/test/resources/application.properties` | Test-only config: H2 in-memory DB, dummy API keys, Eureka/Config Server disabled |
| `PIDEV-.../Course/tp-foyer/src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` | Forces Mockito to use subclass mock maker (required on Java 21 / WSL2) |
| `PIDEV-.../Course/tp-foyer/k8s/` | Kubernetes manifests (Deployment, Service YAML files) |

---

## 2. How to Start Everything from a Cold Desktop

You are sitting at your Windows desktop. Nothing is open. Here is the exact order
of operations to get the pipeline ready to run.

---

### Step 1 — Open WSL
Open **Windows Terminal** (or CMD/PowerShell) and type:
```
wsl
```
Everything DevOps-related (Jenkins, SonarQube, Maven, Docker CLI) runs inside WSL
(Ubuntu). Your project files live on the Windows filesystem and are accessible from
WSL at `/mnt/c/Users/azizs/Downloads/ESPRIT-PI-4SAE4-2025-2026-Ybrainy/`.

---

### Step 2 — Start Docker Desktop
Open **Docker Desktop** from the Windows taskbar or Start menu and wait until it
says "Docker Desktop is running" (the whale icon in the system tray turns solid).

**Why:** The Docker Build stage of the pipeline runs `docker build`. If Docker is
not running, that stage will fail with "Cannot connect to Docker daemon".

Docker Desktop on Windows uses the WSL2 backend, which means the `docker` command
inside your WSL terminal connects to the same Docker engine.

---

### Step 3 — Start Jenkins (inside WSL)
In your WSL terminal:
```bash
sudo systemctl start jenkins
```
Or if Jenkins is not managed by systemd:
```bash
sudo service jenkins start
```
Then open your browser and go to:
```
http://localhost:8080
```
Log in with your Jenkins credentials.

**Why:** Jenkins is the automation server that reads your Jenkinsfile and runs the
pipeline. It does NOT start automatically on boot unless you configured it that way.

**How to check it's running:**
```bash
sudo systemctl status jenkins
```
You should see `Active: active (running)`.

---

### Step 4 — Start SonarQube (inside WSL)
SonarQube is a separate server. Start it with:
```bash
cd /opt/sonarqube/bin/linux-x86-64   # adjust path if different on your machine
./sonar.sh start
```
Then open:
```
http://localhost:9000
```
Log in (default: admin / admin, or whatever you set).

**Why:** The SonarQube Analysis stage of the pipeline sends code analysis to this
server. If SonarQube is down, that stage fails and the Docker Build/Deploy stages
are skipped.

**How to check SonarQube is ready:** The web UI at `localhost:9000` takes about
30–60 seconds to fully start. Wait until you see the dashboard before triggering
a build.

**Note:** SonarQube requires Elasticsearch internally and needs a fair amount of
RAM (~1.5 GB). If your machine is low on memory, it may take longer.

---

### Step 5 — Make sure your credentials exist in Jenkins
Your pipeline uses two stored secrets in Jenkins:

| Credential ID | Type | What it is |
|---|---|---|
| `sonar-token2` | Secret text | Your SonarQube user token (generated from SonarQube → My Account → Security) |
| `kubeconfig` | Secret file | Your `~/.kube/config` file for Kubernetes cluster access |

To verify they exist:
1. Go to `http://localhost:8080`
2. Click **Manage Jenkins** → **Credentials** → **System** → **Global credentials**
3. Confirm both `sonar-token2` and `kubeconfig` are listed

If `sonar-token2` is missing or expired:
1. Go to SonarQube (`localhost:9000`) → top-right user icon → **My Account** → **Security**
2. Generate a new token
3. In Jenkins, add a new "Secret text" credential with ID `sonar-token2` and paste the token

---

### Step 6 — Push your latest commits (if you haven't)
Jenkins reads the Jenkinsfile from the local git repository on your machine
(`/mnt/c/Users/azizs/Downloads/ESPRIT-PI-4SAE4-2025-2026-Ybrainy`).
It does NOT pull from GitHub for this — it reads the file directly from your disk.

So you do NOT need to push to GitHub before running a build. But if you made
changes to the Jenkinsfile or source code, save them. Jenkins will pick them up
on the next build.

---

## 3. How to Trigger and Read the Pipeline

### Triggering a build
1. Go to `http://localhost:8080`
2. Find your pipeline job (probably named `ybrainy-course-service`)
3. Click **Build Now**
4. Watch the **Stage View** — each box represents one stage

### Reading the console output
Click on the build number (e.g. `#19`) → **Console Output**.

Here is what a healthy run looks like:

```
[Build & Test]       → INFO BUILD SUCCESS + Tests run: 21, Failures: 0
[SonarQube Analysis] → INFO ANALYSIS SUCCESSFUL
[Docker Build]       → Successfully built <image-id> / Successfully tagged ybrainy/course-service:19
[Deploy to K8s]      → deployment.apps/course-service image updated  (or UNSTABLE if no cluster)
[Post actions]       → rm -rf /tmp/ybrainy-builds/19
Finished: SUCCESS
```

### What the colors mean in Stage View
| Color | Meaning |
|---|---|
| Green | Stage passed |
| Red | Stage failed — everything after it is skipped |
| Yellow/Orange | Stage is UNSTABLE (passed with warnings, e.g. K8s not configured) |
| Grey | Skipped because a previous stage failed |

### Reading the test results
After a build, click **Test Result** in the left sidebar to see which of the
21 tests passed/failed, with full stack traces for any failures.

### Reading SonarQube results
After the SonarQube stage passes, go to `http://localhost:9000` →
Projects → `course-service`. You will see:
- **Bugs / Vulnerabilities / Code Smells** — quality issues found
- **Coverage %** — what percentage of your code is covered by the 21 tests
- **Duplications** — copy-pasted code blocks

---

## 4. Progress Assessment vs. Professor Requirements

### What the professor asked for (summarized)

| Requirement | Status |
|---|---|
| CI pipeline per backend microservice (build + test + sonar) | Partially done |
| CD pipeline per backend microservice (docker + k8s), auto-triggered after CI | Partially done |
| Common frontend CI + CD pipelines | Not started |
| Unit tests per module (each student writes their own) | Done for courses |
| SonarQube with before/after screenshots | SonarQube wired up, screenshots needed |
| Kubernetes with kubeadm (shared VM environment) | K8s manifests written, kubeadm cluster not set up |
| Monitoring (DevOps tools + backend + frontend) | Not started |
| Bonus: extra tools / complexity | Not started |

---

### What we actually built for the courses microservice

**One combined pipeline** (`DEVOPS/Jenkinsfile`) that covers both CI and CD in a
single Jenkins job with 4 stages:

```
CI part:  Build & Test  →  SonarQube Analysis
CD part:  Docker Build  →  Deploy to Kubernetes
```

**Technically**, the professor asked for two SEPARATE pipelines (a CI job and a CD
job), where the CD job is triggered automatically when the CI job succeeds. What we
have achieves the same end result in one job. This is a common real-world approach,
but your professor may deduct marks if they expect two separate Jenkins jobs.

If you want to split them into two jobs later, the approach would be:
- Job 1 (`course-service-CI`): stages Build & Test + SonarQube, and at the end
  calls `build job: 'course-service-CD'` to trigger the second job
- Job 2 (`course-service-CD`): stages Docker Build + Deploy to Kubernetes

### What we fixed along the way (the technical debt we resolved)
These are all real DevOps problems you can explain to your professor:

1. **WSL/NTFS write permissions** — Maven could not write `target/classes/` because
   Jenkins runs in WSL but the workspace is on a Windows NTFS filesystem. Fixed by
   redirecting Maven output to `/tmp` (Linux native FS) using a custom `build.dir`
   property in `pom.xml`.

2. **`-Dproject.build.directory` doesn't work** — Discovered that Maven model
   properties cannot be overridden via command-line `-D` flags. Had to introduce a
   user-defined property `${build.dir}` instead.

3. **Mockito + Java 21 + WSL2** — Mockito's ByteBuddy inline mock maker cannot
   self-attach to the JVM in a restricted WSL2 environment. Fixed by switching to
   the subclass mock maker via `mockito-extensions/org.mockito.plugins.MockMaker`.

4. **Full Spring context test without infrastructure** — `TpFoyerApplicationTests`
   tries to start the full application context including MySQL and RabbitMQ, which
   don't exist in CI. Fixed by creating `src/test/resources/application.properties`
   with H2 in-memory DB and disabled external services (production database
   untouched).

5. **PageImpl JSON serialization** — `Sort.unsorted()` inside `PageImpl` caused an
   `UnsupportedOperationException` during Jackson serialization in the MockMvc test.
   Fixed by providing a real `PageRequest` to the `PageImpl` constructor.

---

### Honest progress estimate

```
Courses microservice CI/CD pipeline:  ~80% done
  (pipeline works end-to-end; K8s deploy needs real cluster)

Other microservices (Enrollment, Lesson, Quiz, ML-Service):  0% — not started

Frontend pipeline:  0% — not started

SonarQube:  50% (wired up, needs before/after screenshots for report)

Kubernetes cluster (kubeadm):  10% (manifests written, no cluster yet)

Monitoring (Prometheus/Grafana):  0% — not started

Overall DevOps sprint completion:  ~20–25%
```

---

## 5. What Still Needs to Be Done

In rough priority order:

### High priority — needed for grading
1. **Screenshot SonarQube "before" state** right now (current code quality baseline)
   then after you improve the code take the "after" screenshot
2. **Repeat the same pipeline for the other microservices** — Enrollment, Lesson,
   Quiz need their own `Jenkinsfile`, tests, and Docker images
3. **Split CI and CD into separate Jenkins jobs** (if professor is strict about it)

### Medium priority
4. **Set up the shared Kubernetes cluster with kubeadm** — this is a group task,
   all members need a VM, one acts as master, others as worker nodes
5. **Deploy to the cluster** — once kubeadm is set up, the `Deploy to Kubernetes`
   stage will go from UNSTABLE to green

### Lower priority (but needed)
6. **Frontend CI/CD pipeline** — one CI job (lint + build + test) and one CD job
   (build Docker image + deploy) shared by the whole group
7. **Monitoring** — Prometheus scraping metrics from your Spring Boot services
   (already has `/actuator/prometheus` because Micrometer is in the pom),
   Grafana dashboards, alerts

---

## Quick Reference Cheatsheet

```bash
# Start everything (run in WSL)
sudo systemctl start jenkins
cd /opt/sonarqube/bin/linux-x86-64 && ./sonar.sh start

# Check Jenkins is up
curl -s http://localhost:8080/api/json | grep '"mode"'

# Check SonarQube is up
curl -s http://localhost:9000/api/system/status | grep '"status"'

# Check Docker is available from WSL
docker info | grep "Server Version"

# View Jenkins build logs from terminal (replace 19 with build number)
curl -s http://localhost:8080/job/ybrainy-course-service/19/consoleText

# See test report
cat /tmp/ybrainy-builds/<N>/target/surefire-reports/*.xml | grep -E "tests=|failures=|errors="
```
