#!/bin/bash
# YBrainy DevOps Environment Startup Script
# Run after every PC/WSL2 restart:
#   sudo bash /mnt/c/Users/azizs/Downloads/ESPRIT-PI-4SAE4-2025-2026-Ybrainy/DEVOPS/startup.sh
set -e

REPO_PATH="/mnt/c/Users/azizs/Downloads/ESPRIT-PI-4SAE4-2025-2026-Ybrainy"
AZIZS_HOME="/home/azizs"
JENKINS_HOME="/var/lib/jenkins"
JENKINS_PORT=8086

echo "============================================="
echo " YBrainy DevOps Startup"
echo "============================================="

# ── Step 1: Fix containerd cgroup driver ──────────────────────────
echo ""
echo "[1/7] Checking containerd cgroup driver..."
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

# ── Step 2: Start kubelet ──────────────────────────────────────────
echo ""
echo "[2/7] Starting kubelet..."
systemctl start kubelet 2>/dev/null || true
sleep 5

# ── Step 3: Wait for K8s API server (up to 120s) ──────────────────
echo ""
echo "[3/7] Waiting for Kubernetes API server..."
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

# ── Step 4: Distribute kubeconfig ─────────────────────────────────
echo ""
echo "[4/7] Distributing kubeconfig..."
WSL_IP=$(hostname -I | awk '{print $1}')
cp /etc/kubernetes/admin.conf "$AZIZS_HOME/.kube/config"
chown azizs:azizs "$AZIZS_HOME/.kube/config"
chmod 600 "$AZIZS_HOME/.kube/config"

mkdir -p "$JENKINS_HOME/.kube"
cp /etc/kubernetes/admin.conf "$JENKINS_HOME/.kube/config"
chown jenkins:jenkins "$JENKINS_HOME/.kube/config"
chmod 600 "$JENKINS_HOME/.kube/config"
echo "  → kubeconfig copied to azizs and jenkins"

sed -i "s|server: https://[0-9.]*:6443|server: https://$WSL_IP:6443|g" "$JENKINS_HOME/.kube/config"
sed -i "s|server: https://[0-9.]*:6443|server: https://$WSL_IP:6443|g" "$AZIZS_HOME/.kube/config"
echo "  → WSL2 IP updated to $WSL_IP in kubeconfigs"

# ── Step 5: Start local Docker registry ───────────────────────────
echo ""
echo "[5/7] Starting local Docker registry (localhost:5000)..."
if ! docker ps --format '{{.Names}}' 2>/dev/null | grep -q '^registry$'; then
  docker rm -f registry 2>/dev/null || true
  docker run -d \
    --name registry \
    --restart always \
    -p 5000:5000 \
    -e REGISTRY_STORAGE_DELETE_ENABLED=true \
    registry:2
  echo "  → Registry started at localhost:5000"
else
  echo "  → Registry already running at localhost:5000"
fi

# Configure Docker daemon to allow insecure registry at localhost:5000
if ! grep -q '"localhost:5000"' /etc/docker/daemon.json 2>/dev/null; then
  cat > /etc/docker/daemon.json << 'EOF'
{
  "insecure-registries": ["localhost:5000"],
  "log-driver": "json-file",
  "log-opts": {"max-size": "10m", "max-file": "3"}
}
EOF
  systemctl restart docker 2>/dev/null || true
  sleep 3
  echo "  → Docker daemon configured for insecure registry"
fi

# ── Step 6: Start Jenkins and SonarQube port-forward ──────────────
echo ""
echo "[6/7] Starting Jenkins (port $JENKINS_PORT) and SonarQube port-forward..."
systemctl restart jenkins
echo "  → Jenkins restarted (port $JENKINS_PORT)"

systemctl enable sonarqube-portforward 2>/dev/null || true
systemctl restart sonarqube-portforward
sleep 5
systemctl is-active sonarqube-portforward > /dev/null 2>&1 \
  && echo "  → SonarQube port-forward active (localhost:9000)" \
  || echo "  WARNING: sonarqube-portforward failed"

# ── Step 7: Deploy infrastructure and wait for pods ───────────────
echo ""
echo "[7/7] Ensuring K8s infrastructure is deployed..."
kubectl --kubeconfig=/etc/kubernetes/admin.conf get namespace ybrainy   > /dev/null 2>&1 || kubectl --kubeconfig=/etc/kubernetes/admin.conf create namespace ybrainy
kubectl --kubeconfig=/etc/kubernetes/admin.conf get namespace monitoring > /dev/null 2>&1 || kubectl --kubeconfig=/etc/kubernetes/admin.conf create namespace monitoring

kubectl --kubeconfig=/etc/kubernetes/admin.conf apply --validate=false -f "$REPO_PATH/DEVOPS/infrastructure/mysql.yaml"
kubectl --kubeconfig=/etc/kubernetes/admin.conf apply --validate=false -f "$REPO_PATH/DEVOPS/infrastructure/rabbitmq.yaml"
kubectl --kubeconfig=/etc/kubernetes/admin.conf apply --validate=false -f "$REPO_PATH/DEVOPS/infrastructure/sonarqube.yaml"
kubectl --kubeconfig=/etc/kubernetes/admin.conf apply --validate=false -f "$REPO_PATH/DEVOPS/monitoring/prometheus.yaml"
kubectl --kubeconfig=/etc/kubernetes/admin.conf apply --validate=false -f "$REPO_PATH/DEVOPS/monitoring/grafana.yaml"
kubectl --kubeconfig=/etc/kubernetes/admin.conf apply --validate=false -f "$REPO_PATH/DEVOPS/monitoring/alertmanager.yaml"

echo "  → Infrastructure manifests applied"
echo "  → Waiting 30s for pods to initialize..."
sleep 30

echo ""
echo "============================================="
echo " CLUSTER STATUS"
echo "============================================="
kubectl --kubeconfig=/etc/kubernetes/admin.conf get pods -n ybrainy    2>&1
echo ""
kubectl --kubeconfig=/etc/kubernetes/admin.conf get pods -n default    2>&1 | grep -v "kube-dns\|coredns\|kube-proxy"
echo ""
kubectl --kubeconfig=/etc/kubernetes/admin.conf get pods -n monitoring 2>&1

echo ""
echo "============================================="
echo " SERVICE URLS  (WSL2 IP: $WSL_IP)"
echo "============================================="
echo ""
echo "  Jenkins CI/CD     →  http://localhost:$JENKINS_PORT     (user: aziz)"
echo "  SonarQube         →  http://localhost:9000               (quality gate)"
echo "  Grafana           →  http://$WSL_IP:30300               (admin/ybrainy2026)"
echo "  Prometheus        →  http://$WSL_IP:30090               (metrics)"
echo "  Alertmanager      →  http://$WSL_IP:30093               (alerts)"
echo "  Docker Registry   →  http://localhost:5000              (push target)"
echo ""
echo "  course-service    →  http://$WSL_IP:30082/api/courses"
echo "  lesson-service    →  http://$WSL_IP:30084/api/lessons"
echo "  quiz-service      →  http://$WSL_IP:30083/api/quizzes"
echo "  enrollment-service→  http://$WSL_IP:30085/api/enrollments"
echo ""
echo "  GitHub repo       →  https://github.com/issamrh/ESPRIT-PI-4SAE4-2025-2026-Ybrainy"
echo ""
echo "============================================="
echo " READY. Jenkins pipelines can now be triggered."
echo " Run: sudo bash $REPO_PATH/DEVOPS/setup-webhook-polling.sh"
echo " to enable auto-trigger on git push."
echo "============================================="
