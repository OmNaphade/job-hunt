# Production deployment script (PowerShell version)
# Can run locally on Windows or in GitHub Actions
# Usage: ./scripts/deploy-production.ps1

param(
    [string]$DeployKey = $env:DEPLOY_KEY,
    [string]$DeployHost = $env:DEPLOY_HOST,
    [string]$DeployUser = $env:DEPLOY_USER
)

$ErrorActionPreference = 'Stop'

Write-Host "🚀 Starting production deployment..." -ForegroundColor Cyan

# Validate environment variables
if (-not $DeployKey -or -not $DeployHost -or -not $DeployUser) {
    Write-Host "❌ Missing required environment variables:" -ForegroundColor Red
    Write-Host "   DEPLOY_KEY: $(if($DeployKey) {'✓'} else {'✗'})" 
    Write-Host "   DEPLOY_HOST: $(if($DeployHost) {'✓'} else {'✗'})"
    Write-Host "   DEPLOY_USER: $(if($DeployUser) {'✓'} else {'✗'})"
    exit 1
}

# Setup SSH key
Write-Host "🔐 Setting up SSH key..." -ForegroundColor Cyan
$sshDir = "$env:USERPROFILE\.ssh"
if (-not (Test-Path $sshDir)) { New-Item -ItemType Directory -Path $sshDir -Force | Out-Null }

$keyFile = "$sshDir\deploy_key"
$DeployKey | Out-File -FilePath $keyFile -NoNewline -Encoding ASCII

# Run deployment via SSH
Write-Host "📦 Deploying to $DeployHost..." -ForegroundColor Cyan

$deployScript = @"
set -e

echo "📦 Pulling latest images..."
cd /app/job-portal

# Pull latest images from registry
docker login -u `$DOCKER_USERNAME -p `$DOCKER_PASSWORD ghcr.io
docker compose --env-file env/prod.env pull

echo "🔄 Rolling update services..."
docker compose --env-file env/prod.env up -d --no-deps

echo "⏳ Waiting for services to be ready..."
for i in {1..60}; do
  if curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✅ Production is healthy"
    break
  fi
  echo "⏳ Waiting... (\$i/60)"
  sleep 5
done

echo "✅ Production deployment complete!"
"@

# Use SSH to execute deployment
ssh -i $keyFile -o StrictHostKeyChecking=no "$DeployUser@$DeployHost" $deployScript

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Deployment successful!" -ForegroundColor Green
} else {
    Write-Host "❌ Deployment failed with exit code $LASTEXITCODE" -ForegroundColor Red
    exit 1
}
