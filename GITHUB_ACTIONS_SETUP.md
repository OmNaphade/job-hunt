# GitHub Actions CI/CD Pipeline Setup Guide

## Overview
Your CI/CD pipeline is configured and ready. This guide walks you through the remaining setup steps.

---

## Step 1: Add GitHub Secrets (Required for Deployment)

Navigate to your GitHub repository → **Settings** → **Secrets and variables** → **Actions**

### Staging Environment Secrets
Add these secrets for your staging deployment:

```
STAGING_DEPLOY_KEY     → Your SSH private key for staging server
STAGING_HOST           → Your staging server hostname/IP (e.g., staging.job-portal.com)
STAGING_USER           → SSH username for staging server (e.g., deploy)
```

**How to generate SSH key (if needed):**
```bash
ssh-keygen -t rsa -b 4096 -f staging_deploy_key -N ""
# Public key goes to: ~/.ssh/authorized_keys on staging server
# Private key content goes to: STAGING_DEPLOY_KEY secret
```

### Production Environment Secrets
Add these secrets for your production deployment:

```
PROD_DEPLOY_KEY        → Your SSH private key for production server
PROD_HOST              → Your production server hostname/IP
PROD_USER              → SSH username for production server
```

### Optional: Codecov Integration
1. Go to [codecov.io](https://codecov.io)
2. Sign in with GitHub
3. Add your repository
4. **No secret needed** — Codecov auto-detects from public repos

---

## Step 2: Update Deployment Scripts

The pipeline references deployment scripts in your `.github/workflows/ci.yml`:

### Location: `.github/workflows/ci.yml` (Lines 385-393 & 406-414)

**Current placeholder:**
```yaml
run: |
  # Add your staging deployment script here
  echo "Deploying to staging environment..."
  # Example: ssh deploy scripts, kubectl apply, etc.
```

### Update with your deployment approach:

#### Option A: SSH Deploy Script
```bash
run: |
  mkdir -p ~/.ssh
  echo "${{ secrets.STAGING_DEPLOY_KEY }}" > ~/.ssh/deploy_key
  chmod 600 ~/.ssh/deploy_key
  
  ssh -i ~/.ssh/deploy_key -o StrictHostKeyChecking=no \
    ${{ secrets.STAGING_USER }}@${{ secrets.STAGING_HOST }} << 'EOF'
    cd /app
    docker compose pull
    docker compose up -d
    docker compose exec -T api ./healthcheck.sh
  EOF
```

#### Option B: Kubernetes Deploy
```bash
run: |
  mkdir -p ~/.kube
  echo "${{ secrets.STAGING_KUBECONFIG }}" | base64 -d > ~/.kube/config
  chmod 600 ~/.kube/config
  
  kubectl set image deployment/job-portal \
    api=ghcr.io/omnaphade/job-portal-api-gateway:${{ github.sha }} \
    --namespace=staging
  
  kubectl rollout status deployment/job-portal -n staging
```

#### Option C: GitHub Pages / Static Hosting
```bash
run: |
  npm run build
  npm run deploy -- --token ${{ secrets.STAGING_DEPLOY_TOKEN }}
```

---

## Step 3: Push to GitHub

### 1. Configure Git (if not already done)
```bash
git config user.name "Your Name"
git config user.email "your.email@example.com"
```

### 2. Commit Pipeline Changes
```bash
git add .github/workflows/ci.yml
git commit -m "feat: enhanced CI/CD pipeline with Docker builds and deployments

- Added code coverage reporting (Codecov)
- Added Docker image builds for all services
- Added deployment stages (staging/production)
- Added concurrency control and job dependencies
- Optimized artifact retention

Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>"
```

### 3. Push to GitHub
```bash
git push origin main
# or for develop branch:
git push origin develop
```

---

## Step 4: Verify Pipeline Works

### Check Pipeline Status
1. Go to your GitHub repo → **Actions** tab
2. Watch the workflow run
3. Monitor build logs in real-time

### Expected Behavior

**On Pull Request (PRs to main):**
- ✅ Frontend build & test
- ✅ Backend services build & test (6 parallel jobs)
- ✅ Infrastructure services build
- ✅ Security audit & dependency review
- ✅ Contract & lint checks
- ✅ Integration smoke tests
- ❌ Docker builds (skipped - PRs only)
- ❌ Deployments (skipped - PRs only)

**On Push to develop:**
- ✅ All CI jobs above
- ✅ Docker image builds
- ✅ Deploy to staging
- ❌ Deploy to production

**On Push to main:**
- ✅ All CI jobs above
- ✅ Docker image builds
- ✅ Deploy to production
- ❌ Deploy to staging

---

## Step 5: Configure Codecov (Optional)

### Automatic Setup (Recommended)
1. Open the first PR with this pipeline
2. Codecov bot will auto-comment on the PR
3. Click the link to authorize
4. Coverage reports will appear automatically on future PRs

### Manual Setup
```bash
# Visit codecov.io, sign in with GitHub, select your repo
# No additional config needed for public repos
```

### View Coverage Reports
- Coverage appears on each PR automatically
- Branch-level coverage dashboard at `codecov.io/gh/yourname/job-portal`

---

## Step 6: Customize for Your Infrastructure

### For Docker/Container Registry
Update the registry in `.github/workflows/ci.yml` (line 14):
```yaml
env:
  REGISTRY: ghcr.io  # Change to your registry (e.g., docker.io, ecr, etc.)
  IMAGE_NAME: ${{ github.repository }}
```

### For Kubernetes Deployments
Add secrets:
- `STAGING_KUBECONFIG` (base64 encoded)
- `PROD_KUBECONFIG` (base64 encoded)

Update deployment script to use kubectl.

### For Cloud Platforms (AWS/GCP/Azure)
Add platform-specific secrets and update deployment scripts accordingly.

---

## Troubleshooting

### Pipeline Fails at "Build Docker Images"
**Issue:** Can't push to container registry
**Solution:**
- Check `GITHUB_TOKEN` has `packages: write` permission
- Verify registry credentials in secrets
- Check Dockerfile paths exist for all services

### Deployment Job Skipped
**Issue:** Deploy job not running
**Solution:**
- Verify you pushed to `main` or `develop` (not feature branch)
- Check the `if:` condition in workflow
- Verify all previous jobs passed

### Coverage Not Showing on PRs
**Issue:** Codecov not posting comments
**Solution:**
- Visit codecov.io and authorize your GitHub repo
- Wait ~5 minutes for activation
- Re-run the workflow

### Tests Timing Out
**Issue:** "Wait and smoke test" job hangs
**Solution:**
- Increase timeout in workflow (line 224): change `60` to `120`
- Check docker-compose.yml health checks
- Verify all services start correctly locally

---

## Next: Monitoring & Alerts

### GitHub Actions Notifications
1. Settings → Notifications → Actions
2. Choose: Email on failure, success, or always

### Slack Integration (Optional)
Add this step to any job:
```yaml
- name: Notify Slack
  if: always()
  uses: slackapi/slack-github-action@v1.24.0
  with:
    webhook-url: ${{ secrets.SLACK_WEBHOOK }}
    payload: |
      {
        "text": "Job Portal CI/CD: ${{ job.status }}"
      }
```

---

## Summary Checklist

- [ ] Added STAGING_DEPLOY_KEY, STAGING_HOST, STAGING_USER secrets
- [ ] Added PROD_DEPLOY_KEY, PROD_HOST, PROD_USER secrets
- [ ] Updated deployment scripts in `.github/workflows/ci.yml`
- [ ] Tested deployment script locally (optional but recommended)
- [ ] Committed pipeline changes with `git commit`
- [ ] Pushed to GitHub with `git push`
- [ ] Verified first workflow run in GitHub Actions tab
- [ ] (Optional) Authorized Codecov for coverage reports
- [ ] Set up Slack/email notifications

---

## Quick Reference: Pipeline Structure

```
┌─ PR to main/develop
│
├─ Parallel Jobs (15-20 min):
│  ├─ frontend-build-test
│  ├─ build-and-test (6 services matrix)
│  ├─ build-infra
│  ├─ security-baseline (PRs only)
│  └─ ci-contract-and-lint
│
├─ Sequentially After Above:
│  ├─ ci-integration-smoke
│  ├─ build-docker-images (push only)
│  ├─ deploy-staging (develop branch)
│  └─ deploy-production (main branch)
│
└─ Concurrency: Max 1 run per branch (cancels previous runs)
```

---

Need help? Check the workflow file: `.github/workflows/ci.yml`
