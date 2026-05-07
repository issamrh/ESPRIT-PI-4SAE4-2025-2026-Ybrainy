#Requires -Version 7
# START-EVERYTHING.ps1 — YBrainy Platform Full Startup Script
# All Docker operations use WSL native Docker Engine (docker-ce), NOT Docker Desktop.
# Run from PowerShell as Administrator.

$WSL_DISTRO = "Ubuntu"
$WSL_IP     = "172.22.108.68"   # update if WSL IP changes (check: wsl hostname -I)
$SERVICES   = @(
    @{ name="Jenkins";     url="http://localhost:8086"; check={ (Invoke-WebRequest -Uri "http://localhost:8086/login" -UseBasicParsing -TimeoutSec 5 -ErrorAction SilentlyContinue).StatusCode -eq 200 } }
    @{ name="SonarQube";   url="http://${WSL_IP}:30900"; check={ (Invoke-WebRequest -Uri "http://${WSL_IP}:30900/api/server/version" -UseBasicParsing -TimeoutSec 5 -ErrorAction SilentlyContinue).StatusCode -eq 200 } }
    @{ name="Prometheus";  url="http://${WSL_IP}:30090"; check={ (Invoke-WebRequest -Uri "http://${WSL_IP}:30090/-/healthy" -UseBasicParsing -TimeoutSec 5 -ErrorAction SilentlyContinue).StatusCode -eq 200 } }
    @{ name="Grafana";     url="http://${WSL_IP}:30300"; check={ (Invoke-WebRequest -Uri "http://${WSL_IP}:30300/api/health" -UseBasicParsing -TimeoutSec 5 -ErrorAction SilentlyContinue).StatusCode -eq 200 } }
    @{ name="Course-Svc";  url="http://${WSL_IP}:30082/actuator/health"; check={ (Invoke-WebRequest -Uri "http://${WSL_IP}:30082/actuator/health" -UseBasicParsing -TimeoutSec 5 -ErrorAction SilentlyContinue).StatusCode -eq 200 } }
    @{ name="ML-Service";  url="http://${WSL_IP}:30086/health"; check={ (Invoke-WebRequest -Uri "http://${WSL_IP}:30086/health" -UseBasicParsing -TimeoutSec 5 -ErrorAction SilentlyContinue).StatusCode -eq 200 } }
    @{ name="Frontend";    url="http://${WSL_IP}:30080"; check={ (Invoke-WebRequest -Uri "http://${WSL_IP}:30080" -UseBasicParsing -TimeoutSec 5 -ErrorAction SilentlyContinue).StatusCode -eq 200 } }
)

function Write-Header { param($text) Write-Host "`n$('='*60)" -ForegroundColor Cyan; Write-Host "  $text" -ForegroundColor White; Write-Host "$('='*60)" -ForegroundColor Cyan }
function Write-OK    { param($text) Write-Host "  [OK]  $text" -ForegroundColor Green }
function Write-WARN  { param($text) Write-Host "  [!!]  $text" -ForegroundColor Yellow }
function Write-FAIL  { param($text) Write-Host "  [XX]  $text" -ForegroundColor Red }

Write-Header "STEP 1 — Start WSL Ubuntu"
wsl -d $WSL_DISTRO echo "WSL ready" 2>&1 | Out-Null
Write-OK "WSL Ubuntu is running"

Write-Header "STEP 2 — Start Docker Engine inside WSL (native, not Docker Desktop)"
$dockerStatus = wsl -u root -d $WSL_DISTRO -e bash -c "systemctl is-active docker 2>/dev/null"
if ($dockerStatus -ne "active") {
    Write-WARN "Docker not running — starting via systemctl..."
    wsl -u root -d $WSL_DISTRO -e bash -c "systemctl start docker"
    Start-Sleep -Seconds 5
    $dockerStatus = wsl -u root -d $WSL_DISTRO -e bash -c "systemctl is-active docker 2>/dev/null"
}
if ($dockerStatus -eq "active") {
    $dockerVer = wsl -u root -d $WSL_DISTRO -e bash -c "docker version --format '{{.Server.Version}}' 2>/dev/null"
    Write-OK "WSL Docker Engine active — version $dockerVer (native docker-ce, NOT Docker Desktop)"
} else {
    Write-FAIL "Docker Engine failed to start. Run: wsl -u root -e systemctl status docker"
}

Write-Header "STEP 3 — Verify Kubernetes Node Ready"
$nodeReady = wsl -u root -d $WSL_DISTRO -e bash -c "KUBECONFIG=/etc/kubernetes/admin.conf kubectl get nodes --no-headers 2>/dev/null | grep -c Ready"
if ($nodeReady -ge 1) {
    $nodeInfo = wsl -u root -d $WSL_DISTRO -e bash -c "KUBECONFIG=/etc/kubernetes/admin.conf kubectl get nodes --no-headers 2>/dev/null"
    Write-OK "Kubernetes node Ready: $nodeInfo"
} else {
    Write-WARN "K8s node not Ready. Attempting containerd fix..."
    wsl -u root -d $WSL_DISTRO -e bash -c "
        systemctl restart containerd
        sleep 5
        systemctl restart kubelet
        sleep 15
    "
    $nodeReady2 = wsl -u root -d $WSL_DISTRO -e bash -c "KUBECONFIG=/etc/kubernetes/admin.conf kubectl get nodes --no-headers 2>/dev/null | grep -c Ready"
    if ($nodeReady2 -ge 1) {
        Write-OK "K8s node now Ready after restart"
    } else {
        Write-FAIL "K8s node still not Ready. Run: wsl -u root bash /mnt/c/Users/azizs/Downloads/ESPRIT-PI-4SAE4-2025-2026-Ybrainy/DEVOPS/fix-containerd-cgroup.sh"
    }
}

Write-Header "STEP 4 — Check All K8s Pods in ybrainy namespace"
$pods = wsl -u root -d $WSL_DISTRO -e bash -c "KUBECONFIG=/etc/kubernetes/admin.conf kubectl get pods -n ybrainy --no-headers 2>/dev/null"
$crashingPods = $pods | Where-Object { $_ -match "CrashLoopBackOff|Error|OOMKilled|Pending|Init:" }
if ($crashingPods) {
    Write-WARN "Some pods are not Running:"
    $crashingPods | ForEach-Object { Write-Host "    $_" -ForegroundColor Yellow }
} else {
    Write-OK "All ybrainy pods Running"
    $pods | ForEach-Object { Write-Host "    $_" }
}

Write-Header "STEP 5 — Check Monitoring Stack"
$monPods = wsl -u root -d $WSL_DISTRO -e bash -c "KUBECONFIG=/etc/kubernetes/admin.conf kubectl get pods -n monitoring --no-headers 2>/dev/null"
$monPods | ForEach-Object {
    if ($_ -match "Running") { Write-OK $_ } else { Write-WARN $_ }
}

Write-Header "STEP 6 — Verify Jenkins"
$jenkinsRunning = wsl -u root -d $WSL_DISTRO -e bash -c "systemctl is-active jenkins 2>/dev/null"
if ($jenkinsRunning -ne "active") {
    Write-WARN "Jenkins not running — starting..."
    wsl -u root -d $WSL_DISTRO -e bash -c "systemctl start jenkins"
    Write-Host "  Waiting 30s for Jenkins to start..."
    Start-Sleep -Seconds 30
}
$jenkinsPing = Invoke-WebRequest -Uri "http://localhost:8086/login" -UseBasicParsing -TimeoutSec 10 -ErrorAction SilentlyContinue
if ($jenkinsPing.StatusCode -eq 200) {
    Write-OK "Jenkins is up: http://localhost:8086 (admin/admin)"
} else {
    Write-FAIL "Jenkins not responding on port 8086"
}

Write-Header "STEP 7 — Reload Prometheus Config (hot-reload)"
try {
    Invoke-WebRequest -Uri "http://${WSL_IP}:30090/-/reload" -Method POST -UseBasicParsing -TimeoutSec 10 -ErrorAction SilentlyContinue | Out-Null
    Write-OK "Prometheus config reloaded"
} catch { Write-WARN "Prometheus reload skipped (might not be up yet)" }

Write-Header "STEP 8 — Service Health Summary"
$wslIp = $WSL_IP
$results = @()
foreach ($svc in $SERVICES) {
    try {
        $ok = & $svc.check
        if ($ok) {
            $results += [PSCustomObject]@{ Service=$svc.name; Status="UP"; URL=$svc.url }
        } else {
            $results += [PSCustomObject]@{ Service=$svc.name; Status="DOWN"; URL=$svc.url }
        }
    } catch {
        $results += [PSCustomObject]@{ Service=$svc.name; Status="DOWN"; URL=$svc.url }
    }
}

Write-Host ""
Write-Host "  Service Health Table:" -ForegroundColor Cyan
Write-Host "  $('-'*70)"
Write-Host "  {0,-18} {1,-10} {2}" -f "SERVICE", "STATUS", "URL"
Write-Host "  $('-'*70)"
foreach ($r in $results) {
    $color = if ($r.Status -eq "UP") { "Green" } else { "Red" }
    Write-Host ("  {0,-18} {1,-10} {2}" -f $r.Service, $r.Status, $r.URL) -ForegroundColor $color
}
Write-Host "  $('-'*70)"

$upCount = ($results | Where-Object { $_.Status -eq "UP" }).Count
$total   = $results.Count
Write-Host ""
if ($upCount -eq $total) {
    Write-Host "  ALL $total SERVICES UP — Platform is ready for demo!" -ForegroundColor Green
} else {
    Write-Host "  $upCount/$total services UP. Check failed services above." -ForegroundColor Yellow
}
Write-Host ""
Write-Host "  Quick access:" -ForegroundColor Cyan
Write-Host "    Jenkins:    http://localhost:8086         (admin/admin)"
Write-Host "    SonarQube:  http://${WSL_IP}:30900       (admin/admin)"
Write-Host "    Grafana:    http://${WSL_IP}:30300        (admin/ybrainy2026)"
Write-Host "    Prometheus: http://${WSL_IP}:30090"
Write-Host "    Frontend:   http://${WSL_IP}:30080"
Write-Host "    ML Service: http://${WSL_IP}:30086/health"
Write-Host ""
