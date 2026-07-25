# Job Portal Platform

Last updated: 2026-07-25

This repository contains a full-stack job portal built on Spring Boot microservices and a React + Vite frontend.

## What Is Included

- Frontend: React, Vite, Tailwind (`frontend`)
- Backend services:
  - `api_gateway` (8080)
  - `auth_service` (8081)
  - `user_service` (8082)
  - `job_service` (8083)
  - `company_service` (8084)
  - `application_service` (8085)
  - `notification_service` (8086)
- Infrastructure services:
  - `service_registry` (8761)
  - `config_server` (8888)
- Platform dependencies:
  - PostgreSQL
  - Kafka + Zookeeper

## Quick Start (Docker)

From repository root:

```powershell
docker compose up -d --build
```

Check status:

```powershell
docker compose ps
```

## Local Validation Scripts

Run from repository root:

```powershell
./scripts/synthetic-checks.ps1
./scripts/journey-checks.ps1
```

Notes:

- Synthetic checks validate gateway and auth guards with secured endpoint expectations.
- Journey checks validate candidate and employer flows and verify monitoring authorization behavior.

## Deterministic Admin Seed (Docker)

In Docker Compose runs, auth_service seeds an admin account on startup when enabled:

- Email: admin@jobportal.local
- Password: Pass123!

Seed controls (auth_service environment):

- ADMIN_SEED_ENABLED (default in compose: true)
- ADMIN_SEED_EMAIL
- ADMIN_SEED_PASSWORD

## Frontend Run

```powershell
Set-Location frontend
npm install
npm run dev
```

## GitHub Actions

Workflow file:

- `.github/workflows/ci.yml`

Pipeline coverage:

- Frontend build/test/lint and Playwright smoke
- Backend matrix build/test for all business services
- Infra service build (registry/config/gateway)
- Dependency security baseline checks (PR)
- Docker Compose integration smoke
- Scripted synthetic and journey checks
- Artifact upload (jars, reports, frontend dist, integration logs)

## Core Documentation

- API reference and architecture: `API_DOCS.md`
- Backend functionality and implementation notes: `BACKEND_README.md`
- Frontend functionality and endpoint mapping: `frontend/README.md`
- Startup and local ops scripts: `STARTUP.md`
- TODO and delivery status: `TODO.md`
