#!/bin/bash
# Fix: containerd cgroupfs vs kubelet systemd cgroup driver mismatch on cgroup v2
# Root cause: pause containers get SIGKILL'd due to cgroup hierarchy conflict
set -e

echo "=== Step 1: Stopping kubelet and clearing stale state ==="
systemctl stop kubelet

# Clear all pod checkpoints (static + regular)
for uid in \
  58fc5e81fba3e5b1fb83102c9913619e \
  4c48cc19127ee1dedeedb447ffc93dab \
  e465936c7ec3d405934be83a8a89db23 \
  46d2efae8c066b9e24ce2d9940dd9ce4; do
  rm -rf "/var/lib/kubelet/pods/$uid" 2>/dev/null || true
done
echo "Static pod checkpoints cleared"

# Remove all containerd sandboxes
crictl rmp $(crictl pods -q 2>/dev/null) 2>/dev/null || true
echo "Containerd sandboxes cleared"

echo ""
echo "=== Step 2: Configuring containerd with SystemdCgroup=true ==="
mkdir -p /etc/containerd
containerd config default > /etc/containerd/config.toml
sed -i 's/SystemdCgroup = false/SystemdCgroup = true/g' /etc/containerd/config.toml
grep 'SystemdCgroup' /etc/containerd/config.toml
echo "containerd config written"

echo ""
echo "=== Step 3: Restarting containerd ==="
systemctl restart containerd
sleep 3
systemctl is-active containerd && echo "containerd OK"

echo ""
echo "=== Step 4: Starting kubelet ==="
systemctl start kubelet
sleep 5
systemctl is-active kubelet && echo "kubelet OK"

echo ""
echo "=== Waiting for API server (up to 120s) ==="
for i in $(seq 1 24); do
  sleep 5
  ok=$(curl -sk --max-time 4 https://127.0.0.1:6443/healthz 2>/dev/null | grep -c '^ok' || true)
  if [ "$ok" -gt 0 ]; then
    echo "API server HEALTHY after $((i*5))s"
    cp /etc/kubernetes/admin.conf /home/azizs/.kube/config
    chown azizs:azizs /home/azizs/.kube/config
    echo ""
    echo "=== Cluster status ==="
    kubectl --kubeconfig=/etc/kubernetes/admin.conf get nodes
    kubectl --kubeconfig=/etc/kubernetes/admin.conf get pods -n kube-system
    exit 0
  fi
  echo "  ...waiting (${i}/24)"
done

echo "TIMEOUT — check: sudo crictl ps -a | grep -E 'etcd|apiserver'"
