param([int]$Port = 8761, [switch]$SkipWait, [switch]$DryRun)

$env:YBRAINY_EUREKA_URL = "http://localhost:$Port/eureka/"

& "$PSScriptRoot\_run-service.ps1" `
    -Name "Unified Eureka" `
    -ProjectPath "forum\eureka" `
    -Kind Maven `
    -Port $Port `
    -WaitUrl "http://localhost:$Port/eureka/apps" `
    -MavenRepoGroup "unified" `
    -EnvVars @{ SERVER_PORT = "$Port" } `
    -EurekaUrl "" `
    -SkipWait:$SkipWait `
    -DryRun:$DryRun
