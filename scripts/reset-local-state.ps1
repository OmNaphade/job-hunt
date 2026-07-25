param(
    [switch]$Force
)

$ErrorActionPreference = 'Stop'

if (-not $Force) {
    $answer = Read-Host 'This will recreate local docker volumes (data loss). Type YES to continue'
    if ($answer -ne 'YES') {
        Write-Host 'Operation cancelled.' -ForegroundColor Yellow
        exit 0
    }
}

Push-Location "$PSScriptRoot\.."
try {
    Write-Host 'Stopping and removing containers/volumes...' -ForegroundColor Cyan
    docker compose down -v

    Write-Host 'Starting fresh environment...' -ForegroundColor Cyan
    docker compose up -d
}
finally {
    Pop-Location
}

Write-Host 'Local state reset completed.' -ForegroundColor Green
