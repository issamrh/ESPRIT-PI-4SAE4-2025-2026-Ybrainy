#!/bin/bash
# YBrainy DevOps Environment Startup Script
# Run after every PC/WSL2 restart: sudo bash /mnt/c/Users/azizs/Downloads/ESPRIT-PI-4SAE4-2025-2026-Ybrainy/DEVOPS/startup.sh
set -e

REPO_PATH="/mnt/c/Users/azizs/Downloads/ESPRIT-PI-4SAE4-2025-2026-Ybrainy"
AZIZS_HOME="/home/azizs"
JENKINS_HOME="/var/lib/jenkins"

echo "============================================="
echo " YBrainy DevOps Startup"
echo "============================================="

# ── Step 1: Fix containerd cgroup driver (WSL2 cgroup v2 requires systemd) ──
echo ""
echo "[1/6] Checking containerd cgroup driver..."
CGROUP_OK=$(grep -c 'SystemdCgroup = true' /etc/containerd/config.toml 2>/dev/null || echo 0)
if [ "$CGROUP_OK" -eq 0 ]; then
  echo "  → Applying SystemdCgroup=true fix..."
  mkdir -p /etc/containerd
  containerd config default > /etc/containerd/config.toml
  sed -i 's/SystemdCgroup = false/SystemdCgroup = true/g' /etc/containerd/config.toml
  systemctl restart containerd
  sleep 4
  echo "  → containerd fixed"
else
  echo "  → containerd config OK"
fi

systemctl is-active containerd > /dev/null 2>&1 || { systemctl start containerd; sleep 3; }

# ── Step 2: Start kubelet ──
echo ""
echo "[2/6] Starting kubelet..."
systemctl start kubelet 2>/dev/null || true
sleep 5

# ── Step 3: Wait for K8s API server (up to 120s) ──
echo ""
echo "[3/6] Waiting for Kubernetes API server..."
API_READY=0
for i in $(seq 1 24); do
  sleep 5
  OK=$(curl -sk --max-time 4 https://127.0.0.1:6443/healthz 2>/dev/null | grep -c 'ok' || echo 0)
  if [ "$OK" -gt 0 ]; then
    echo "  → API server ready after $((i*5))s"
    API_READY=1
    break
  fi
  echo "  ...waiting ($((i*5))/120s)"
done

if [ "$API_READY" -eq 0 ]; then
  echo ""
  echo "ERROR: K8s API server did not become ready."
  echo "Try running the full cgroup fix:"
  echo "  sudo bash $REPO_PATH/DEVOPS/fix-containerd-cgroup.sh"
  exit 1
fi

# ── Step 4: Distribute kubeconfig ──
echo ""
echo "[4/6] Distributing kubeconfig..."
cp /etc/kubernetes/admin.conf "$AZIZS_HOME/.kube/config"
chown azizs:azizs "$AZIZS_HOME/.kube/config"
chmod 600 "$AZIZS_HOME/.kube/config"

mkdir -p "$JENKINS_HOME/.kube"
cp /etc/kubernetes/admin.conf "$JENKINS_HOME/.kube/config"
chown jenkins:jenkins "$JENKINS_HOME/.kube/config"
chmod 600 "$JENKINS_HOME/.kube/config"
echo "  → kubeconfig copied to azizs and jenkins"

# Update WSL2 IP in Jenkins kubeconfig (it can change on each WSL2 restart)
WSL_IP=$(hostname -I | awk '{print $1}')
sed -i "s|server: https://[0-9.]*:6443|server: https://$WSL_IP:6443|g" "$JENKINS_HOME/.kube/config"
sed -i "s|server: https://[0-9.]*:6443|server: https://$WSL_IP:6443|g" "$AZIZS_HOME/.kube/config"
echo "  → WSL2 IP updated to $WSL_IP in kubeconfigs"

# ── Step 5: Start/restart Jenkins and SonarQube port-forward ──
echo ""
echo "[5/6] Starting Jenkins and SonarQube port-forward..."

# Jenkins is enabled so it likely auto-started, but restart to ensure it has the new kubeconfig
systemctl restart jenkins
echo "  → Jenkins restarted"

# SonarQube port-forward: maps K8s SonarQube pod → localhost:9000 (used by Jenkins pipeline)
systemctl enable sonarqube-portforward 2>/dev/null || true
systemctl restart sonarqube-portforward
sleep 5
systemctl is-active sonarqube-portforward > /dev/null 2>&1 && echo "  → SonarQube port-forward active (localhost:9000)" || echo "  WARNING: sonarqube-portforward failed to start"

# ── Step 6: Wait for pods and print status ──
echo ""
echo "[6/6] Waiting for pods (30s)..."
sleep 30

echo ""
echo "============================================="
echo " CLUSTER STATUS"
echo "============================================="
kubectl --kubeconfig=/etc/kubernetes/admin.conf get pods -n ybrainy 2>&1
echo ""
kubectl --kubeconfig=/etc/kubernetes/admin.conf get pods -n default 2>&1 | grep -v kube-system
echo ""
kubectl --kubeconfig=/etc/kubernetes/admin.conf get pods -n monitoring 2>&1

echo ""
echo "============================================="
echo " SERVICE URLS  (WSL2 IP: $WSL_IP)"
echo "============================================="
echo ""
echo "  Jenkins CI/CD     →  http://$WSL_IP:8080"
echo "  SonarQube         →  http://$WSL_IP:30900  (login: admin / admin)"
echo "  Prometheus        →  http://$WSL_IP:30090"
echo "  Grafana           →  http://$WSL_IP:30300  (login: admin / admin)"
echo ""
echo "  course-service    →  http://$WSL_IP:30082/api/courses"
echo "  lesson-service    →  http://$WSL_IP:30084/api/lessons"
echo "  quiz-service      →  http://$WSL_IP:30083/api/quizzes"
echo "  enrollment-service→  http://$WSL_IP:30085/api/enrollments"
echo ""
echo "  GitHub Actions    →  https://github.com/issamrh/ESPRIT-PI-4SAE4-2025-2026-Ybrainy/actions"
echo ""
echo "============================================="
echo " READY. Jenkins pipelines can now be triggered."
echo "============================================="
