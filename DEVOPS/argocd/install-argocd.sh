#!/bin/bash
# Install ArgoCD on the kubeadm cluster and configure YBrainy GitOps
# Run: sudo bash .../DEVOPS/argocd/install-argocd.sh
# Takes ~3 minutes. ArgoCD UI will be at http://WSL_IP:30808

set -e
KUBECONFIG_PATH=/etc/kubernetes/admin.conf
REPO_URL="https://github.com/issamrh/ESPRIT-PI-4SAE4-2025-2026-Ybrainy.git"
BRANCH="3la-5atr-houssem"

echo "============================================="
echo " YBrainy ArgoCD GitOps Setup"
echo "============================================="

# ── 1. Install ArgoCD ─────────────────────────────────────────────
echo ""
echo "[1/4] Installing ArgoCD..."
kubectl --kubeconfig=$KUBECONFIG_PATH create namespace argocd 2>/dev/null || true
kubectl --kubeconfig=$KUBECONFIG_PATH apply -n argocd \
    -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

echo "  → Waiting for ArgoCD pods (up to 3 minutes)..."
kubectl --kubeconfig=$KUBECONFIG_PATH wait pod \
    -n argocd \
    -l app.kubernetes.io/name=argocd-server \
    --for=condition=Ready \
    --timeout=180s

# ── 2. Expose ArgoCD UI via NodePort ─────────────────────────────
echo ""
echo "[2/4] Exposing ArgoCD UI on NodePort 30808..."
kubectl --kubeconfig=$KUBECONFIG_PATH -n argocd patch svc argocd-server \
    -p '{"spec":{"type":"NodePort","ports":[{"port":443,"targetPort":8080,"nodePort":30808,"name":"https"}]}}'

# ── 3. Get initial admin password ────────────────────────────────
echo ""
echo "[3/4] Getting ArgoCD admin password..."
ARGOCD_PASS=$(kubectl --kubeconfig=$KUBECONFIG_PATH \
    -n argocd get secret argocd-initial-admin-secret \
    -o jsonpath="{.data.password}" | base64 -d)
echo "  → admin password: $ARGOCD_PASS"
echo "$ARGOCD_PASS" > /tmp/argocd-initial-password.txt

# ── 4. Create ArgoCD Application for YBrainy ─────────────────────
echo ""
echo "[4/4] Creating YBrainy ArgoCD Application..."
kubectl --kubeconfig=$KUBECONFIG_PATH apply -f - << EOF
apiVersion: argoproj.io/v1alpha1
kind: AppProject
metadata:
  name: ybrainy
  namespace: argocd
spec:
  description: YBrainy E-Learning Platform
  sourceRepos:
    - '$REPO_URL'
  destinations:
    - namespace: ybrainy
      server: https://kubernetes.default.svc
    - namespace: monitoring
      server: https://kubernetes.default.svc
    - namespace: default
      server: https://kubernetes.default.svc
  clusterResourceWhitelist:
    - group: '*'
      kind: '*'
---
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: ybrainy-services
  namespace: argocd
  annotations:
    argocd.argoproj.io/sync-wave: "1"
spec:
  project: ybrainy
  source:
    repoURL: $REPO_URL
    targetRevision: $BRANCH
    path: DEVOPS
    directory:
      recurse: true
      include: '**/*.yaml'
      exclude: 'monitoring/**,infrastructure/**'
  destination:
    server: https://kubernetes.default.svc
    namespace: ybrainy
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
      - ServerSideApply=true
---
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: ybrainy-infrastructure
  namespace: argocd
spec:
  project: ybrainy
  source:
    repoURL: $REPO_URL
    targetRevision: $BRANCH
    path: DEVOPS/infrastructure
  destination:
    server: https://kubernetes.default.svc
    namespace: default
  syncPolicy:
    automated:
      prune: false
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
---
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: ybrainy-monitoring
  namespace: argocd
spec:
  project: ybrainy
  source:
    repoURL: $REPO_URL
    targetRevision: $BRANCH
    path: DEVOPS/monitoring
  destination:
    server: https://kubernetes.default.svc
    namespace: monitoring
  syncPolicy:
    automated:
      prune: false
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
EOF

WSL_IP=$(hostname -I | awk '{print $1}')
echo ""
echo "============================================="
echo " ArgoCD READY"
echo "============================================="
echo ""
echo "  UI:       https://$WSL_IP:30808"
echo "  Username: admin"
echo "  Password: $ARGOCD_PASS"
echo "  (also saved to /tmp/argocd-initial-password.txt)"
echo ""
echo "  YBrainy apps: 3 applications configured"
echo "  Git repo: $REPO_URL"
echo "  Branch:   $BRANCH"
echo ""
echo "  From now on, git push → ArgoCD auto-syncs K8s state"
echo "============================================="
