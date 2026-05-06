# ArgoCD GitOps — YBrainy Platform

ArgoCD watches the `DEVOPS/` folder in this repository. Any change pushed to `3la-5atr-houssem` branch is automatically synced to the Kubernetes cluster within 3 minutes (or instantly via "Sync" in the UI).

## Setup (one-time)

```bash
sudo bash /mnt/c/Users/azizs/Downloads/ESPRIT-PI-4SAE4-2025-2026-Ybrainy/DEVOPS/argocd/install-argocd.sh
```

## Access

| Item | Value |
|------|-------|
| UI | `https://<WSL_IP>:30808` |
| Username | `admin` |
| Password | Run `kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d` |

## Applications

| App | Source Path | Target Namespace |
|-----|------------|-----------------|
| `ybrainy-services` | `DEVOPS/course-service/`, `lesson-service/`, etc. | `ybrainy` |
| `ybrainy-infrastructure` | `DEVOPS/infrastructure/` | `default` |
| `ybrainy-monitoring` | `DEVOPS/monitoring/` | `monitoring` |

## GitOps Flow

```
git push origin 3la-5atr-houssem
        ↓
ArgoCD detects diff (polls every 3min)
        ↓
ArgoCD applies changed manifests to K8s
        ↓
K8s rolling-updates pods
        ↓
ArgoCD UI shows "Synced ✓"
```

## Demo Script for Professor

1. Open `https://<WSL_IP>:30808` — show 3 green "Synced" apps
2. Change `replicas: 1` → `replicas: 2` in any deployment YAML
3. `git push origin 3la-5atr-houssem`
4. In ArgoCD UI, click "Refresh" — shows "OutOfSync"
5. Wait 3 minutes OR click "Sync" — ArgoCD deploys automatically
6. `kubectl get pods -n ybrainy` — shows 2 pods for that service
