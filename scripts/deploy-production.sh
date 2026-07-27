#!/bin/bash
# Production deployment script
# Called by: GitHub Actions CI/CD pipeline
# Purpose: Deploy latest production build to production environment with zero-downtime

set -e

echo "🚀 Starting production deployment..."

# Skip gracefully if deployment secrets are not configured
if [ -z "$DEPLOY_KEY" ] || [ -z "$DEPLOY_HOST" ] || [ -z "$DEPLOY_USER" ]; then
  echo "⚠️  Deployment secrets not configured — skipping deployment."
  echo "   To enable deployment, add these GitHub secrets:"
  echo "     - PROD_DEPLOY_KEY"
  echo "     - PROD_HOST"
  echo "     - PROD_USER"
  echo "   See DEPLOYMENT_SCRIPTS.md for setup instructions."
  exit 0
fi

# Setup SSH key
mkdir -p ~/.ssh
echo "$DEPLOY_KEY" > ~/.ssh/deploy_key
chmod 600 ~/.ssh/deploy_key
ssh-keyscan -H "$DEPLOY_HOST" >> ~/.ssh/known_hosts 2>/dev/null

# Connect to production server and deploy
ssh -i ~/.ssh/deploy_key -o StrictHostKeyChecking=no \
    "$DEPLOY_USER@$DEPLOY_HOST" << 'EOF'

  set -e
  
  echo "📦 Pulling latest images..."
  cd /app/job-portal
  
  # Pull latest images from registry
  docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD ghcr.io
  docker compose pull
  
  echo "🔄 Rolling update services..."
  # Use health checks for graceful restart
  docker compose up -d --no-deps
  
  echo "⏳ Waiting for services to be ready..."
  # Wait for all services to be healthy
  for service in api-gateway auth-service user-service job-service company-service application-service notification-service; do
    echo "  Checking $service..."
    for i in {1..60}; do
      if docker compose exec -T $service curl -f http://localhost:8080/actuator/health > /dev/null 2>&1 2>&1; then
        echo "  ✅ $service is healthy"
        break
      fi
      echo "  ⏳ Waiting... ($i/60)"
      sleep 5
    done
  done
  
  echo "📊 Running smoke tests..."
  # Verify critical endpoints
  curl -f http://localhost:8080/actuator/health || exit 1
  curl -f http://localhost:8080/api/health || exit 1
  
  echo "✅ Production deployment complete!"

EOF

echo "✅ Deployment successful!"
