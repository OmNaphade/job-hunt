param(
    [ValidateSet('gateway','auth_service','user_service','job_service','company_service','application_service','notification_service','kafka','postgres')]
    [string]$Target,
    [ValidateSet('down','up')]
    [string]$Action = 'down'
)

$ErrorActionPreference = 'Stop'

if (-not $Target) {
    throw 'Please provide -Target with a supported service name.'
}

Push-Location "$PSScriptRoot\.."
try {
    if ($Action -eq 'down') {
        Write-Host "Stopping $Target..." -ForegroundColor Yellow
        docker compose stop $Target
    }
    else {
        Write-Host "Starting $Target..." -ForegroundColor Cyan
        docker compose start $Target
    }
}
finally {
    Pop-Location
}

Write-Host "Failure simulation action completed: $Target -> $Action" -ForegroundColor Green
