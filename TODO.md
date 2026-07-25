# Job Portal Platform — TODO List

> Auto-generated from full codebase audit | 2026-07-25  
> Stack: Java 21 · Spring Boot 3.5.11 · Spring Cloud 2025.0.1 · PostgreSQL · Kafka · Vault · React Vite Tailwind  
> Total: 118 tasks across 24 phases

> Planning mode: Localhost-first product quality with strong CI via GitHub Actions.  
> Note: Phases 13-20 remain valid for production-hardening; Phases 21-24 are the top implementation priority for this project mode.

---

## PHASE 1 — Fix Compile Errors *(do this first — 4 services won't build)*

- [x] `fix-lombok-company`      — Add Lombok annotations to company_service entities/DTOs (CompanyMapper fails to compile)
- [x] `fix-lombok-notification` — Add Lombok annotations to notification_service entities/DTOs (NotificationMapper fails to compile)
- [x] `fix-lombok-application`  — Add Lombok annotations to application_service entities/DTOs (ApplicationMapper fails to compile)
- [x] `fix-lombok-job`          — Add Lombok annotations to job_service DTOs (JobMapper fails to compile)
- [x] `fix-typo-application`    — Rename `ApplicaitonServiceApplication` → `ApplicationServiceApplication` in .java + pom.xml

---

## PHASE 2 — Fix Infrastructure Services

- [x] `fix-service-registry` — Add `spring-cloud-starter-netflix-eureka-server` dep + `@EnableEurekaServer` + properties (port 8761)
- [x] `fix-config-server`    — Add `@EnableConfigServer` + native backend config + per-service config files (port 8888)
- [x] `fix-server-ports`     — Set unique ports: registry=8761, config=8888, gateway=8080, auth=8081, user=8082, job=8083, company=8084, application=8085, notification=8086

---

## PHASE 3 — Fix API Gateway

- [x] `fix-gateway-routes`    — Define routes for all 6 business services + Eureka client config  *(needs: fix-service-registry)*
- [x] `fix-gateway-jwt-filter`— Add JWT validation WebFilter, whitelist public routes             *(needs: fix-gateway-routes)*
- [x] `fix-gateway-cors`      — Add global CORS config at gateway, remove @CrossOrigin(*) from UserController

---

## PHASE 4 — Fix user_service

- [x] `fix-user-create-endpoint` — Add POST /api/users/{userId}/profile (auth_service calls this on register but only PUT exists!) + rename UserRepository → ProfileRepository
- [x] `fix-user-security`        — Uncomment SecurityConfig, add JwtAuthFilter                    *(needs: fix-user-create-endpoint)*
- [x] `fix-user-n1-query`        — Replace per-skill loop in getUserSkills() with a JOIN/batch query

---

## PHASE 5 — Fix Credentials & Secrets

- [x] `fix-db-credentials`   — Align compose.yaml DB creds (mydatabase/myuser/secret) with application.properties (jobapp_db/postgres/manager)
- [x] `fix-jwt-secret`       — Remove hardcoded JWT secret from JwtUtil.java → read from env var JWT_SECRET or Vault
- [x] `fix-vault-token`      — Replace dummy Vault token 00000000-... with real token or switch to AppRole auth
- [x] `fix-actuator-security`— Change `include: "*"` → `include: health,info,prometheus` in all services

---

## PHASE 6 — Eureka Client Registration *(needs: fix-service-registry)*

- [x] `fix-eureka-clients` — Add eureka.client.service-url.defaultZone to all services. Fix UserServiceClient to use lb://user-service

---

## PHASE 7 — Kafka

- [x] `fix-kafka-config`          — Add spring.kafka.bootstrap-servers to all services, define topics, add Kafka to docker-compose
- [x] `impl-kafka-job-producer`   — Publish JobPostedEvent/JobClosedEvent to job-events topic        *(needs: fix-kafka-config, impl-job-service)*
- [x] `impl-kafka-app-producer`   — Publish ApplicationEvent to application-events topic             *(needs: fix-kafka-config, impl-application-service)*
- [x] `impl-kafka-notif-consumer` — Consume job-events + application-events, trigger email/in-app   *(needs: fix-kafka-config, impl-kafka-app-producer, impl-notification-service)*

---

## PHASE 8 — Implement Skeleton Services *(all are empty shells)*

- [x] `impl-job-service`         — JobRepository + JobService + JobController (CRUD + search/filter) + SecurityConfig  *(needs: fix-lombok-job)*
- [x] `impl-company-service`     — CompanyRepository + RecruiterRepository + services + CompanyController              *(needs: fix-lombok-company)*
- [x] `impl-application-service` — ApplicationRepository + service (status machine) + ApplicationController            *(needs: fix-lombok-application, fix-typo-application)*
- [x] `impl-notification-service`— NotificationRepository + service + controller + email integration                   *(needs: fix-lombok-notification)*

---

## PHASE 9 — Cross-Cutting Concerns

- [x] `add-input-validation`    — Add @NotBlank/@NotNull/@Min/@Max/@Email to all DTOs, @Valid on controllers, 400 handler in GlobalExceptionHandler
- [x] `add-distributed-tracing` — Add Micrometer Tracing + Zipkin to all services + docker-compose  *(needs: impl-job-service, impl-company-service)*
- [x] `add-resilience4j-config` — Configure circuit breakers for all inter-service HTTP calls, add fallback methods
- [x] `add-swagger-config`      — Add @OpenAPIDefinition + @SecurityScheme (JWT Bearer) + @Operation on all controllers
- [x] `add-method-security`     — Enable @EnableMethodSecurity, replace manual validateAccess()/validateAdmin() with @PreAuthorize

---

## PHASE 10 — Auth Improvements

- [x] `impl-refresh-token-store` — DB-backed RefreshToken entity (revocable), POST /api/auth/logout endpoint, revoke on password change/delete

---

## PHASE 11 — DevOps & Architecture

- [x] `fix-docker-compose`  — Root docker-compose.yml: all 9 services + PostgreSQL + Kafka + Zookeeper + Vault + Zipkin  *(needs: fix-server-ports)*
- [x] `add-per-service-db`  — Separate DBs per service: auth_db, user_db, job_db, company_db, application_db, notification_db
- [x] `add-ci-cd`           — .github/workflows/ci.yml: build, test, Docker build/push. Consider Maven parent POM
- [x] `add-integration-tests`— @SpringBootTest + Testcontainers for auth_service and user_service. Fill in empty test stubs

---

## PHASE 12 — Post-Implementation Fixes & Documentation

- [x] `fix-actuator-security`       — Added `/actuator/health`, `/actuator/info`, `/actuator/prometheus` to `permitAll()` in all 6 service SecurityConfigs
- [x] `fix-auth-logout-security`    — Added `POST /api/auth/logout` to `permitAll()` in auth_service SecurityConfig
- [x] `fix-user-skill-by-name`      — Updated `POST /api/users/{id}/skills` to accept `{"skillName":"X"}` (auto-creates skill) + added `GET/POST /api/users/skills` catalog endpoints
- [x] `fix-notification-post`       — Added `POST /api/notifications` endpoint for internal/admin use (Kafka consumer also uses service layer directly)
- [x] `remove-crossorigin-wildcard` — Removed `@CrossOrigin(origins="*")` from UserController (CORS handled at gateway)
- [x] `create-api-docs`             — Created `API_DOCS.md` with all 6 services, full endpoint reference, request/response examples, user flows
- [x] `create-startup-guide`        — Created `STARTUP.md` with prerequisites, build steps, ports, env vars, Docker Compose instructions
- [x] `fix-bom-encoding`            — Stripped UTF-8 BOM from 48 Java source files (Windows file creation adds \ufeff which breaks Java compiler)
- [x] `test-all-apis`               — 33/33 API tests passing across all 6 services (auth, user, job, company, application, notification)

---

## PHASE 13 — Identity, Access, and Account Lifecycle

- [~] `feat-admin-invite-flow`         — Implementation design and data model captured in IDENTITY_ACCESS_PLAN.md; endpoint rollout pending
- [~] `feat-email-verification`        — Verification token flow design captured in IDENTITY_ACCESS_PLAN.md; backend implementation pending
- [x] `feat-password-reset`            — Implemented request/confirm password reset endpoints with hashed expiring tokens and refresh-token revocation on reset
- [~] `feat-login-rate-limits`         — Rate limit strategy captured in IDENTITY_ACCESS_PLAN.md; gateway/auth enforcement pending
- [~] `feat-account-lockout`           — Lockout model and fields documented in IDENTITY_ACCESS_PLAN.md; auth changes pending
- [~] `feat-session-management`        — Session listing/revocation endpoint plan captured in IDENTITY_ACCESS_PLAN.md; implementation pending

---

## PHASE 14 — Security Hardening and Compliance

- [~] `hardening-jwt-key-rotation`     — Key rotation design with `kid` captured in SECURITY_HARDENING_PLAN.md; implementation pending
- [~] `hardening-secret-rotation`      — Secret rotation workflow documented in SECURITY_HARDENING_PLAN.md; automation pending
- [~] `hardening-dependency-scan`      — Security scan gate policy documented in SECURITY_HARDENING_PLAN.md; full Maven gate rollout pending
- [x] `hardening-headers`              — Enforced gateway security headers via api_gateway filter (HSTS, CSP, X-Content-Type-Options and related headers)
- [~] `hardening-audit-trail`          — Immutable audit trail schema and event model documented in SECURITY_HARDENING_PLAN.md; implementation pending
- [~] `hardening-threat-model`         — STRIDE modeling approach documented in SECURITY_HARDENING_PLAN.md; per-service threat sheets pending

---

## PHASE 15 — Data Consistency and Event Reliability

- [~] `reliability-outbox-pattern`     — Outbox transactional design documented in RELIABILITY_EVENT_PLAN.md; service implementation pending
- [~] `reliability-idempotent-consume` — Dedup store and idempotent consumer model documented in RELIABILITY_EVENT_PLAN.md; implementation pending
- [~] `reliability-dlq-retry`          — Retry and DLQ topology documented in RELIABILITY_EVENT_PLAN.md; broker/service config pending
- [~] `reliability-schema-versioning`  — Event schema-versioning policy documented in RELIABILITY_EVENT_PLAN.md; enforcement pending
- [~] `reliability-saga-compensation`  — Saga compensation approach documented in RELIABILITY_EVENT_PLAN.md; workflow implementation pending

---

## PHASE 16 — API Governance and Contract Quality

- [~] `api-versioning-policy`          — Versioning policy and rollout plan defined in API_GOVERNANCE.md and API_CONTRACT_ROLLOUT_PLAN.md; route migration pending
- [~] `api-contract-tests`             — Contract strategy and rollout plan documented in API_GOVERNANCE.md and API_CONTRACT_ROLLOUT_PLAN.md; suites pending
- [~] `api-error-standard`             — Standard error payload and rollout plan documented in API_GOVERNANCE.md and API_CONTRACT_ROLLOUT_PLAN.md
- [x] `api-pagination-sorting`         — Implemented pagination/sorting on jobs and notifications list/search endpoints with validated sort fields
- [~] `api-idempotency-post`           — Idempotency contract and rollout plan documented in API_GOVERNANCE.md and API_CONTRACT_ROLLOUT_PLAN.md; implementation pending

---

## PHASE 17 — Observability, SLOs, and Operations

- [x] `ops-admin-monitoring-dashboard-ui` — Add admin-only monitoring dashboard route/page in frontend with role guard and restricted navigation
- [x] `ops-admin-monitoring-metric-collector` — Collect per-service actuator health + prometheus metrics (gateway/auth/user/job/company/application/notification)
- [x] `ops-admin-monitoring-widget-customization` — Add widget toggle controls, service scope filters, and auto-refresh interval control persisted for operator use
- [x] `ops-admin-monitoring-snapshot-export` — Add JSON snapshot export for sampled monitoring state and trend history
- [x] `ops-admin-monitoring-backend-aggregation` — Add gateway endpoint `GET /api/monitoring/summary` to aggregate actuator health + metrics server-side
- [x] `ops-admin-monitoring-threshold-badges` — Add warning/critical health badges for CPU, heap memory, and DB connection saturation
- [x] `ops-admin-monitoring-layout-modes` — Add compact/expanded dashboard modes with local preference persistence
- [x] `ops-admin-monitoring-card-ordering` — Add card pinning and up/down reordering with persistent state
- [x] `ops-slo-definition`             — Define SLOs/SLIs per service (availability, latency, error budget)
- [x] `ops-dashboards`                 — Built Grafana dashboard JSON assets for service, DB, Kafka, and gateway health
- [x] `ops-alert-rules`                — Add actionable alerting (latency, 5xx rate, consumer lag, DB saturation)
- [x] `ops-log-correlation`            — Standardized X-Trace-Id propagation and MDC traceId usage across gateway and all business services
- [x] `ops-synthetic-checks`           — Add synthetic API checks for login, job search, apply, and notification read flow

---

## PHASE 18 — Performance and Scalability

- [x] `perf-load-testing`              — Added k6 scenarios under performance/k6 for login/search/apply and notification read flows
- [x] `perf-db-index-tuning`           — Added baseline index tuning script DATABASE_INDEX_TUNING.sql for key query paths
- [~] `perf-cache-strategy`            — Redis cache rollout strategy and target endpoints documented in PERFORMANCE_SCALING_STRATEGY.md
- [~] `perf-kafka-throughput`          — Kafka throughput and lag tuning strategy documented in PERFORMANCE_SCALING_STRATEGY.md
- [~] `perf-pool-sizing`               — DB/thread/consumer pool sizing baseline documented in PERFORMANCE_SCALING_STRATEGY.md

---

## PHASE 19 — Frontend Productization and UX

- [x] `frontend-role-route-guards`     — Enforce route-level role guards with unauthorized screen and reason messaging
- [x] `frontend-form-validation`       — Add schema-based client validation (zod/yup) aligned with backend DTO constraints
- [x] `frontend-query-layer`           — Introduced React Query query/mutation layer with caching, retries, and optimistic updates in active notification workflows
- [~] `frontend-design-system`         — Tokenized CSS baseline and shared feedback components present; extraction into dedicated design-system package pending
- [~] `frontend-accessibility-pass`    — Accessibility-focused guard and feedback UX improved; full WCAG audit/report pending
- [x] `frontend-e2e-tests`             — Add Playwright smoke tests for candidate, employer, and admin critical flows

---

## PHASE 20 — Delivery, Reliability, and DR Readiness

- [x] `delivery-parent-pom`            — All service modules now inherit from root parent BOM via ../pom.xml with centralized shared properties
- [x] `delivery-environment-promotion` — Added environment promotion workflow in operations/ENV_PROMOTION.md
- [x] `delivery-zero-downtime`         — Added zero-downtime strategy and migration rules in operations/ZERO_DOWNTIME.md
- [x] `dr-backup-restore`              — Added backup/restore runbook and executable scripts (scripts/backup-db.ps1, scripts/restore-db.ps1)
- [~] `dr-chaos-exercises`             — Chaos exercise playbook added in operations/CHAOS_EXERCISES.md; execution evidence pending
- [x] `runbook-oncall`                 — Added operations/ONCALL_RUNBOOK.md with triage and mitigation procedures

---

## PHASE 21 — Frontend Excellence (Max CSS + UX)

- [~] `ui-design-tokens-v1`            — Token variables applied in index.css; expanded semantic token map and usage audit pending
- [~] `ui-typography-system`           — Typography hierarchy implemented in global styles; formal type scale documentation pending
- [~] `ui-layout-consistency`          — Shared AppLayout/SectionCard patterns in use; final layout audit across all pages pending
- [~] `ui-form-polish`                 — Inline validation and control consistency improved; full module-by-module polish pass pending
- [~] `ui-micro-interactions`          — Route/toast/form transitions implemented; interaction audit and tuning pending
- [x] `ui-empty-loading-error-pattern` — Shared ListSkeleton/EmptyState/ErrorMessage patterns implemented and reused across modules
- [~] `ui-accessibility-local-pass`    — Accessibility audit plan created in frontend/ACCESSIBILITY_AUDIT.md; cross-page remediation pass pending
- [~] `ui-mobile-first-refinement`     — Mobile-responsive baseline applied; final flow-by-flow small-screen audit pending

---

## PHASE 22 — Functional Completeness (Local End-to-End)

- [~] `func-candidate-journey-pass`    — Journey checks script present and candidate notification/guard behavior hardened; live full-flow execution evidence pending
- [~] `func-employer-journey-pass`     — Journey checks script present and employer-facing notification controls improved; live full-flow execution evidence pending
- [~] `func-admin-journey-pass`        — Journey checks script present and admin monitoring + notification action controls expanded; live full-flow execution evidence pending
- [x] `func-role-guard-finalize`       — Route-level and action-level role/ownership guards finalized for protected routes and notification actions
- [~] `func-search-filter-depth`       — Deterministic filtering/sorting strengthened in notifications; cross-module search/filter expansion pending
- [x] `func-notification-ux-complete`  — Add notification sorting, status filters, bulk mark-read, and optimistic updates
- [x] `func-regression-checklist`      — Create a manual regression checklist covering all major user and role journeys

---

## PHASE 23 — Local Reliability and Developer Productivity

- [x] `local-one-command-start`        — Add one-command local bootstrap script for backend services and frontend
- [x] `local-seed-demo-data`           — Add reproducible demo seed data for users, companies, jobs, and applications
- [x] `local-env-validation`           — Add startup-time validation for required env vars with clear error output
- [x] `local-logs-dashboard-lite`      — Add simple local observability bundle (logs + actuator + quick dashboard links)
- [x] `local-test-data-reset`          — Add reset script for DB and Kafka topics to quickly restore clean test state
- [x] `local-failure-simulation`       — Add scripts to simulate key failures (service down, kafka down) and verify recovery UX

---

## PHASE 24 — GitHub Actions Quality Gates (Mandatory)

- [x] `ci-frontend-build-test`         — Add frontend install, build, and unit test jobs on pull requests
- [x] `ci-backend-matrix-build`        — Add per-service Maven compile/test matrix with cached dependencies
- [x] `ci-integration-smoke`           — Add docker-compose integration smoke workflow for critical API paths
- [x] `ci-contract-and-lint`           — Add API contract checks and style/lint gates for Java and frontend code
- [x] `ci-security-baseline`           — Add dependency vulnerability checks (npm + Maven) with fail threshold policy
- [x] `ci-artifact-publish`            — Publish build artifacts and test reports for each workflow run
- [~] `ci-branch-protection-rules`     — Branch protection configuration guide added in operations/BRANCH_PROTECTION.md; GitHub settings apply step pending

---

## Status Legend
- [ ] Pending
- [~] In Progress  
- [x] Done
