# YBrainy DevOps Additions

This folder adds three practical upgrades on top of the current Jenkins + Kubernetes setup:

- `fluxcd/` for GitOps synchronization
- `gatekeeper/` for policy-as-code checks
- Jenkins email notifications in the service pipelines

These additions are intentionally non-breaking:

- the current Jenkins CI/CD jobs still work
- Gatekeeper constraints are created in `dryrun` mode first
- FluxCD manifests are additive and can be enabled gradually

## 1. FluxCD

FluxCD lets the cluster pull the desired state from Git instead of relying only on `kubectl apply` from Jenkins.

### Install from WSL

```bash
curl -s https://fluxcd.io/install.sh | sudo bash
flux check --pre
kubectl create namespace flux-system
flux install --namespace flux-system
```

### Apply the YBrainy Flux resources

From the repository root:

```bash
kubectl apply -f "ybrainy events/devops/fluxcd/gitrepository.yaml"
kubectl apply -f "ybrainy events/devops/fluxcd/kustomization-services.yaml"
kubectl apply -f "ybrainy events/devops/fluxcd/kustomization-monitoring.yaml"
kubectl apply -f "ybrainy events/devops/fluxcd/image-repositories.yaml"
kubectl apply -f "ybrainy events/devops/fluxcd/image-policies.yaml"
```

### What Flux does here

- watches the `integration-final` branch
- syncs service manifests from the existing `event-service`, `feedback-service`, and `inscription-service` Kubernetes folders
- syncs monitoring manifests from `ybrainy events/monitoring`
- prepares image repositories and policies for Docker Hub

The sample `GitRepository` uses the public GitHub URL directly, so no Flux Git credential secret is required for the first setup.

Note:

- automatic image mutation is not enabled yet because the current deployment manifests still use static image tags and Jenkins CD does `kubectl set image`
- once you decide to move fully to GitOps, we can replace the direct Jenkins deployment stage with Flux image automation

## 2. OPA Gatekeeper

Gatekeeper adds Kubernetes admission policies.

### Install from WSL

```bash
helm repo add gatekeeper https://open-policy-agent.github.io/gatekeeper/charts
helm repo update
helm install gatekeeper gatekeeper/gatekeeper \
  --namespace gatekeeper-system \
  --create-namespace
```

### Apply the YBrainy policies

```bash
kubectl apply -f "ybrainy events/devops/gatekeeper/k8sdisallowlatesttag-template.yaml"
kubectl apply -f "ybrainy events/devops/gatekeeper/k8srequiredresources-template.yaml"
kubectl apply -f "ybrainy events/devops/gatekeeper/disallow-latest-dryrun.yaml"
kubectl apply -f "ybrainy events/devops/gatekeeper/require-resources-dryrun.yaml"
```

### What the policies check

- deployments should not use `:latest`
- deployments should define CPU and memory requests/limits

The constraints are currently `dryrun`, which means:

- violations are reported
- deployments are not blocked yet

This is ideal for the current repo, because some manifests still use `latest` and do not yet define resource limits.

## 3. Jenkins Email Notifications

The Jenkinsfiles now send email when CI or CD fails.

### Jenkins setup steps

1. Install the `Email Extension Plugin`
2. Open `Manage Jenkins` -> `System`
3. Configure SMTP under `Extended E-mail Notification`
4. Set:
   - SMTP server
   - SMTP port
   - credentials if your provider requires authentication
   - default suffix and sender address
5. Use the `Test configuration` button to send a validation email

### Optional secure credential setup

If your SMTP provider needs a username/password:

1. Open `Manage Jenkins` -> `Credentials`
2. Add a username/password credential for the SMTP account
3. Reference it in the Jenkins global email configuration

### Pipeline configuration

The following pipelines now use `emailext` on failure:

- `event-service/Jenkinsfile.ci`
- `event-service/Jenkinsfile.cd`
- `feedback-service/Jenkinsfile.ci`
- `feedback-service/Jenkinsfile.cd`
- `inscription-service/Jenkinsfile.ci`
- `inscription-service/Jenkinsfile.cd`

By default, they send to:

```text
devops@ybrainy.local
```

Replace that placeholder with the real team email address you want.

### WSL requirement for email

No WSL step is needed for Jenkins email itself.

WSL is only relevant here for:

- `mvn`
- `docker`
- `kubectl`
- `flux`
- `helm`

## Suggested rollout order

1. Configure Jenkins SMTP and test one pipeline failure email
2. Install FluxCD and apply the Git source + Kustomizations
3. Install Gatekeeper and keep constraints in `dryrun`
4. Review violations
5. Harden deployment manifests with fixed image tags and resource limits
6. Switch Gatekeeper constraints from `dryrun` to enforcement
