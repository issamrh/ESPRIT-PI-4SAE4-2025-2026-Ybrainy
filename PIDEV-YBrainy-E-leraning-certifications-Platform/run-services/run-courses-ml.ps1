param([int]$Port = 5000, [switch]$SkipWait, [switch]$DryRun)

& "$PSScriptRoot\_run-service.ps1" `
    -Name "Courses ML Service" `
    -ProjectPath "courses\ML-Service" `
    -Kind Python `
    -Port $Port `
    -WaitUrl "http://localhost:$Port/health" `
    -PythonScript "app.py" `
    -SkipWait:$SkipWait `
    -DryRun:$DryRun
