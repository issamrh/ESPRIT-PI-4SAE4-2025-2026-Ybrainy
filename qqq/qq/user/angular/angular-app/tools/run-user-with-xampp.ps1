param(
  # Root of the mono-repo.
  [string]$RepoRoot = "D:\taw\qqq\qq",

  # XAMPP install folder (contains mysql\bin\mysqladmin.exe and mysql\data\mysql_error.log).
  [string]$XamppRoot = "C:\xampp",

  # MySQL connection used by the user-service (defaults match user-service application.properties).
  [string]$MySqlHost = "127.0.0.1",
  [int]$MySqlPort = 3306,
  [string]$MySqlUser = "root",
  [string]$MySqlPassword = "",

  # Service ports.
  [int]$EurekaPort = 8761,
  [int]$GatewayPort = 8088,
  [int]$UserServicePort = 8899,
  [int]$FrontendPort = 4200,

  # Optional toggles.
  [switch]$SkipFrontend,
  [switch]$SkipGateway,
  [switch]$SkipEureka,
  [switch]$SkipUserService,

  # If RabbitMQ is not running locally, disable listeners so startup doesn't block on it.
  [switch]$DisableRabbitMq = $true,

  # Timeouts.
  [int]$MySqlReadyTimeoutSec = 120,
  [int]$SleepBetweenChecksSec = 2,

  # Debug helpers.
  [switch]$TailMySqlLogOnFailure = $true,
  [switch]$SkipMySqlCheck,

  # Pass through to the underlying run-services scripts.
  [switch]$SkipWait,
  [switch]$DryRun
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Resolve-Folder([string]$Path) {
  if (-not (Test-Path -LiteralPath $Path -PathType Container)) {
    throw "Folder not found: $Path"
  }
  return (Resolve-Path -LiteralPath $Path).Path
}

function Resolve-File([string]$Path) {
  if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
    throw "File not found: $Path"
  }
  return (Resolve-Path -LiteralPath $Path).Path
}

function Get-MySqlAdminPath([string]$XamppRootResolved) {
  $candidate = Join-Path $XamppRootResolved "mysql\bin\mysqladmin.exe"
  if (Test-Path -LiteralPath $candidate -PathType Leaf) {
    return (Resolve-Path -LiteralPath $candidate).Path
  }

  $cmd = Get-Command mysqladmin.exe -ErrorAction SilentlyContinue
  if ($cmd) {
    return $cmd.Source
  }

  throw "mysqladmin.exe not found. Set -XamppRoot correctly or add it to PATH."
}

function Invoke-MySqlPing([string]$MySqlAdmin, [string]$HostName, [int]$Port, [string]$User, [string]$Password) {
  $args = @("--host=$HostName", "--port=$Port", "--user=$User", "--connect-timeout=2", "ping")
  if ($Password -ne $null -and $Password -ne "") {
    # mysqladmin expects --password=... (no space) to avoid interactive prompt.
    $args = @("--password=$Password") + $args
  }

  $output = & $MySqlAdmin @args 2>&1
  if ($LASTEXITCODE -eq 0) { return @{ ok = $true; output = ($output | Out-String).Trim() } }

  # Some XAMPP builds emit a bell char; return false but keep output for debugging when needed.
  return @{ ok = $false; output = ($output | Out-String).Trim() }
}

function Wait-ForMySqlReady([string]$MySqlAdmin, [int]$TimeoutSec) {
  $deadline = (Get-Date).AddSeconds($TimeoutSec)
  while ((Get-Date) -lt $deadline) {
    $ping = Invoke-MySqlPing -MySqlAdmin $MySqlAdmin -HostName $MySqlHost -Port $MySqlPort -User $MySqlUser -Password $MySqlPassword
    if ($ping.ok) {
      Write-Host "[mysql] Ready on $($MySqlHost):$MySqlPort"
      return
    }
    Start-Sleep -Seconds $SleepBetweenChecksSec
  }

  $final = Invoke-MySqlPing -MySqlAdmin $MySqlAdmin -HostName $MySqlHost -Port $MySqlPort -User $MySqlUser -Password $MySqlPassword
  $details = if ($final.output) { $final.output } else { "mysqladmin ping failed (no output)" }
  throw "[mysql] Not ready after ${TimeoutSec}s. Last ping result: $details"
}

$repoRootResolved = Resolve-Folder $RepoRoot
$xamppRootResolved = Resolve-Folder $XamppRoot
$mysqlAdmin = Get-MySqlAdminPath $xamppRootResolved
$mysqlErrorLog = Join-Path $xamppRootResolved "mysql\data\mysql_error.log"

Write-Host "[setup] RepoRoot: $repoRootResolved"
Write-Host "[setup] XamppRoot: $xamppRootResolved"
Write-Host "[setup] mysqladmin: $mysqlAdmin"

try {
  if (-not $SkipMySqlCheck) {
    Write-Host "[mysql] Waiting for MySQL handshake/ping..."
    Wait-ForMySqlReady -MySqlAdmin $mysqlAdmin -TimeoutSec $MySqlReadyTimeoutSec
  } else {
    Write-Host "[mysql] Skipping MySQL readiness check (-SkipMySqlCheck)."
  }
} catch {
  Write-Warning $_.Exception.Message
  if ($TailMySqlLogOnFailure -and (Test-Path -LiteralPath $mysqlErrorLog -PathType Leaf)) {
    Write-Host "[mysql] Last 80 lines of $mysqlErrorLog"
    Get-Content -LiteralPath $mysqlErrorLog -Tail 80
  } else {
    Write-Host "[mysql] Tip: ensure XAMPP MySQL is started and no other MySQL/MariaDB instance is locking InnoDB files."
  }

  Write-Host "[mysql] Quick check commands:"
  Write-Host "  - $mysqlAdmin --host=$MySqlHost --port=$MySqlPort --user=$MySqlUser ping"
  if ($MySqlPassword) {
    Write-Host "  - (password is set via -MySqlPassword)"
  }
  exit 1
}

# Set env vars used by Spring services.
$env:YBRAINY_EUREKA_URL = "http://localhost:$EurekaPort/eureka/"
if ($DisableRabbitMq) {
  $env:YBRAINY_RABBIT_LISTENER_ENABLED = "false"
}

$runServices = Join-Path $repoRootResolved "run-services"
$runEureka = Resolve-File (Join-Path $runServices "run-eureka.ps1")
$runUserService = Resolve-File (Join-Path $runServices "run-user-service.ps1")
$runGateway = Resolve-File (Join-Path $runServices "run-user-gateway.ps1")
$runFrontend = Resolve-File (Join-Path $runServices "run-user-angular.ps1")

$commonArgs = @()
if ($SkipWait) { $commonArgs += "-SkipWait" }
if ($DryRun) { $commonArgs += "-DryRun" }

if (-not $SkipEureka) {
  Write-Host "[run] Eureka on :$EurekaPort"
  & $runEureka -Port $EurekaPort @commonArgs
} else {
  Write-Host "[run] Skipping Eureka (expects it already running on :$EurekaPort)"
}

if (-not $SkipUserService) {
  Write-Host "[run] User service on :$UserServicePort"
  & $runUserService -Port $UserServicePort @commonArgs
} else {
  Write-Host "[run] Skipping user-service"
}

if (-not $SkipGateway) {
  Write-Host "[run] API gateway on :$GatewayPort"
  & $runGateway -Port $GatewayPort @commonArgs
} else {
  Write-Host "[run] Skipping API gateway"
}

if (-not $SkipFrontend) {
  Write-Host "[run] Angular on :$FrontendPort"
  & $runFrontend -Port $FrontendPort @commonArgs
} else {
  Write-Host "[run] Skipping Angular"
}
