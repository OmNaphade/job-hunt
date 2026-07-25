param(
    [string]$DbHost = 'localhost',
    [int]$DbPort = 5432,
    [string]$DbName = 'jobapp_db',
    [string]$DbUser = 'postgres'
)

$ErrorActionPreference = 'Stop'

if (-not (Get-Command psql -ErrorAction SilentlyContinue)) {
    throw 'psql is required to load seed data. Install PostgreSQL client tools.'
}

$sqlFile = Join-Path $PSScriptRoot 'seed-demo-data.sql'

Write-Host "Applying demo data to $DbHost:$DbPort/$DbName ..." -ForegroundColor Cyan
psql -h $DbHost -p $DbPort -U $DbUser -d $DbName -f $sqlFile

Write-Host 'Seed data applied.' -ForegroundColor Green
