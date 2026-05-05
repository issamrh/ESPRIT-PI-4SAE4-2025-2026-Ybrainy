#!/bin/bash
# Fix etcd instability in WSL2 by tuning heartbeat/election timeouts
# WSL2 has higher I/O latency — default etcd timeouts are too tight
# Run with: sudo bash /mnt/c/Users/azizs/.../DEVOPS/fix-etcd-wsl.sh

set -e
ETCD_MANIFEST="/etc/kubernetes/manifests/etcd.yaml"
KUBECONFIG="/etc/kubernetes/admin.conf"

echo "====================================================="
echo " etcd WSL2 Stability Fix"
echo "====================================================="

# ── Show current crash count ──────────────────────────────────────
echo ""
echo "Current etcd restart count (before fix):"
kubectl --kubeconfig=$KUBECONFIG get pod -n kube-system -l component=etcd \
    -o jsonpath='{.items[0].status.containerStatuses[0].restartCount}' 2>/dev/null \
    && echo " restarts" || echo "(API server not available)"

# ── Show last etcd error ──────────────────────────────────────────
echo ""
echo "Last etcd log lines:"
ls -t /var/log/pods/kube-system_etcd-msi_*/etcd/*.log 2>/dev/null | head -1 \
    | xargs tail -20 2>/dev/null \
    | python3 -c "import sys,json; [print(json.loads(l).get('log','').rstrip()) for l in sys.stdin if l.strip()]" \
    2>/dev/null || echo "(logs not readable)"

# ── Patch etcd manifest: increase heartbeat and election timeouts ─
echo ""
echo "Patching $ETCD_MANIFEST for WSL2 higher latency..."

# Check if already patched
if grep -q "heartbeat-interval=500" "$ETCD_MANIFEST" 2>/dev/null; then
    echo "  Already patched."
else
    # Backup
    cp "$ETCD_MANIFEST" "${ETCD_MANIFEST}.bak.$(date +%Y%m%d%H%M%S)"

    # Add --heartbeat-interval=500 and --election-timeout=5000 if not present
    # Default is 100ms heartbeat and 1000ms election — too tight for WSL2
    python3 << 'PYEOF'
import re

with open("/etc/kubernetes/manifests/etcd.yaml", "r") as f:
    content = f.read()

# Find the command section and add the flags after the last existing -- flag
# We look for the line containing "etcd" binary call and add flags after it
lines = content.split('\n')
new_lines = []
added = False
for i, line in enumerate(lines):
    new_lines.append(line)
    # Find a line like "    - etcd" (the binary)
    if not added and re.match(r'\s+- etcd\s*$', line):
        indent = len(line) - len(line.lstrip())
        spaces = ' ' * indent
        new_lines.append(f"{spaces}- --heartbeat-interval=500")
        new_lines.append(f"{spaces}- --election-timeout=5000")
        added = True

if not added:
    # Try inserting before first -- flag after the container name
    new_lines = []
    for i, line in enumerate(lines):
        new_lines.append(line)
        if not added and '- --advertise-client-urls' in line:
            indent = len(line) - len(line.lstrip())
            spaces = ' ' * indent
            new_lines.insert(-1, f"{spaces}- --heartbeat-interval=500")
            new_lines.insert(-1, f"{spaces}- --election-timeout=5000")
            added = True

with open("/etc/kubernetes/manifests/etcd.yaml", "w") as f:
    f.write('\n'.join(new_lines))

print("  Patched: added --heartbeat-interval=500 --election-timeout=5000")
PYEOF
fi

# ── Restart kubelet to apply the manifest change ─────────────────
echo ""
echo "Restarting kubelet to apply etcd manifest change..."
systemctl restart kubelet
echo "Waiting for API server to come up (up to 120s)..."

for i in $(seq 1 24); do
    sleep 5
    if kubectl --kubeconfig=$KUBECONFIG get nodes --request-timeout=5s > /dev/null 2>&1; then
        echo "API server is UP after $((i*5))s"
        break
    fi
    echo "  ...waiting (${i}/24)"
done

# ── Wait for SonarQube to be ready ───────────────────────────────
echo ""
echo "Waiting for SonarQube portforward to be ready (max 120s)..."
for i in $(seq 1 24); do
    SQ=$(curl -s --max-time 3 "http://localhost:9000/api/system/status" 2>/dev/null | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('status','DOWN'))" 2>/dev/null || echo "DOWN")
    if [ "$SQ" = "UP" ]; then
        echo "  SonarQube is UP after $((i*5))s"
        break
    fi
    echo "  SonarQube status: $SQ ($i/24)"
    sleep 5
done

# ── Show final status ────────────────────────────────────────────
echo ""
echo "====================================================="
echo " Final Status"
echo "====================================================="
echo ""
kubectl --kubeconfig=$KUBECONFIG get pods -n kube-system 2>&1 | head -10 || true
echo ""
echo "SonarQube portforward: $(systemctl is-active sonarqube-portforward.service)"
echo "Port 9000 open:        $(ss -tlnp | grep -c ':9000' || echo 0)"
echo "Port 6443 open:        $(ss -tlnp | grep -c ':6443' || echo 0)"
echo ""
echo "Run builds with:"
echo "  bash /mnt/c/Users/azizs/Downloads/ESPRIT-PI-4SAE4-2025-2026-Ybrainy/DEVOPS/trigger-builds.sh"
