param(
    [switch]$SkipDocker,
    [switch]$SkipFrontendInstall,
    [switch]$StartFrontendDev
)

$ErrorActionPreference = 'Stop'

Write-Host 'Validating local environment...' -ForegroundColor Cyan
& "$PSScriptRoot\validate-env.ps1"

if (-not $SkipDocker) {
    Write-Host 'Starting infrastructure and services via docker-compose...' -ForegroundColor Cyan
    Push-Location "$PSScriptRoot\.."
    try {
        docker compose up -d
    }
    finally {
        Pop-Location
    }
}

Push-Location "$PSScriptRoot\..\frontend"
try {
    if (-not $SkipFrontendInstall) {
        Write-Host 'Installing frontend dependencies...' -ForegroundColor Cyan
        npm install
    }

    if ($StartFrontendDev) {
        Write-Host 'Starting frontend dev server...' -ForegroundColor Cyan
        npm run dev
    }
    else {
        Write-Host 'Running frontend test + build checks...' -ForegroundColor Cyan
        npm run test
        npm run build
    }
}
finally {
    Pop-Location
}

Write-Host 'Local bootstrap completed.' -ForegroundColor Green
