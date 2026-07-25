param(
    [Parameter(Mandatory = $true)]
    [string]$BackupFile,
    [string]$DbHost = 'localhost',
    [int]$DbPort = 5432,
    [string]$DbName = 'jobapp_db',
    [string]$DbUser = 'postgres'
)

$ErrorActionPreference = 'Stop'

if (-not (Get-Command pg_restore -ErrorAction SilentlyContinue)) {
    throw 'pg_restore not found. Install PostgreSQL client tools.'
}

if (-not (Test-Path $BackupFile)) {
    throw "Backup file not found: $BackupFile"
}

Write-Host "Restoring backup: $BackupFile" -ForegroundColor Yellow
pg_restore -h $DbHost -p $DbPort -U $DbUser -d $DbName --clean --if-exists --no-owner --no-privileges $BackupFile
Write-Host 'Restore completed.' -ForegroundColor Green
