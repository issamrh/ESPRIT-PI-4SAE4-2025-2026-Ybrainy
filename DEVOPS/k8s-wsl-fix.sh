#!/bin/bash
# K8s WSL2 stability pre-flight script
# Runs ONCE at boot, before kubelet starts.
# DO NOT restart kubelet here — doing so increments restart counters
# and causes exponential back-off on etcd/apiserver.

set -e

# Apply sysctl tuning (also in /etc/sysctl.conf but explicit here for safety)
sysctl -w vm.max_map_count=262144
sysctl -w kernel.panic=10
sysctl -w kernel.panic_on_oops=1
sysctl -w net.ipv4.ip_forward=1

# Disable swap (also in wsl.conf but enforce here)
swapoff -a 2>/dev/null || true

# Increase inotify limits for many pods
sysctl -w fs.inotify.max_user_watches=524288
sysctl -w fs.inotify.max_user_instances=512

echo "K8s WSL2 pre-flight complete"
