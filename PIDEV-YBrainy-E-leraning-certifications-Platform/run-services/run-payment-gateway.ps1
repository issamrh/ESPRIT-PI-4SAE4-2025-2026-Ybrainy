param([int]$Port = 8091, [switch]$SkipWait, [switch]$DryRun)

& "$PSScriptRoot\_run-service.ps1" `
    -Name "Payment API Gateway" `
    -ProjectPath "payment\api-gateway" `
    -Kind Maven `
    -Port $Port `
    -WaitUrl "http://localhost:$Port" `
    -MavenRepoGroup "payment" `
    -EnvVars @{ SERVER_PORT = "$Port" } `
    -SkipWait:$SkipWait `
    -DryRun:$DryRun
