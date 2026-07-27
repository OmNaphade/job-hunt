#!/bin/bash
# Staging deployment script
# Called by: GitHub Actions CI/CD pipeline
# Purpose: Deploy latest staging build to staging environment
#
# NOTE: Staging server is NOT set up yet, so the actual deployment logic
#       below is COMMENTED OUT. This script currently just prints a placeholder
#       message and succeeds, so the pipeline stays green.
#
#       When your staging server is ready:
#         1. Add GitHub secrets: STAGING_DEPLOY_KEY, STAGING_HOST, STAGING_USER
#         2. Uncomment the "REAL DEPLOYMENT" block below
#         3. Adjust paths/commands to match your infrastructure
#       See DEPLOYMENT_SCRIPTS.md for full setup instructions.

set -e

echo "🚀 Staging deploy step reached."
echo "ℹ️  Staging server not configured yet — skipping real deployment."
echo "   Enable it by adding secrets and uncommenting the block in scripts/deploy-staging.sh"

# ---------------------------------------------------------------------------
# REAL DEPLOYMENT (uncomment when staging server is ready)
# ---------------------------------------------------------------------------
# # Fail early if secrets are missing
# if [ -z "$DEPLOY_KEY" ] || [ -z "$DEPLOY_HOST" ] || [ -z "$DEPLOY_USER" ]; then
#   echo "❌ Missing deployment secrets (STAGING_DEPLOY_KEY / STAGING_HOST / STAGING_USER)."
#   exit 1
# fi
#
# # Setup SSH key
# mkdir -p ~/.ssh
# echo "$DEPLOY_KEY" > ~/.ssh/deploy_key
# chmod 600 ~/.ssh/deploy_key
# ssh-keyscan -H "$DEPLOY_HOST" >> ~/.ssh/known_hosts 2>/dev/null
#
# # Connect to staging server and deploy
# ssh -i ~/.ssh/deploy_key -o StrictHostKeyChecking=no \
#     "$DEPLOY_USER@$DEPLOY_HOST" << 'EOF'
#
#   set -e
#
#   echo "📦 Pulling latest images..."
#   cd /app/job-portal
#
#   # Pull latest images from registry
#   docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD ghcr.io
#   docker compose pull
#
#   echo "🔄 Restarting services..."
#   docker compose up -d
#
#   echo "⏳ Waiting for services to be ready..."
#   for i in {1..60}; do
#     if docker compose exec -T api-gateway curl -f http://localhost:8080/actuator/health > /dev/null 2>&1; then
#       echo "✅ Gateway is healthy"
#       break
#     fi
#     echo "⏳ Waiting... ($i/60)"
#     sleep 5
#   done
#
#   echo "✅ Staging deployment complete!"
#
# EOF
#
# echo "✅ Deployment successful!"
# ---------------------------------------------------------------------------

exit 0
