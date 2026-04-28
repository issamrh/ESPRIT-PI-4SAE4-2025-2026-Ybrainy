#!/usr/bin/env bash
set -euo pipefail

cluster_type="${CLUSTER_TYPE:-none}"
image_tag="${IMAGE_TAG:-latest}"
docker_registry="${DOCKER_REGISTRY:-}"

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

prefix=""
if [[ -n "${docker_registry}" ]]; then
  prefix="${docker_registry%/}/"
fi

build_one() {
  local image_name="$1"
  local context_dir="$2"

  echo "==> Building ${prefix}${image_name}:${image_tag} (context: ${context_dir})"
  docker build -t "${prefix}${image_name}:${image_tag}" "${repo_root}/${context_dir}"

  if [[ -n "${docker_registry}" ]]; then
    echo "==> Pushing ${prefix}${image_name}:${image_tag}"
    docker push "${prefix}${image_name}:${image_tag}"
  fi
}

load_to_cluster() {
  local image_ref="$1"

  case "${cluster_type}" in
    kind)
      echo "==> Loading image into kind: ${image_ref}"
      kind load docker-image "${image_ref}"
      ;;
    minikube)
      echo "==> Loading image into minikube: ${image_ref}"
      minikube image load "${image_ref}"
      ;;
    none)
      ;;
    *)
      echo "Unknown CLUSTER_TYPE: ${cluster_type} (expected none|kind|minikube)" >&2
      exit 2
      ;;
  esac
}

build_one "cart-service" "cart"
build_one "payment-service" "Payment"
build_one "finance-service" "finance"

if [[ -z "${docker_registry}" && "${cluster_type}" != "none" ]]; then
  load_to_cluster "cart-service:${image_tag}"
  load_to_cluster "payment-service:${image_tag}"
  load_to_cluster "finance-service:${image_tag}"
fi

echo "==> Done"

