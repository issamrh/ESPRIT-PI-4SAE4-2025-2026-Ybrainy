#!/usr/bin/env bash
set -euo pipefail

namespace="${K8S_NAMESPACE:-ybrainy}"
image_tag="${IMAGE_TAG:-latest}"
docker_registry="${DOCKER_REGISTRY:-}"

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

echo "==> Using namespace: ${namespace}"

kubectl apply --validate=false -f "${repo_root}/k8s/namespace.yaml"
kubectl apply --validate=false -f "${repo_root}/k8s/mysql.yaml"
kubectl apply --validate=false -f "${repo_root}/k8s/rabbitmq.yaml"
kubectl apply --validate=false -f "${repo_root}/k8s/jaeger-zipkin.yaml"
kubectl apply --validate=false -f "${repo_root}/k8s/eureka.yaml"
kubectl apply --validate=false -f "${repo_root}/k8s/api-gateway.yaml"
kubectl apply --validate=false -f "${repo_root}/k8s/cart.yaml"
kubectl apply --validate=false -f "${repo_root}/k8s/payment.yaml"
kubectl apply --validate=false -f "${repo_root}/k8s/finance.yaml"

prefix=""
if [[ -n "${docker_registry}" ]]; then
  prefix="${docker_registry%/}/"
fi

echo "==> Setting service images (tag: ${image_tag})"
kubectl -n "${namespace}" set image deployment/cart-service cart-service="${prefix}cart-service:${image_tag}"
kubectl -n "${namespace}" set image deployment/payment-service payment-service="${prefix}payment-service:${image_tag}"
kubectl -n "${namespace}" set image deployment/finance-service finance-service="${prefix}finance-service:${image_tag}"

echo "==> Waiting for rollouts"
kubectl -n "${namespace}" rollout status deployment/cart-service --timeout=180s
kubectl -n "${namespace}" rollout status deployment/payment-service --timeout=180s
kubectl -n "${namespace}" rollout status deployment/finance-service --timeout=180s

echo "==> Done"
