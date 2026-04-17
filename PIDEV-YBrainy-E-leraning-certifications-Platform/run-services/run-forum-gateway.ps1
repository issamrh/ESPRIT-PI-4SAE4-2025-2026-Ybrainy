param([int]$Port = 8090, [switch]$SkipWait, [switch]$DryRun)

& "$PSScriptRoot\_run-service.ps1" `
    -Name "Forum API Gateway" `
    -ProjectPath "forum\api-gateway" `
    -Kind Maven `
    -Port $Port `
    -WaitUrl "http://localhost:$Port/actuator/health" `
    -MavenRepoGroup "forum" `
    -EnvVars @{ SERVER_PORT = "$Port"; FORUM_GATEWAY_PORT = "$Port" } `
    -SkipWait:$SkipWait `
    -DryRun:$DryRun
