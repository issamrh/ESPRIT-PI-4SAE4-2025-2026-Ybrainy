#!/bin/bash
# Clear stale kubelet checkpoints for all control-plane static pods
# Root cause: stale pod worker state causes kubelet to send SIGTERM to running containers
set -e

echo "=== Clearing stale control-plane pod checkpoints ==="
systemctl stop kubelet
echo "kubelet stopped"

# Clear stale checkpoints: kube-apiserver, kube-controller-manager, kube-scheduler
for uid in \
  4c48cc19127ee1dedeedb447ffc93dab \
  e465936c7ec3d405934be83a8a89db23 \
  46d2efae8c066b9e24ce2d9940dd9ce4; do
  if [ -d "/var/lib/kubelet/pods/$uid" ]; then
    rm -rf "/var/lib/kubelet/pods/$uid"
    echo "Cleared checkpoint: $uid"
  fi
done

systemctl start kubelet
echo "kubelet started"

echo ""
echo "Waiting for API server (up to 90s)..."
for i in $(seq 1 18); do
  sleep 5
  ok=$(curl -sk --max-time 4 https://127.0.0.1:6443/healthz 2>/dev/null | grep -c 'ok' || true)
  etcd_fail=$(curl -sk --max-time 4 https://127.0.0.1:6443/healthz 2>/dev/null | grep -c 'etcd failed' || true)
  if [ "$ok" -gt 10 ] && [ "$etcd_fail" -eq 0 ]; then
    echo "API server HEALTHY after $((i*5))s"
    cp /etc/kubernetes/admin.conf /home/azizs/.kube/config
    chown azizs:azizs /home/azizs/.kube/config
    echo ""
    echo "=== Cluster status ==="
    kubectl --kubeconfig=/etc/kubernetes/admin.conf get nodes
    kubectl --kubeconfig=/etc/kubernetes/admin.conf get pods -n kube-system
    exit 0
  fi
  echo "  ...waiting (${i}/18) ok_checks=${ok} etcd_failed=${etcd_fail}"
done

echo "TIMEOUT — API not healthy after 90s"
curl -sk --max-time 4 https://127.0.0.1:6443/healthz 2>/dev/null | head -5
