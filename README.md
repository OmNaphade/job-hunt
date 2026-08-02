# Job Portal Platform

Production-style job portal platform with microservices backend and role-based frontend, built for hiring workflows across candidates, recruiters, and admins.

[![Build Status](https://github.com/OmNaphade/job-portal/actions/workflows/ci.yml/badge.svg)](https://github.com/OmNaphade/job-portal/actions/workflows/ci.yml)
[![Version](https://img.shields.io/badge/version-1.0.0-informational.svg)](https://github.com/OmNaphade/job-portal)
[![Runtime](https://img.shields.io/badge/java-21-orange.svg)](https://www.oracle.com/java/)

## Latest Project Details

- Services: 9 microservices (auth, user, job, company, application, notification, api_gateway, config_server, service_registry)
- Frontend: React + Vite (Node 22)
- Backend: Java 21 (Spring Boot)
- Database: PostgreSQL (used in CI as service)
- CI: GitHub Actions (tests, build, integration smoke, Docker image build) — all 7 HTTP-facing services (including `api_gateway`) run their test suites in CI, not just build
- Deployment: SSH-based scripts are present but **deployment is currently inactive** (scripts are templates). See `DEPLOYMENT_SCRIPTS.md` for activation steps.
- Code Coverage: frontend (Vitest) + backend (JaCoCo, wired via the root `pom.xml` for every module) reported to Codecov — the pipeline now actually produces and uploads `jacoco.xml` for every service; coverage % is only meaningful for `job_service`, `application_service`, and `auth_service` today, the rest still just have the Spring Boot stub test
- Observability: Prometheus metrics + Zipkin distributed tracing on every service, Grafana dashboards in `operations/grafana/`, k6 load tests in `performance/k6/`
- Security: gateway-level rate limiting (Resilience4j, per-client-IP) on top of the existing JWT + role-based `@PreAuthorize` model
- Full API reference and architecture notes: see [`API_DOCS.md`](./API_DOCS.md)

## Table of Contents

- [About](#about)
- [Latest Project Details](#latest-project-details)
- [Built With](#built-with)
- [Getting Started](#getting-started)
- [Usage](#usage)
- [Project Structure](#project-structure)
- [Testing](#testing)
- [Deployment](#deployment)
- [Contributing](#contributing)
- [License](#license)

## About

This project provides a complete hiring lifecycle system: authentication, profiles, jobs, companies, applications, and notifications behind an API Gateway. It is designed for teams that want a modular microservices backend with a modern React frontend and Docker-first local setup.

## Built With

- Java 21, Spring Boot
- React + Vite + Tailwind CSS
- PostgreSQL, Kafka (optional)
- Docker Compose, GitHub Actions

## Getting Started

### Prerequisites

- Java 21+
- Node.js 22+
- Docker + Docker Compose

### Installation

```bash
git clone https://github.com/OmNaphade/job-portal.git
cd job-portal
```

Frontend:

```bash
cd frontend
npm install
```

### Configuration

```bash
cp .env.example .env
```

Key variables are documented in the repository `.env.example` and the services' README.

## Usage

Start full local stack:

```bash
docker compose up -d --build
```

Frontend dev:

```bash
cd frontend
npm run dev
```

Run validation scripts (PowerShell on Windows or use WSL):

```powershell
./scripts/synthetic-checks.ps1
./scripts/journey-checks.ps1
```

## Project Structure

- `frontend/` - React app
- `auth_service/`, `user_service/`, `job_service/`, `company_service/`, `application_service/`, `notification_service/` - Java microservices
- `api_gateway/`, `service_registry/`, `config_server/` - infra services
- `scripts/` - helper scripts (synthetic checks, deployments templates)
- `.github/workflows/ci.yml` - CI pipeline

## Testing

- Backend: `./mvnw clean verify` per-service
- Frontend: `npm run test` (Vitest) and `npm run test:e2e` (Playwright)

## Deployment

Deployment templates exist in `scripts/deploy-staging.sh` and `scripts/deploy-production.sh` and detailed instructions are available in `DEPLOYMENT_SCRIPTS.md`.

> Note: Deployment scripts are currently commented out and act as templates. To enable real deployments:
> 1. Provision servers and ensure Docker is installed
> 2. Add secrets in GitHub repo settings: `STAGING_DEPLOY_KEY`, `STAGING_HOST`, `STAGING_USER`, `PROD_DEPLOY_KEY`, `PROD_HOST`, `PROD_USER`
> 3. Uncomment the `REAL DEPLOYMENT` block in the relevant script and adjust paths

## Contributing

Contributions welcome. Please open issues or PRs.

## License

MIT
