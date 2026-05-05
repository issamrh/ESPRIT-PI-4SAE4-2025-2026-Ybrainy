#!/bin/bash
# YBrainy DevOps Fix Script v2
# Run with: sudo bash /mnt/c/Users/azizs/Downloads/ESPRIT-PI-4SAE4-2025-2026-Ybrainy/DEVOPS/fix-devops.sh
# Idempotent — safe to run multiple times.

KUBECONFIG_PATH=/etc/kubernetes/admin.conf
DEVOPS_BASE="/mnt/c/Users/azizs/Downloads/ESPRIT-PI-4SAE4-2025-2026-Ybrainy/DEVOPS"
export KUBECONFIG=$KUBECONFIG_PATH

echo "============================================================"
echo " YBrainy DevOps Fix Script"
echo "============================================================"

# ── 1. WSL swap config ────────────────────────────────────────────
echo ""
echo "[1/7] Checking /etc/wsl.conf for swap=0..."
if grep -q "^swap=0" /etc/wsl.conf 2>/dev/null; then
    echo "      Already configured."
else
    if grep -q "\[wsl2\]" /etc/wsl.conf 2>/dev/null; then
        sed -i '/\[wsl2\]/a swap=0' /etc/wsl.conf
    else
        printf '\n[wsl2]\nswap=0\n' >> /etc/wsl.conf
    fi
    echo "      Added swap=0 to /etc/wsl.conf"
fi
swapoff -a 2>/dev/null || true
echo "      swapoff -a applied (swap=$(free | awk '/Swap/{print $2}')KB total)"

# ── 2. Fix Jenkins kubeconfig ────────────────────────────────────
echo ""
echo "[2/7] Fixing Jenkins kubeconfig (/var/lib/jenkins/.kube/config)..."
mkdir -p /var/lib/jenkins/.kube
cp $KUBECONFIG_PATH /var/lib/jenkins/.kube/config
chown jenkins:jenkins /var/lib/jenkins/.kube/config
chmod 600 /var/lib/jenkins/.kube/config
echo "      Copied fresh admin.conf to /var/lib/jenkins/.kube/config"

# ── 3. Restart kubelet to clear CrashLoopBackOff back-off ─────────
echo ""
echo "[3/7] Restarting kubelet to clear CrashLoopBackOff back-off..."
systemctl restart kubelet
echo "      kubelet restarted. Waiting 30s for control plane to initialize..."
sleep 10

# Poll for kube-apiserver to come up (max 120s)
APISERVER_UP=0
for i in $(seq 1 24); do
    if kubectl --kubeconfig=$KUBECONFIG_PATH get nodes --request-timeout=5s > /dev/null 2>&1; then
        APISERVER_UP=1
        break
    fi
    echo "      Waiting for API server... (${i}/24)"
    sleep 5
done

if [ "$APISERVER_UP" -eq 0 ]; then
    echo ""
    echo "  !! API server did not come up within 120s."
    echo "     Checking etcd status..."
    kubectl --kubeconfig=$KUBECONFIG_PATH get pods -n kube-system 2>&1 | head -15 || true
    echo ""
    echo "  Try running this script again after 2-3 minutes."
    echo "  If etcd keeps crashing, check: journalctl -u kubelet -n 50"
    exit 1
fi

echo "      API server is UP."
kubectl --kubeconfig=$KUBECONFIG_PATH get nodes 2>&1 || true

# ── 4. Deploy SonarQube to default namespace ──────────────────────
echo ""
echo "[4/7] Checking SonarQube pod in default namespace..."
SQ_POD=$(kubectl --kubeconfig=$KUBECONFIG_PATH get pods -n default -l app=sonarqube \
         --no-headers 2>/dev/null | grep -v Terminating | wc -l)
if [ "$SQ_POD" -eq 0 ]; then
    echo "      SonarQube pod not found — deploying..."
    kubectl --kubeconfig=$KUBECONFIG_PATH apply --validate=false \
        -f "$DEVOPS_BASE/infrastructure/sonarqube.yaml"
    echo "      SonarQube deployment applied. Allow 60-90s for it to start."
else
    echo "      SonarQube pod already exists."
    kubectl --kubeconfig=$KUBECONFIG_PATH get pods -n default -l app=sonarqube 2>&1 || true
fi

# Restart SonarQube portforward (now that K8s and Jenkins kubeconfig are fixed)
echo ""
echo "      Restarting sonarqube-portforward.service..."
systemctl daemon-reload
systemctl restart sonarqube-portforward.service
sleep 5
SQ_STATUS=$(systemctl is-active sonarqube-portforward.service)
echo "      sonarqube-portforward.service: $SQ_STATUS"
if [ "$SQ_STATUS" != "active" ]; then
    journalctl -u sonarqube-portforward.service -n 5 --no-pager
fi

# ── 5. Ensure ybrainy namespace ───────────────────────────────────
echo ""
echo "[5/7] Ensuring ybrainy namespace exists..."
kubectl --kubeconfig=$KUBECONFIG_PATH get namespace ybrainy > /dev/null 2>&1 \
    && echo "      ybrainy namespace already exists." \
    || { kubectl --kubeconfig=$KUBECONFIG_PATH create namespace ybrainy \
         && echo "      Created ybrainy namespace."; }

# ── 6. Apply infrastructure manifests ────────────────────────────
echo ""
echo "[6/7] Applying infrastructure (MySQL, RabbitMQ)..."
kubectl --kubeconfig=$KUBECONFIG_PATH apply --validate=false \
    -f "$DEVOPS_BASE/infrastructure/mysql.yaml"
kubectl --kubeconfig=$KUBECONFIG_PATH apply --validate=false \
    -f "$DEVOPS_BASE/infrastructure/rabbitmq.yaml"
echo "      Infrastructure manifests applied."

# ── 7. Deploy monitoring stack ───────────────────────────────────
echo ""
echo "[7/7] Deploying Prometheus + Grafana monitoring stack..."
kubectl --kubeconfig=$KUBECONFIG_PATH apply --validate=false \
    -f "$DEVOPS_BASE/monitoring/prometheus.yaml"
kubectl --kubeconfig=$KUBECONFIG_PATH apply --validate=false \
    -f "$DEVOPS_BASE/monitoring/grafana.yaml"
echo "      Monitoring stack applied."

# ── Status summary ───────────────────────────────────────────────
echo ""
echo "============================================================"
echo " Cluster Status"
echo "============================================================"
echo ""
echo "kube-system pods:"
kubectl --kubeconfig=$KUBECONFIG_PATH get pods -n kube-system 2>&1 || true

echo ""
echo "ybrainy namespace pods:"
kubectl --kubeconfig=$KUBECONFIG_PATH get pods -n ybrainy 2>&1 || true

echo ""
echo "monitoring namespace pods:"
kubectl --kubeconfig=$KUBECONFIG_PATH get pods -n monitoring 2>&1 || true

NODE_IP=$(kubectl --kubeconfig=$KUBECONFIG_PATH get nodes \
    -o jsonpath='{.items[0].status.addresses[?(@.type=="InternalIP")].address}' \
    2>/dev/null || echo "172.22.108.68")

echo ""
echo "============================================================"
echo " Access URLs"
echo "============================================================"
echo "  Jenkins:    http://localhost:8086"
echo "  SonarQube:  http://localhost:9000   (wait 90s after first deploy)"
echo "  Prometheus: http://${NODE_IP}:30090  (available after pods start)"
echo "  Grafana:    http://${NODE_IP}:30300  admin / ybrainy2026"
echo ""
echo " NodePort access from Windows:"
echo "  course-service:    http://${NODE_IP}:30082"
echo "  lesson-service:    http://${NODE_IP}:30084"
echo "  quiz-service:      http://${NODE_IP}:30083"
echo "  enrollment-service: http://${NODE_IP}:30085"
echo ""
echo "============================================================"
echo " Next Steps"
echo "============================================================"
echo "  1. git push origin 3la-5atr-houssem  (Jenkins reads from GitHub)"
echo "  2. Jenkins → Build Now → course-service-CI (auto-triggers CD)"
echo "  3. Repeat for lesson, quiz, enrollment CI jobs"
echo "  4. After first CI run: screenshot SonarQube at localhost:9000"
echo "  5. Grafana dashboards to import: 4701 (JVM), 9964 (Jenkins), 12900 (Spring Boot)"
echo ""
