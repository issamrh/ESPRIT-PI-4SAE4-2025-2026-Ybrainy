param([int]$Port = 8097, [switch]$SkipWait, [switch]$DryRun)

& "$PSScriptRoot\_run-service.ps1" `
    -Name "Parteneriat API Gateway" `
    -ProjectPath "Parteneriat\backend\api-gateway" `
    -Kind Maven `
    -Port $Port `
    -WaitUrl "http://localhost:$Port/actuator/health" `
    -MavenRepoGroup "parteneriat" `
    -EnvVars @{ SERVER_PORT = "$Port"; PARTNERSHIP_GATEWAY_PORT = "$Port" } `
    -SkipWait:$SkipWait `
    -DryRun:$DryRun
