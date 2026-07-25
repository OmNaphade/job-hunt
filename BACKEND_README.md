# Job Portal Backend README

Last updated: 2026-07-25

This document is the backend reference for the Job Portal microservices system.

## Primary Goal

This backend must be documented by what it can do (functionalities), not only by architecture.
Use this file as a functionality-first source of truth.

## Stack

- Java 21+
- Spring Boot 3.5.11
- Spring Cloud 2025.0.1
- PostgreSQL
- Kafka
- Eureka (service registry)
- Config Server
- API Gateway

## Services and Ports

- api_gateway: 8080
- auth_service: 8081
- user_service: 8082
- job_service: 8083
- company_service: 8084
- application_service: 8085
- notification_service: 8086
- service_registry: 8761
- config_server: 8888

## High-Level Architecture

- API Gateway routes client traffic to business services.
- auth_service handles credentials, JWT, refresh/logout lifecycle.
- user_service handles profile and skills domain.
- job_service handles job posting lifecycle and search.
- company_service handles companies and recruiters.
- application_service handles job applications and status transitions.
- notification_service consumes events and serves user notifications.
- application_service and job_service publish events to Kafka; notification_service consumes.

## Database Model

Database-per-service strategy is supported.

Databases:

- auth_db
- user_db
- job_db
- company_db
- application_db
- notification_db

Initialization script:

- init-db.sql at workspace root

Reference:

- DATABASES.md

## Startup Options

### Option 1: Full Docker Startup

Use root compose file:

- docker-compose up -d

This starts PostgreSQL, Kafka, Zookeeper, infrastructure services, and all business services.

### Option 2: Local Service Startup

Build per service:

- .\mvnw.cmd package -DskipTests

Run service jar with dev profile:

- java -jar <service>\target\<service>-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

Reference:

- STARTUP.md

## GitHub Actions

Repository CI is defined in:

- .github/workflows/ci.yml

Pipeline coverage includes:

- Frontend build, test, lint, and Playwright smoke
- Backend matrix build/test across all business services
- Infrastructure service build (registry, config, gateway)
- Dependency security baseline checks on pull requests
- Docker Compose integration smoke plus scripted synthetic/journey checks
- Artifact publishing for jars, reports, and compose logs

## Security and Auth Notes

- JWT is used for service protection.
- Access token and refresh token flow is implemented.
- Logout revokes refresh token.
- Register policy enforcement:
  - Self-registration allows JOB_SEEKER and RECRUITER.
  - ADMIN self-registration is blocked.
- Gateway health endpoint may be intentionally secured in hardened setups (expect 200 or 401 depending on profile/policy).

Deterministic admin seed support:

- Auth service can seed an admin at startup via env-driven toggle.
- Compose defaults enable this seed for local and CI journeys.
- Default seeded credentials:
  - email: admin@jobportal.local
  - password: Pass123!
- Controls:
  - ADMIN_SEED_ENABLED
  - ADMIN_SEED_EMAIL
  - ADMIN_SEED_PASSWORD

## API Documentation

Full endpoint reference and payload examples:

- API_DOCS.md

## Current Backend Status (Implementation Summary)

- Core controllers are implemented for all 6 business services.
- Role-based method security is in place with @PreAuthorize where applicable.
- Application status update and notification flow is event-driven via Kafka.
- Actuator endpoints are available per service for health checks.
- API gateway now exposes admin-only aggregated monitoring summary for frontend operations dashboard.

## Backend Functionality Inventory

### 1. auth_service

Functionalities present:

- Register user with role policy enforcement
  - Allows JOB_SEEKER and RECRUITER
  - Blocks ADMIN self-registration
- Login with JWT issuance
- Refresh token flow
- Logout token revocation
- Get current user details
- Get user by ID (self or admin)
- Update password (self or admin)
- Delete user (admin)

Endpoints:

- POST /api/auth/register
- POST /api/auth/login
- POST /api/auth/refresh
- POST /api/auth/logout
- GET /api/auth/me
- GET /api/auth/users/{userId}
- PUT /api/auth/users/{userId}/password
- DELETE /api/auth/users/{userId}

### 2. user_service

Functionalities present:

- Create profile
- Get profile by user ID
- Update profile
- Get all profiles
- Add skill to user
- Get user skills
- Remove skill from user
- Get global skill catalog
- Create global skill

Endpoints:

- GET /api/users
- POST /api/users/{userId}/profile
- GET /api/users/{userId}/profile
- PUT /api/users/{userId}/profile
- GET /api/users/{userId}/skills
- POST /api/users/{userId}/skills
- DELETE /api/users/{userId}/skills/{skillId}
- GET /api/users/skills
- POST /api/users/skills

### 3. job_service

Functionalities present:

- Create job
- Get all jobs
- Search jobs with optional filters
- Get job by ID
- Get jobs by company
- Update full job payload
- Update job status (OPEN/CLOSED/DRAFT)
- Delete job

Endpoints:

- POST /api/jobs
- GET /api/jobs
- GET /api/jobs/search
- GET /api/jobs/{id}
- GET /api/jobs/company/{companyId}
- PUT /api/jobs/{id}
- PATCH /api/jobs/{id}/status
- DELETE /api/jobs/{id}

### 4. company_service

Functionalities present:

- Create company
- List companies
- Get company by ID
- Update company
- Delete company
- Add recruiter to company
- List company recruiters
- Remove recruiter from company

Endpoints:

- POST /api/companies
- GET /api/companies
- GET /api/companies/{id}
- PUT /api/companies/{id}
- DELETE /api/companies/{id}
- POST /api/companies/{companyId}/recruiters
- GET /api/companies/{companyId}/recruiters
- DELETE /api/companies/{companyId}/recruiters/{recruiterId}

### 5. application_service

Functionalities present:

- Submit application (job seeker)
- Get application by ID
- Get my applications
- Get applications by job (recruiter/admin)
- Update application status (recruiter/admin)
- Withdraw application (job seeker)
- Publish application status events to Kafka

Endpoints:

- POST /api/applications
- GET /api/applications/{id}
- GET /api/applications/my
- GET /api/applications?jobId=...
- PATCH /api/applications/{id}/status
- PATCH /api/applications/{id}/withdraw

### 6. notification_service

Functionalities present:

- Create notification
- Get my notifications
- Get unread notifications
- Get unread count
- Mark notification as read
- Mark all notifications as read for current user
- Delete notification
- Consume Kafka events and persist notifications

Endpoints:

- POST /api/notifications
- GET /api/notifications
- GET /api/notifications/unread
- GET /api/notifications/unread/count
- PATCH /api/notifications/{id}/read
- PATCH /api/notifications/read-all
- DELETE /api/notifications/{id}

### 7. Platform and Infrastructure Services

Functionalities present:

- API Gateway routing for all business services
- API Gateway container routing via env placeholders (`AUTH_SERVICE_URI`, `USER_SERVICE_URI`, etc.) for Docker-safe service resolution
- JWT verification filter for protected API access
- Admin-only monitoring aggregation endpoint that fetches actuator health and prometheus metrics across gateway and all business services
- Configurable monitoring target base URLs via gateway properties
- Service discovery via Eureka
- Centralized config serving
- Health and metrics exposure for operations

Monitoring endpoint:

- GET /api/monitoring/summary
  - Optional query params: services=gateway&services=auth&services=user...
  - Authorization: ADMIN role required
  - Returns sampled service snapshots with key runtime KPIs

Reference docs:

- API_DOCS.md
- STARTUP.md
- DATABASES.md
- SLOS.md
- ALERT_RULES.md
- REGRESSION_CHECKLIST.md
- scripts/README.md
- API_GOVERNANCE.md
- performance/README.md
- DATABASE_INDEX_TUNING.sql
- PERFORMANCE_SCALING_STRATEGY.md
- pom.xml (root parent BOM baseline)
- operations/ENV_PROMOTION.md
- operations/ZERO_DOWNTIME.md
- operations/BACKUP_RESTORE_RUNBOOK.md
- operations/ONCALL_RUNBOOK.md
- operations/BRANCH_PROTECTION.md
- operations/LOG_CORRELATION.md
- operations/CHAOS_EXERCISES.md
- operations/grafana/README.md
- IDENTITY_ACCESS_PLAN.md
- SECURITY_HARDENING_PLAN.md
- RELIABILITY_EVENT_PLAN.md
- API_CONTRACT_ROLLOUT_PLAN.md

## Backend Context Update Log

Use this section to keep backend context current over time.

2026-07-25:

- Verified backend-to-frontend endpoint mapping coverage.
- Enforced auth register role policy to block ADMIN self-registration.
- Added explicit invalid-role handling with 400 response.
- Login user-not-found behavior aligned to registration-assist frontend flow.
- Added gateway admin-only aggregated monitoring endpoint for frontend operations dashboard (`/api/monitoring/summary`).
- Added SLOS.md with service-level objectives, SLIs, error budgets, and alert threshold baselines.
- Added ALERT_RULES.md with actionable warning/critical rule definitions for gateway, services, and Kafka.
- Added local operations toolkit scripts for bootstrap, validation, synthetic checks, reset, and failure simulation.
- Added reproducible demo data seeding and local logs dashboard-lite scripts for reliability workflows.
- Stabilized Docker runtime startup and fixed gateway/container routing for cross-service forwarding.
- Added executable-jar Docker packaging flow for all services (Spring Boot repackage in container builds).
- Verified `scripts/synthetic-checks.ps1` and `scripts/journey-checks.ps1` pass against local compose stack.
- Expanded CI workflow with integration smoke, security baseline checks, contract/lint checks, and artifact publishing.
- Added API governance contract policy and baseline performance toolkit (k6 scripts + DB index recommendations).
- Added delivery and DR runbooks (promotion, zero-downtime, backup/restore, on-call, branch protection, chaos exercises).
- Added Grafana dashboard assets for service/Kafka/DB operational visibility.
- Added notification ownership-aware action controls and bulk mark-read endpoint.
- Added root Maven parent BOM baseline and performance scaling strategy guidance.
- Added gateway security-headers filter and trace-id propagation filter (`X-Trace-Id`) for stronger security and request correlation.
- Added implementation-plan docs for identity/access, security hardening, reliability/event patterns, and API contract rollout.
- Completed trace-id propagation standardization across gateway and all business-service correlation filters.

## How to Maintain This File

After backend changes, update:

1. Backend Functionality Inventory first.
2. Endpoint lists under impacted service.
3. Services and ports if any network/port change occurs.
4. Security and auth notes if token/role policy changes.
5. Database model section when schemas or DB strategy changes.
6. Backend Context Update Log with date and concise bullets.
7. Add links to any new backend docs or runbooks.
8. Keep SLO/SLI targets synchronized with SLOS.md after reliability updates.
