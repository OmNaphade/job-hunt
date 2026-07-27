# Deployment Scripts Guide

## Overview

Two deployment scripts are provided:
- `deploy-staging.sh` - Deploy to staging environment
- `deploy-production.sh` - Deploy to production environment

Both scripts are **Bash scripts designed for GitHub Actions** (Linux runners). They handle SSH-based deployment to remote servers.

---

## Environment Variables Required

Both scripts require these environment variables:

```
DEPLOY_KEY       → SSH private key (for authentication)
DEPLOY_HOST      → Server hostname/IP (e.g., staging.job-portal.com)
DEPLOY_USER      → SSH username (e.g., deploy)
DOCKER_USERNAME  → Docker registry username (e.g., github.actor)
DOCKER_PASSWORD  → Docker registry password (e.g., GitHub token)
```

---

## What Each Script Does

### deploy-staging.sh
1. ✅ Validates required environment variables
2. ✅ Sets up SSH key authentication
3. ✅ Connects to staging server via SSH
4. ✅ Pulls latest Docker images from registry
5. ✅ Restarts services with `docker compose up -d`
6. ✅ Waits for services to be healthy (health checks)
7. ✅ Confirms deployment successful

**Location:** `scripts/deploy-staging.sh`

### deploy-production.sh
1. ✅ Validates required environment variables
2. ✅ Sets up SSH key authentication
3. ✅ Connects to production server via SSH
4. ✅ Pulls latest Docker images from registry
5. ✅ Performs zero-downtime update with `docker compose up -d --no-deps`
6. ✅ Waits for all services to be healthy
7. ✅ Runs smoke tests to verify deployment
8. ✅ Confirms deployment successful

**Location:** `scripts/deploy-production.sh`

---

## How It Works in CI/CD

### Deployment Flow
```
GitHub Actions (Ubuntu Linux runner)
    ↓
Pulls code + artifacts
    ↓
Runs deploy script (bash shell)
    ↓
Sets up SSH key from secrets
    ↓
SSHes to remote server
    ↓
Executes deployment commands
    ↓
Monitors health checks
    ↓
Reports success/failure
```

---

## Testing Locally (Bash/Linux Only)

If you have Linux/WSL/Bash available:

```bash
# Set environment variables
export DEPLOY_KEY="$(cat ~/.ssh/your_key)"
export DEPLOY_HOST="staging.job-portal.com"
export DEPLOY_USER="deploy"
export DOCKER_USERNAME="your-github-username"
export DOCKER_PASSWORD="your-github-token"

# Run script
./scripts/deploy-staging.sh
```

### On Windows?
- Use WSL2 (Windows Subsystem for Linux)
- Or use Git Bash: `bash ./scripts/deploy-staging.sh`
- Or wait for GitHub Actions to test it (it runs on Linux)

---

## Server Requirements

Your staging and production servers must have:

1. **SSH Access**
   - SSH server running
   - Public key in `~/.ssh/authorized_keys`
   - User with deployment permissions

2. **Docker**
   ```bash
   docker --version
   docker-compose --version
   ```

3. **Directory Structure**
   ```
   /app/job-portal/
   ├── docker-compose.yml
   ├── .env
   └── services/...
   ```

4. **Docker Registry Access**
   ```bash
   docker login -u username -p password ghcr.io
   ```

---

## Setup Server for Deployment

### Example: Ubuntu Server

```bash
# 1. Create deploy user
sudo useradd -m -s /bin/bash deploy

# 2. Setup SSH key
sudo mkdir -p /home/deploy/.ssh
sudo chmod 700 /home/deploy/.ssh
# Copy your public key into authorized_keys
echo "ssh-rsa AAAA... your-key" | sudo tee /home/deploy/.ssh/authorized_keys

# 3. Allow deploy user to run Docker
sudo usermod -aG docker deploy

# 4. Create deployment directory
sudo mkdir -p /app/job-portal
sudo chown -R deploy:deploy /app/job-portal

# 5. Copy docker-compose.yml and .env
sudo -u deploy cp docker-compose.yml /app/job-portal/
sudo -u deploy cp .env /app/job-portal/

# 6. Test Docker Compose
sudo -u deploy docker-compose -f /app/job-portal/docker-compose.yml pull
```

---

## Generate SSH Keys

```bash
# Generate new SSH key pair
ssh-keygen -t rsa -b 4096 -f deployment_key -N ""

# Output:
# - deployment_key (private key)
# - deployment_key.pub (public key)

# Copy public key to server
ssh-copy-id -i deployment_key.pub deploy@your-server.com

# Get private key content for GitHub secret
cat deployment_key
# Copy entire output → GitHub secret as STAGING_DEPLOY_KEY or PROD_DEPLOY_KEY
```

---

## Troubleshooting

### Script fails: "Host key verification failed"
**Cause:** Server not in known_hosts
**Fix:** The script includes `ssh-keyscan` to handle this automatically

### Script fails: "Permission denied (publickey)"
**Cause:** SSH key not authorized on server
**Fix:** 
```bash
# On your server:
cat ~/.ssh/authorized_keys  # Should contain your public key
ssh -i your_key deploy@server "echo OK"  # Test SSH
```

### Script fails: "docker: not found"
**Cause:** Docker not installed on server
**Fix:** Install Docker on your deployment server

### Script fails: "docker-compose.yml: No such file"
**Cause:** Directory structure mismatch
**Fix:** Ensure `/app/job-portal/docker-compose.yml` exists on server

### Script times out waiting for health check
**Cause:** Services taking too long to start
**Fix:** Increase timeout in script (change `{1..60}` to `{1..120}`)

---

## Alternative Deployment Methods

If SSH deployment doesn't work for you, consider:

### 1. **Kubernetes Deployment**
Replace deployment script with:
```bash
kubectl set image deployment/job-portal \
  api=ghcr.io/omnaphade/job-portal-api:$SHA \
  --namespace=production

kubectl rollout status deployment/job-portal -n production
```

### 2. **AWS CodeDeploy**
```bash
aws deploy create-deployment \
  --application-name job-portal \
  --deployment-group-name production \
  --s3-location s3://bucket/artifacts.zip
```

### 3. **Terraform + Cloud Providers**
```bash
terraform apply -var="image_tag=${{ github.sha }}"
```

### 4. **GitHub Deployments API**
```bash
curl -X POST \
  -H "Authorization: token $GITHUB_TOKEN" \
  https://api.github.com/repos/user/repo/deployments \
  -d '{"ref":"main","environment":"production"}'
```

---

## Environment Secrets Setup

### GitHub Actions Secrets
Go to: **Settings → Secrets and variables → Actions**

Add:
```
STAGING_DEPLOY_KEY    → SSH private key
STAGING_HOST          → Server hostname
STAGING_USER          → SSH username

PROD_DEPLOY_KEY       → SSH private key
PROD_HOST             → Server hostname
PROD_USER             → SSH username
```

These are automatically available as environment variables in the workflow.

---

## Monitoring Deployments

### View deployment logs
```bash
# GitHub Actions
https://github.com/OmNaphade/job-portal/actions
# Find the workflow run → Click deploy-production job

# SSH logs on server
ssh deploy@prod-server 'docker logs api-gateway -f'
```

### Verify deployment
```bash
# Health check
curl https://job-portal.com/actuator/health

# Check running services
docker ps
docker compose ps
```

---

## Summary

| Aspect | Details |
|--------|---------|
| **Type** | Bash shell scripts |
| **Runner** | GitHub Actions Linux (ubuntu-latest) |
| **Auth** | SSH with key-based authentication |
| **Trigger** | Push to main (prod) or develop (staging) |
| **Duration** | 2-5 minutes per deployment |
| **Rollback** | Manual: `docker compose down && docker compose up -d` |

---

**Next Steps:**
1. ✅ Set up production/staging servers
2. ✅ Generate SSH keys
3. ✅ Add GitHub secrets
4. ✅ Test SSH connection: `ssh -i key deploy@host 'echo OK'`
5. ✅ Test deployment script on server
6. ✅ Push code to trigger GitHub Actions

Done! 🚀
