#!/bin/bash
# YBrainy DevOps Fix Script
# Run this ONCE in your WSL terminal with: sudo bash /mnt/c/Users/azizs/Downloads/ESPRIT-PI-4SAE4-2025-2026-Ybrainy/DEVOPS/fix-devops.sh
# This script requires sudo and fixes:
#   1. SonarQube portforward (updates Jenkins kubeconfig)
#   2. WSL swap config (/etc/wsl.conf)
#   3. Creates ybrainy namespace in K8s
#   4. Deploys infrastructure (MySQL, RabbitMQ) to ybrainy namespace
#   5. Deploys monitoring namespace + Prometheus + Grafana

set -e

KUBECONFIG_PATH=/etc/kubernetes/admin.conf
DEVOPS_BASE="/mnt/c/Users/azizs/Downloads/ESPRIT-PI-4SAE4-2025-2026-Ybrainy/DEVOPS"
export KUBECONFIG=$KUBECONFIG_PATH

echo "============================================================"
echo " YBrainy DevOps Fix Script"
echo "============================================================"

# ── 1. Fix WSL swap config ────────────────────────────────────────
echo ""
echo "[1/6] Checking /etc/wsl.conf for swap=0..."
if grep -q "^swap=0" /etc/wsl.conf 2>/dev/null; then
    echo "      Already configured."
else
    # Add or create [wsl2] section with swap=0
    if grep -q "\[wsl2\]" /etc/wsl.conf 2>/dev/null; then
        sed -i '/\[wsl2\]/a swap=0' /etc/wsl.conf
    else
        printf '\n[wsl2]\nswap=0\n' >> /etc/wsl.conf
    fi
    echo "      Added swap=0 to /etc/wsl.conf"
fi

# Also disable swap immediately for this session
swapoff -a 2>/dev/null || true
echo "      swapoff -a applied."

# ── 2. Fix Jenkins kubeconfig ────────────────────────────────────
echo ""
echo "[2/6] Fixing Jenkins kubeconfig (/var/lib/jenkins/.kube/config)..."
mkdir -p /var/lib/jenkins/.kube
cp $KUBECONFIG_PATH /var/lib/jenkins/.kube/config
chown jenkins:jenkins /var/lib/jenkins/.kube/config
chmod 600 /var/lib/jenkins/.kube/config
echo "      Copied fresh admin.conf to /var/lib/jenkins/.kube/config"

# ── 3. Ensure SonarQube pod exists in default namespace ──────────
echo ""
echo "[3a/6] Checking SonarQube pod in default namespace..."
SQ_POD=$(kubectl --kubeconfig=$KUBECONFIG_PATH get pods -n default -l app=sonarqube --no-headers 2>/dev/null | grep -v Terminating | wc -l)
if [ "$SQ_POD" -eq 0 ]; then
    echo "      SonarQube pod not found — deploying..."
    kubectl --kubeconfig=$KUBECONFIG_PATH apply -f "$DEVOPS_BASE/infrastructure/sonarqube.yaml"
    echo "      SonarQube deployment applied. Wait ~60s for it to start."
else
    echo "      SonarQube pod already running."
fi

# ── 3. Restart SonarQube portforward service ─────────────────────
echo ""
echo "[3b/6] Restarting sonarqube-portforward.service..."
systemctl daemon-reload
systemctl restart sonarqube-portforward.service
sleep 5
STATUS=$(systemctl is-active sonarqube-portforward.service)
if [ "$STATUS" = "active" ]; then
    echo "      sonarqube-portforward.service is now ACTIVE"
    echo "      SonarQube should be accessible at http://localhost:9000"
else
    echo "      Service status: $STATUS — checking logs..."
    journalctl -u sonarqube-portforward.service -n 10 --no-pager
    echo ""
    echo "      NOTE: If SonarQube pod is still starting, the portforward will"
    echo "      auto-restart every 10s. Wait 60-90s for SonarQube to be ready."
fi

# ── 4. Ensure ybrainy namespace exists ───────────────────────────
echo ""
echo "[4/6] Ensuring ybrainy namespace exists..."
kubectl --kubeconfig=$KUBECONFIG_PATH get namespace ybrainy > /dev/null 2>&1 && \
    echo "      ybrainy namespace already exists." || \
    kubectl --kubeconfig=$KUBECONFIG_PATH create namespace ybrainy && \
    echo "      Created ybrainy namespace."

# ── 5. Apply infrastructure manifests ────────────────────────────
echo ""
echo "[5/6] Applying infrastructure (MySQL, RabbitMQ)..."
kubectl --kubeconfig=$KUBECONFIG_PATH apply -f "$DEVOPS_BASE/infrastructure/mysql.yaml"
kubectl --kubeconfig=$KUBECONFIG_PATH apply -f "$DEVOPS_BASE/infrastructure/rabbitmq.yaml"
echo "      Infrastructure manifests applied."

# ── 6. Deploy monitoring stack ───────────────────────────────────
echo ""
echo "[6/6] Deploying Prometheus + Grafana monitoring stack..."
kubectl --kubeconfig=$KUBECONFIG_PATH apply -f "$DEVOPS_BASE/monitoring/prometheus.yaml"
kubectl --kubeconfig=$KUBECONFIG_PATH apply -f "$DEVOPS_BASE/monitoring/grafana.yaml"
echo "      Monitoring stack applied."

# ── Status summary ───────────────────────────────────────────────
echo ""
echo "============================================================"
echo " Status Check"
echo "============================================================"
echo ""
echo "Pods in ybrainy namespace:"
kubectl --kubeconfig=$KUBECONFIG_PATH get pods -n ybrainy 2>&1 || true

echo ""
echo "Pods in monitoring namespace:"
kubectl --kubeconfig=$KUBECONFIG_PATH get pods -n monitoring 2>&1 || true

NODE_IP=$(kubectl --kubeconfig=$KUBECONFIG_PATH get nodes -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}' 2>/dev/null || echo "172.22.108.68")

echo ""
echo "============================================================"
echo " Access URLs (after pods reach Running state)"
echo "============================================================"
echo "  Jenkins:    http://localhost:8086"
echo "  SonarQube:  http://localhost:9000"
echo "  Prometheus: http://${NODE_IP}:30090"
echo "  Grafana:    http://${NODE_IP}:30300  (admin / ybrainy2026)"
echo ""
echo " To get the correct node IP from Windows:"
echo "   wsl -- bash -c \"kubectl --kubeconfig=/etc/kubernetes/admin.conf get nodes -o wide\""
echo ""
echo "============================================================"
echo " Next Steps"
echo "============================================================"
echo "  1. Push this branch to GitHub (triggers Jenkins SCM poll)"
echo "  2. In Jenkins, run course-service-CI first (it will trigger course-service-CD)"
echo "  3. After CI runs, check SonarQube at http://localhost:9000"
echo "  4. Take 'after' SonarQube screenshot for the report"
echo "     (before screenshots are already in DEVOPS/sonarqube-before-pictures/)"
echo "  5. In Grafana, import dashboards:"
echo "     - Spring Boot dashboard: ID 12900"
echo "     - JVM dashboard: ID 4701"
echo "     - Jenkins dashboard: ID 9964"
echo ""
