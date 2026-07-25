$ErrorActionPreference = 'Stop'

$endpoints = @(
    'http://localhost:8080/actuator/health',
    'http://localhost:8081/actuator/health',
    'http://localhost:8082/actuator/health',
    'http://localhost:8083/actuator/health',
    'http://localhost:8084/actuator/health',
    'http://localhost:8085/actuator/health',
    'http://localhost:8086/actuator/health'
)

Write-Host 'Local Health Dashboard' -ForegroundColor Cyan
Write-Host '----------------------' -ForegroundColor Cyan

foreach ($url in $endpoints) {
    try {
        $res = Invoke-RestMethod -Uri $url -Method Get -TimeoutSec 5
        Write-Host ("[UP]   {0} -> {1}" -f $url, $res.status) -ForegroundColor Green
    }
    catch {
        Write-Host ("[DOWN] {0} -> {1}" -f $url, $_.Exception.Message) -ForegroundColor Red
    }
}

Write-Host ''
Write-Host 'Useful log commands:' -ForegroundColor Yellow
Write-Host '  docker compose logs -f api-gateway'
Write-Host '  docker compose logs -f auth-service user-service job-service company-service application-service notification-service'
