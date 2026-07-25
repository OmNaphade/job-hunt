$ErrorActionPreference = 'Stop'

function Assert-Command {
    param(
        [Parameter(Mandatory = $true)] [string]$Name,
        [Parameter(Mandatory = $true)] [string]$Help
    )

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Missing required command '$Name'. $Help"
    }
}

Write-Host 'Checking required tools...' -ForegroundColor Cyan
Assert-Command -Name 'java' -Help 'Install Java 21 or newer.'
Assert-Command -Name 'node' -Help 'Install Node.js 22 LTS.'
Assert-Command -Name 'npm' -Help 'Install npm (bundled with Node.js).'
Assert-Command -Name 'docker' -Help 'Install Docker Desktop and ensure daemon is running.'

try {
    docker info | Out-Null
}
catch {
    throw 'Docker daemon is not available. Start Docker Desktop and retry.'
}

Write-Host 'Validating key environment variables...' -ForegroundColor Cyan
if (-not $env:JWT_SECRET) {
    Write-Warning 'JWT_SECRET is not set; development default may be used by services.'
}

if (-not $env:VITE_API_BASE_URL) {
    Write-Warning 'VITE_API_BASE_URL is not set; frontend will use default gateway URL.'
}

Write-Host 'Environment validation passed.' -ForegroundColor Green
