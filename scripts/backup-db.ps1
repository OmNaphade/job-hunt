param(
    [string]$DbHost = 'localhost',
    [int]$DbPort = 5432,
    [string]$DbName = 'jobapp_db',
    [string]$DbUser = 'postgres',
    [string]$OutputDir = '.\\backups'
)

$ErrorActionPreference = 'Stop'

if (-not (Get-Command pg_dump -ErrorAction SilentlyContinue)) {
    throw 'pg_dump not found. Install PostgreSQL client tools.'
}

if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir | Out-Null
}

$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$outFile = Join-Path $OutputDir "$DbName-$timestamp.dump"

Write-Host "Creating backup: $outFile" -ForegroundColor Cyan
pg_dump -h $DbHost -p $DbPort -U $DbUser -F c -b -v -f $outFile $DbName
Write-Host 'Backup completed.' -ForegroundColor Green
