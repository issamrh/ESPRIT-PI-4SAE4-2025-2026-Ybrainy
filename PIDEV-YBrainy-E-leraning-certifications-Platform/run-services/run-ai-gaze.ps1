param(
    [int]$Port = 5002,
    [switch]$SkipWait,
    [switch]$DryRun
)

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
$gazeRoot = (Resolve-Path -LiteralPath (Join-Path $repoRoot "..\..\Gazeassess")).Path

$command = @"
`$python = Get-Command python -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source -First 1
if (-not `$python) { `$python = Get-Command py -ErrorAction SilentlyContinue | Select-Object -ExpandProperty Source -First 1 }
if (-not `$python) { throw 'Python was not found in PATH.' }
& `$python -m pip install --disable-pip-version-check -r requirements.txt
if (`$LASTEXITCODE -ne 0) { exit `$LASTEXITCODE }
& `$python eye_tracking_service.py --port $Port
"@

& "$PSScriptRoot\_run-service.ps1" `
    -Name "AI Gaze Tracking" `
    -ProjectPath $gazeRoot `
    -Kind Command `
    -Port $Port `
    -WaitUrl "http://localhost:$Port/health" `
    -Command $command `
    -SkipWait:$SkipWait `
    -DryRun:$DryRun
