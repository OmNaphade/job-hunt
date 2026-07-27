#!/bin/bash
# Staging deployment script
# Called by: GitHub Actions CI/CD pipeline
# Purpose: Deploy latest staging build to staging environment

set -e

echo "🚀 Starting staging deployment..."

# Setup SSH key
mkdir -p ~/.ssh
echo "$DEPLOY_KEY" > ~/.ssh/deploy_key
chmod 600 ~/.ssh/deploy_key
ssh-keyscan -H "$DEPLOY_HOST" >> ~/.ssh/known_hosts 2>/dev/null

# Connect to staging server and deploy
ssh -i ~/.ssh/deploy_key -o StrictHostKeyChecking=no \
    "$DEPLOY_USER@$DEPLOY_HOST" << 'EOF'

  set -e
  
  echo "📦 Pulling latest images..."
  cd /app/job-portal
  
  # Pull latest images from registry
  docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD ghcr.io
  docker compose pull
  
  echo "🔄 Restarting services..."
  # Update and restart services
  docker compose up -d
  
  echo "⏳ Waiting for services to be ready..."
  # Wait for gateway to be healthy
  for i in {1..60}; do
    if docker compose exec -T api-gateway curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
      echo "✅ Gateway is healthy"
      break
    fi
    echo "⏳ Waiting... ($i/60)"
    sleep 5
  done
  
  echo "✅ Staging deployment complete!"

EOF

echo "✅ Deployment successful!"
