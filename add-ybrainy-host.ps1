$hostsPath = "C:\Windows\System32\drivers\etc\hosts"
$entry = "127.0.0.1 ybrainy"

if (Select-String -Path $hostsPath -Pattern "ybrainy" -Quiet) {
    Write-Host "ybrainy is already in hosts file." -ForegroundColor Yellow
} else {
    Add-Content -Path $hostsPath -Value "`n# YBrainy platform - use http://ybrainy:<port> instead of localhost`n$entry"
    Write-Host "Done! Added: $entry" -ForegroundColor Green
}

Write-Host ""
Write-Host "You can now use:" -ForegroundColor Cyan
Write-Host "  http://ybrainy:8088              (Gateway)"  -ForegroundColor White
Write-Host "  http://ybrainy:8088/swagger-ui.html  (Swagger)" -ForegroundColor White
Write-Host "  http://ybrainy:8761              (Eureka)"   -ForegroundColor White
Write-Host "  http://ybrainy:15672             (RabbitMQ)" -ForegroundColor White
Write-Host ""
pause
