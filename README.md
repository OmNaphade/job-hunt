# Job Portal Platform

Production-style job portal platform with microservices backend and role-based frontend, built for hiring workflows across candidates, recruiters, and admins.

[![Build Status](https://github.com/OmNaphade/job-portal/actions/workflows/ci.yml/badge.svg)](https://github.com/OmNaphade/job-portal/actions/workflows/ci.yml)
[![Version](https://img.shields.io/badge/version-1.0.0-informational.svg)](https://github.com/OmNaphade/job-portal)
[![Runtime](https://img.shields.io/badge/java-21-orange.svg)](https://www.oracle.com/java/)

## Latest Project Details

- Services: 9 microservices (auth, user, job, company, application, notification, api_gateway, config_server, service_registry)
- Frontend: React + Vite + Tailwind (Node 22), including a live admin monitoring dashboard at `/monitoring`
- Backend: Java 21 (Spring Boot 3.5, Spring Cloud), PostgreSQL per service, Kafka for event-driven notifications
- Resilience & Security: JWT + role-based `@PreAuthorize` RBAC, Resilience4j circuit breakers (auth → user), and a gateway-level per-client-IP rate limiter (stricter on `/api/auth/login`/`register`)
- Observability: Prometheus metrics + Zipkin distributed tracing on every service, Grafana dashboards in `operations/grafana/`, k6 load tests in `performance/k6/`
- CI: GitHub Actions (build, test, integration smoke, Docker image build) — all 7 HTTP-facing services (including `api_gateway`) run their test suites in CI, not just build
- Code Coverage: frontend (Vitest) + backend (JaCoCo, wired via the root `pom.xml` for every module) reported to Codecov — coverage % is only meaningful for `job_service`, `application_service`, and `auth_service` today, the rest still have the Spring Boot stub test
- Deployment: SSH-based scripts are present but **deployment is currently inactive** (scripts are templates). See `DEPLOYMENT_SCRIPTS.md` for activation steps.
- Full API reference and architecture notes: see [`API_DOCS.md`](./API_DOCS.md); local run-from-source walkthrough: see [`STARTUP.md`](./STARTUP.md)

## Table of Contents

- [About](#about)
- [Latest Project Details](#latest-project-details)
- [Built With](#built-with)
- [Getting Started](#getting-started)
- [Usage](#usage)
- [Observability & Admin Tools](#observability--admin-tools)
- [Project Structure](#project-structure)
- [Testing](#testing)
- [Deployment](#deployment)
- [Contributing](#contributing)
- [License](#license)

## About

This project provides a complete hiring lifecycle system: authentication, profiles, jobs, companies, applications, and notifications behind an API Gateway. It is designed for teams that want a modular microservices backend with a modern React frontend, Docker-first local setup, and production-grade observability (metrics, tracing, dashboards) baked in rather than bolted on later.

## Built With

- Java 21, Spring Boot 3.5, Spring Cloud (Gateway, Eureka, Config Server)
- Resilience4j (circuit breakers, rate limiting), Kafka (event-driven notifications)
- React 19 + Vite + Tailwind CSS
- PostgreSQL, Prometheus, Zipkin, Grafana, k6
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
cp frontend/.env.example frontend/.env
```

Root `.env` covers backend secrets (`JWT_SECRET`, `CORS_ALLOWED_ORIGINS`, `MAIL_USERNAME`/`MAIL_PASSWORD`).
Frontend `.env` covers `VITE_API_BASE_URL`, `VITE_ALLOW_SELF_REGISTER`, and `VITE_ZIPKIN_URL` (used by the
admin dashboard's "Open Zipkin Traces" link). See [`STARTUP.md`](./STARTUP.md) for the full list of
backend environment variables and running services from source (without Docker).

## Usage

Start full local stack (Postgres, Kafka, Zipkin, all 9 services):

```bash
docker compose up -d --build
```

A seeded admin account is available by default in Compose (`admin@jobportal.local` / `Pass123!`,
controlled via `ADMIN_SEED_*` env vars — see `API_DOCS.md`).

Frontend dev:

```bash
cd frontend
npm run dev
```

Run validation scripts (PowerShell on Windows or use WSL):

```powershell
./scripts/synthetic-checks.ps1   # route/guard smoke checks against the gateway
./scripts/journey-checks.ps1     # candidate/employer/admin end-to-end flows
```

Additional local-ops scripts live in `scripts/` — environment validation, one-command bootstrap, demo
data seeding, DB backup/restore, failure-injection drills, and a lightweight logs/health dashboard. See
the "Local Operations Scripts" section of [`STARTUP.md`](./STARTUP.md) for the full list.

## Observability & Admin Tools

- **Admin monitoring dashboard** — `/monitoring` in the frontend (ADMIN role required). Live per-service
  health, heap/CPU, DB connections, request counts, circuit-breaker state, and gateway rate-limit
  rejections, with sparkline history, pin/reorder, and JSON export.
- **Zipkin** — distributed traces across all 7 HTTP-facing services, at `http://localhost:9411/zipkin/`
  (also linked directly from the admin dashboard).
- **Prometheus** — every service exposes `/actuator/prometheus`; see `API_DOCS.md` for the scrape config.
- **Grafana** — pre-built dashboards in `operations/grafana/dashboards/` (service overview, Kafka/DB
  overview) — import into any Grafana instance pointed at the Prometheus scrape config.
- **k6 load tests** — `performance/k6/` (login → search → apply journey, notification read path). Run with
  `k6 run performance/k6/login-search-apply.js` against a running stack.

## Project Structure

- `frontend/` - React app (incl. the admin monitoring dashboard)
- `auth_service/`, `user_service/`, `job_service/`, `company_service/`, `application_service/`,
  `notification_service/` - Java microservices
- `api_gateway/`, `service_registry/`, `config_server/` - infra services
- `pom.xml` - Maven parent (shared dependency/plugin management, incl. JaCoCo for every module)
- `operations/grafana/` - Grafana dashboard JSON
- `performance/k6/` - k6 load test scripts
- `scripts/` - local-ops PowerShell scripts (validation, seeding, backup/restore, failure drills,
  deployment templates) — see `STARTUP.md` for the full catalog
- `docker-compose.yml` - full local stack (Postgres, Kafka, Zipkin, all 9 services)
- `.github/workflows/ci.yml` - CI pipeline
- `API_DOCS.md` - full API reference and architecture notes
- `STARTUP.md` - run-from-source guide (no Docker)

## Testing

- Backend: `./mvnw clean verify` per-service (JaCoCo coverage report generated for every module).
  `job_service` and `application_service` have real unit tests over their service layer (pagination,
  search, the `ApplicationStatus` state machine, Kafka publish behavior); `auth_service` has an
  integration test over register/login; `api_gateway` has tests for CORS and the rate limiter.
  `user_service`, `company_service`, and `notification_service` currently only have the Spring Boot
  stub test — real coverage there is the next priority.
- Frontend: `npm run test` (Vitest) and `npm run test:e2e` (Playwright)
- CI runs the full backend test matrix (all 7 HTTP-facing services) plus an integration smoke stage that
  spins up the full Docker Compose stack and runs the synthetic + journey scripts above.

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
