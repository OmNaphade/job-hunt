# Job Portal — API & Architecture Documentation

> **Last updated:** 2026-07-25  
> **Stack:** Java 21 · Spring Boot 3.5.11 · Spring Cloud 2025.0.1 · PostgreSQL · Kafka  
> **SOLID compliance:** Verified — SRP, OCP, LSP, ISP, DIP applied across all services

---

## Table of Contents

1. [Application Description](#application-description)
2. [Architecture Overview](#architecture-overview)
3. [Microservices Detail](#microservices-detail)
4. [Service Communication](#service-communication)
5. [Security Model](#security-model)
6. [API Reference](#api-reference)
   - [Auth Service](#1-auth-service-8081)
   - [User/Profile Service](#2-userprofile-service-8082)
   - [Job Service](#3-job-service-8083)
   - [Company Service](#4-company-service-8084)
   - [Application Service](#5-application-service-8085)
   - [Notification Service](#6-notification-service-8086)
7. [Event-Driven Flows](#event-driven-flows)
8. [Monitoring & Observability](#monitoring--observability)
9. [Error Handling](#error-handling)
10. [User Flows](#user-flows)

---

## Application Description

Job Portal is a role-based hiring platform that manages the full recruitment lifecycle:

- Candidates (JOB_SEEKER) create profiles, browse jobs, apply, and track status updates.
- Recruiters (RECRUITER) manage companies, post jobs, and process applications.
- Admins (ADMIN) perform cross-system oversight and protected operations such as user/company moderation and monitoring views.

The frontend communicates through API Gateway, which routes requests to specialized Spring Boot microservices. PostgreSQL stores transactional data, and Kafka powers asynchronous notification flows for application status updates.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENT (Browser/Mobile)               │
└────────────────────────────┬────────────────────────────────┘
                             │ HTTP
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                  API Gateway (:8080)                         │
│  • JWT validation (rejects invalid tokens before routing)   │
│  • Route: /api/auth/**   → auth_service:8081                │
│  • Route: /api/users/**  → user_service:8082                │
│  • Route: /api/jobs/**   → job_service:8083                 │
│  • Route: /api/companies/**  → company_service:8084         │
│  • Route: /api/applications/** → application_service:8085   │
│  • Route: /api/notifications/** → notification_service:8086 │
└──────┬──────────┬──────────┬──────────┬──────────┬──────────┘
       │          │          │          │          │
       ▼          ▼          ▼          ▼          ▼
  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐
  │ auth   │ │ user   │ │  job   │ │company │ │  app   │ │notifi- │
  │ :8081  │ │ :8082  │ │ :8083  │ │ :8084  │ │ :8085  │ │cation  │
  │        │ │        │ │        │ │        │ │        │ │ :8086  │
  └───┬────┘ └───┬────┘ └───┬────┘ └───┬────┘ └───┬────┘ └───┬────┘
      │          │          │          │          │          │
      └──────────┴──────────┴──────────┴──────────┴──────────┘
                                   │
                            ┌──────▼──────┐
                            │  PostgreSQL  │
                            │   :5432      │
                            │  jobapp_db   │
                            └─────────────┘
                                   │
                  ┌────────────────┴────────────────┐
                  │           Apache Kafka           │
                  │  Topics: application-events      │
                  │           job-events             │
                  └─────────────────────────────────┘

  Infrastructure (optional for dev):
  ┌─────────────────┐    ┌─────────────────┐
  │ Eureka Registry  │    │  Config Server  │
  │    :8761         │    │     :8888        │
  └─────────────────┘    └─────────────────┘
```

---

## Microservices Detail

### auth_service (:8081)
**Bounded Context:** Identity & Authentication

**Responsibility (Single Responsibility):**  
Manages user credentials, JWT token lifecycle, and authentication only. Does NOT manage user profiles — that belongs to user_service.

**Key Design Decisions:**
- Authorization enforced via `@PreAuthorize` (not in controller logic)
- Wrong password throws `AuthenticationFailedException` → HTTP 401 (not 404)
- `AccessDeniedException` handled globally → HTTP 403
- Publishes no events (stateless auth)
- Calls user_service via `UserServiceClient` (WebClient + Resilience4j circuit breaker on ALL calls)
- JWT tokens signed with HS256 using configurable secret (`${jwt.secret}`)
- Refresh tokens stored in `refresh_tokens` table (DB-backed, invalidated on logout)

**Entities:** `User` (credentials), `RefreshToken`  
**Tables:** `users`, `refresh_tokens`

**Dependencies:** user_service (via HTTP for profile creation on register, with circuit breaker fallback)

---

### user_service (:8082)
**Bounded Context:** User Profiles & Skills

**Responsibility:**  
Manages user profiles (headline, summary, experience, location) and skills. Separated from auth to allow profile data to evolve independently of credential data.

**Key Design Decisions:**
- `UserRepository.findByUserId()` returns `Optional<Profile>` (not nullable) — safe orElseThrow pattern
- `IUserService` split concerns — `addSkill()` routing logic lives in service, not controller
- Skill catalog is self-maintaining: `POST /api/users/{id}/skills` with `{"skillName":"X"}` finds-or-creates the skill automatically
- `ProfileMapper` has a single `toResponseDTO()` method (duplicate removed)
- `ProfileCreateDTO` validates `@Size(max=200)` headline, `@Size(max=2000)` summary, `@Min(0)` experience

**Entities:** `Profile`, `Skill`, `UserSkill`  
**Tables:** `profiles`, `skills`, `user_skills`

**Dependencies:** None (standalone)

---

### job_service (:8083)
**Bounded Context:** Job Listings

**Responsibility:**  
Full lifecycle of job postings: create, update, publish, close, search. Manages job skills as a first-class sub-resource.

**Key Design Decisions:**
- `JobSkillRepository` fully wired — skills saved on create, deleted on delete, fetched on all reads
- Search query uses `CAST(:param AS string)` for PostgreSQL null-type compatibility
- Status machine: `OPEN → CLOSED/DRAFT`
- `minSalary`/`maxSalary` as DTO field names (user-friendly, maps to `salaryMin`/`salaryMax` in entity via mapper)
- JPQL status filter passes `JobStatus.OPEN` enum as parameter (not hardcoded string)

**Entities:** `Job`, `JobSkill`  
**Tables:** `jobs`, `job_skills`

**Dependencies:** None (standalone)

---

### company_service (:8084)
**Bounded Context:** Company Registry & Recruiter Management

**Responsibility:**  
Manages company profiles and tracks which recruiters belong to which company. Completely independent — does not call any other service.

**Key Design Decisions:**
- `ICompanyService` handles both company CRUD and recruiter sub-resource (bounded together by domain)
- Duplicate company name check on creation
- Recruiter uniqueness enforced per company (`userId + companyId` unique constraint)
- `ErrorResponseDTO` standardized across all exception handlers

**Entities:** `Company`, `Recruiter`  
**Tables:** `companies`, `recruiters`

**Dependencies:** None (standalone)

---

### application_service (:8085)
**Bounded Context:** Job Applications & Status Lifecycle

**Responsibility:**  
Manages the lifecycle of a job application from submission to hire/reject/withdraw. Publishes status-change events to Kafka for the notification pipeline.

**Key Design Decisions:**
- `ApplicationStatus` enum encodes its own valid transitions via `canTransitionTo()` — **OCP compliant**: adding a new status (e.g., `INTERVIEWING`) requires only adding an enum constant, not modifying `ApplicationServiceImpl`
- `userId` in `ApplicationCreateDTO` is `@JsonIgnore` — set from JWT, never from client JSON body (prevents impersonation)
- `ApplicationEventProducer` publishes `{userId, jobId, status, applicationId}` JSON to `application-events` Kafka topic on every status change
- Kafka publish is fire-and-forget with try/catch (Kafka down does not break status update)
- Status machine: `APPLIED → SHORTLISTED|REJECTED → HIRED|REJECTED; any → WITHDRAWN`

**Entities:** `Application`  
**Tables:** `applications`

**Publishes:** `application-events` (Kafka topic)  
**Dependencies:** None (event-driven, not HTTP-coupled)

---

### notification_service (:8086)
**Bounded Context:** User Notifications

**Responsibility:**  
Stores and delivers notifications to users. Receives application lifecycle events via Kafka and translates them into user-readable notifications.

**Key Design Decisions:**
- `NotificationStatus` enum (`UNREAD`, `READ`, `ARCHIVED`) — **OCP compliant**: no magic `"UNREAD"/"READ"` string literals; stored as `EnumType.STRING` in DB
- `NotificationKafkaConsumer` is thin: calls `notificationService.createApplicationStatusNotification()` — message template logic lives in the service layer (SRP)
- `INotificationService` owns the message template (`"Your application for job #X was SHORTLISTED"`)
- Kafka consumer is disabled in dev profile when Kafka is not running

**Entities:** `Notification`  
**Tables:** `notifications`

**Consumes:** `application-events`, `job-events` (Kafka topics)  
**Dependencies:** None (event-driven)

---

## Service Communication

### Synchronous (HTTP/REST)
```
auth_service ──WebClient──► user_service
  On: POST /api/auth/register
  Call: POST /api/users/{userId}/profile  (create profile)
  Circuit Breaker: Resilience4j (name="userServiceCall")
  Fallback: logs warning, registration succeeds without profile
  Config: ${services.user-service.url:http://user-service/api/users}

auth_service ──WebClient──► user_service
  On: DELETE /api/auth/users/{id}
  Call: DELETE /api/users/{userId}/profile
  Circuit Breaker: Resilience4j (name="userServiceCall")
  Fallback: logs warning, deletion proceeds
```

### Asynchronous (Kafka Events)
```
application_service ──Kafka──► notification_service
  Topic: application-events
  Payload: {"userId":4,"jobId":1,"status":"SHORTLISTED","applicationId":1}
  Trigger: PATCH /api/applications/{id}/status
  Consumer group: notification-group
  Effect: Creates UNREAD notification for the job seeker
```

### No Direct Coupling (by design)
- `job_service` ↔ `company_service` — No HTTP calls. Jobs reference `companyId` (foreign key by value, not join)
- `application_service` ↔ `job_service` — No HTTP calls. Applications reference `jobId` by value
- `user_service` ↔ `application_service` — No HTTP calls. Applications reference `userId` by value

---

## Security Model

### JWT Token Structure
```json
{
  "sub": "4",           // userId (principal name in Spring Security)
  "role": "JOB_SEEKER", // single role per user
  "iat": 1784988686,
  "exp": 1784989586     // 15 minutes (access token)
}
```

### Token Lifecycle
```
Register/Login → accessToken (15 min) + refreshToken (7 days, DB-stored)
POST /api/auth/refresh → new accessToken (refreshToken reused)
POST /api/auth/logout  → refreshToken deleted from DB (invalidated)
```

### Authorization Layers
```
Layer 1: API Gateway JwtAuthFilter
  → Rejects requests with invalid/missing JWT (except /register, /login, /refresh)
  → All 6 business services are behind this filter in production

Layer 2: Per-service JwtAuthFilter (Spring Security)
  → Validates JWT signature, sets Authentication in SecurityContext
  → Principal: userId (String from sub claim)
  → Authorities: ["ROLE_JOB_SEEKER"] or ["ROLE_RECRUITER"] or ["ROLE_ADMIN"]

Layer 3: @PreAuthorize (method-level)
  → hasRole('ADMIN') — admin-only operations (delete user, delete company)
  → hasAnyRole('RECRUITER','ADMIN') — create/update jobs, companies, status changes
  → hasRole('JOB_SEEKER') — apply for jobs, withdraw
  → isAuthenticated() — view notifications
  → authentication.name == #userId.toString() or hasRole('ADMIN') — self-or-admin
```

### Roles & Permissions Matrix
| Endpoint | JOB_SEEKER | RECRUITER | ADMIN |
|---|:---:|:---:|:---:|
| POST /api/auth/register | ✅ | ✅ | ✅ |
| GET /api/auth/me | ✅ | ✅ | ✅ |
| DELETE /api/auth/users/{id} | ❌ | ❌ | ✅ |
| GET/PUT /api/users/{id}/profile (own) | ✅ | ✅ | ✅ |
| POST /api/jobs | ❌ | ✅ | ✅ |
| GET /api/jobs (public) | ✅ | ✅ | ✅ |
| POST /api/companies | ❌ | ✅ | ✅ |
| DELETE /api/companies/{id} | ❌ | ❌ | ✅ |
| POST /api/applications | ✅ | ❌ | ❌ |
| PATCH /api/applications/{id}/status | ❌ | ✅ | ✅ |
| PATCH /api/applications/{id}/withdraw | ✅ | ❌ | ❌ |
| GET /api/notifications | ✅ | ✅ | ✅ |
| PATCH /api/notifications/read-all | ✅ | ✅ | ✅ |

---

## API Reference

> **Auth header for protected endpoints:**
> ```
> Authorization: Bearer <accessToken>
> ```
> **Error format (all services):**
> ```json
> { "message": "...", "status": 400, "timestamp": "2026-07-25T20:00:00" }
> ```

---

## 1. Auth Service (:8081)

### POST /api/auth/register — Public
```json
// Request
{
  "email": "alice@example.com",
  "password": "Pass123!",      // min 6 chars
  "role": "JOB_SEEKER",        // JOB_SEEKER | RECRUITER | ADMIN
  "firstName": "Alice",
  "lastName": "Smith"
}

// Response 200
{
  "userId": 1,
  "email": "alice@example.com",
  "role": "JOB_SEEKER",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```
Errors: `409` duplicate email · `400` validation failed

---

### POST /api/auth/login — Public
```json
// Request
{ "email": "alice@example.com", "password": "Pass123!" }

// Response 200 — same structure as register + userId
```
Errors: `404` user not found · `401` wrong password

### Admin Login Availability

- Self-registration for `ADMIN` is intentionally blocked.
- Admin access is provided through deterministic seed configuration in `auth_service`.
- In Docker Compose runs, admin seeding is enabled by default.

Default admin credentials for local/CI compose setup:

- Email: `admin@jobportal.local`
- Password: `Pass123!`

Compose seed controls:

- `ADMIN_SEED_ENABLED=true`
- `ADMIN_SEED_EMAIL=admin@jobportal.local`
- `ADMIN_SEED_PASSWORD=Pass123!`

If you run services manually (without compose), set these env vars for `auth_service` before startup so an admin user is created on boot.

---

### POST /api/auth/refresh — Public
```json
// Request
{ "refreshToken": "eyJhbGciOiJIUzI1NiJ9..." }

// Response 200
{ "accessToken": "new-token...", "refreshToken": "same-token..." }
```
Errors: `401` invalid/expired refresh token

---

### POST /api/auth/logout — Public
```json
// Request
{ "refreshToken": "eyJhbGciOiJIUzI1NiJ9..." }
// Response: 204 No Content
```

---

### GET /api/auth/me 🔒
```json
// Response 200
{
  "id": 1, "email": "alice@example.com",
  "role": "JOB_SEEKER", "firstName": "Alice", "lastName": "Smith"
}
```

---

### GET /api/auth/users/{userId} 🔒 (Self or ADMIN)
Response: same as `/me`. Errors: `403` not own account · `404` not found

---

### PUT /api/auth/users/{userId}/password 🔒 (Self or ADMIN)
```json
// Request
{ "currentPassword": "Pass123!", "newPassword": "NewPass456!" }
// Response: 204 No Content
```
Errors: `401` current password wrong · `400` validation failed

---

### DELETE /api/auth/users/{userId} 🔒 ADMIN only
Response: `204 No Content`. Errors: `403` not admin · `404` not found

---

## 2. User/Profile Service (:8082)

### POST /api/users/{userId}/profile — Internal (no auth) / or 🔒
```json
// Request
{
  "headline": "Senior Java Developer",    // max 200 chars
  "summary": "5+ years of Spring Boot...", // max 2000 chars
  "experienceYears": 5,                   // min 0
  "currentLocation": "Bangalore, India"
}

// Response 201
{
  "id": 1, "userId": 4,
  "headline": "Senior Java Developer",
  "summary": "5+ years of Spring Boot...",
  "experienceYears": 5,
  "currentLocation": "Bangalore, India"
}
```
Errors: `409` profile already exists

---

### GET /api/users/{userId}/profile 🔒
Response: profile object. Errors: `404` not found

---

### PUT /api/users/{userId}/profile 🔒
Request: same as POST. Response: updated profile.

---

### POST /api/users/{userId}/skills 🔒
Accepts skill by name (auto-creates) OR by existing ID:
```json
// By name (recommended)
{ "skillName": "Spring Boot" }

// By ID
{ "skillId": 1 }

// Response 201
{ "id": 1, "name": "Spring Boot" }
```
Errors: `409` already assigned to user · `404` skillId not found

---

### GET /api/users/{userId}/skills 🔒
```json
// Response 200
[{ "id": 1, "name": "Spring Boot" }, { "id": 2, "name": "Java" }]
```

---

### DELETE /api/users/{userId}/skills/{skillId} 🔒
Response: `204 No Content`

---

### GET /api/users/skills 🔒 — Skill Catalog
Returns all skills in the system. Response: array of `{id, name}`.

---

### POST /api/users/skills 🔒 — Create Skill
```json
// Request
{ "name": "Kubernetes" }
// Response 201: { "id": 3, "name": "Kubernetes" }
```

---

### GET /api/users 🔒 — All Profiles (admin use)
Response: array of profile objects.

---

## 3. Job Service (:8083)

### POST /api/jobs 🔒 RECRUITER | ADMIN
```json
// Request
{
  "title": "Senior Backend Engineer",
  "description": "5+ years Java, Spring Boot, microservices",
  "companyId": 1,
  "location": "Bangalore, India",
  "jobType": "FULL_TIME",     // FULL_TIME|PART_TIME|CONTRACT|INTERNSHIP|REMOTE
  "minSalary": 120000,
  "maxSalary": 180000,
  "experienceRequired": 5,   // years
  "skills": ["Java", "Spring Boot", "PostgreSQL", "Kafka"]
}

// Response 201
{
  "id": 5, "title": "Senior Backend Engineer",
  "companyId": 1, "location": "Bangalore, India",
  "jobType": "FULL_TIME", "minSalary": 120000.0, "maxSalary": 180000.0,
  "experienceRequired": 5, "status": "OPEN",
  "skills": ["Java", "Spring Boot", "PostgreSQL", "Kafka"],
  "createdAt": "2026-07-25T20:00:00"
}
```
Errors: `400` validation failed · `403` not recruiter/admin

---

### GET /api/jobs — Public
Response: all jobs with skills array.

---

### GET /api/jobs/{id} — Public
Response: single job. Errors: `404` not found

---

### GET /api/jobs/company/{companyId} — Public
Response: all jobs for a company.

---

### GET /api/jobs/search — Public
All params optional. CAST-safe null handling for PostgreSQL.

| Param | Type | Example |
|---|---|---|
| `keyword` | string | `Java` (searches title + description) |
| `location` | string | `Bangalore` |
| `jobType` | string | `FULL_TIME` |
| `minSalary` | double | `100000` |
| `maxExperience` | int | `5` |

Only returns `status=OPEN` jobs.

```
GET /api/jobs/search?keyword=Java&minSalary=100000&jobType=FULL_TIME
```
Response: filtered array of job objects.

---

### PUT /api/jobs/{id} 🔒 RECRUITER | ADMIN
Request: same as POST. Response: updated job.

---

### PATCH /api/jobs/{id}/status?status={status} 🔒 RECRUITER | ADMIN
`status` values: `OPEN`, `CLOSED`, `DRAFT`  
Response: updated job.

---

### DELETE /api/jobs/{id} 🔒 RECRUITER | ADMIN
Deletes job and all associated `job_skills`. Response: `204 No Content`

---

## 4. Company Service (:8084)

### POST /api/companies 🔒 RECRUITER | ADMIN
```json
// Request
{
  "name": "TechCorp Inc",
  "description": "Leading software company",
  "website": "https://techcorp.com",
  "location": "San Francisco, CA",
  "industry": "TECHNOLOGY"   // TECHNOLOGY|FINANCE|HEALTHCARE|EDUCATION|RETAIL|MANUFACTURING|OTHER
}

// Response 201
{
  "id": 1, "name": "TechCorp Inc",
  "description": "Leading software company",
  "website": "https://techcorp.com",
  "location": "San Francisco, CA",
  "industry": "TECHNOLOGY"
}
```
Errors: `409` company name already exists

---

### GET /api/companies — Public
Response: all companies.

---

### GET /api/companies/{id} — Public
Response: single company. Errors: `404` not found

---

### PUT /api/companies/{id} 🔒 RECRUITER | ADMIN
Request: same as POST. Response: updated company.

---

### DELETE /api/companies/{id} 🔒 ADMIN only
Response: `204 No Content`

---

### POST /api/companies/{companyId}/recruiters 🔒 RECRUITER | ADMIN
```json
// Request
{ "userId": 5, "designation": "Senior Recruiter" }

// Response 201
{
  "id": 1, "userId": 5, "companyId": 1,
  "designation": "Senior Recruiter", "verified": false
}
```
Errors: `409` already a recruiter for this company

---

### GET /api/companies/{companyId}/recruiters — Public
Response: array of recruiter objects.

---

### DELETE /api/companies/{companyId}/recruiters/{recruiterId} 🔒 RECRUITER | ADMIN
Response: `204 No Content`

---

## 5. Application Service (:8085)

### POST /api/applications 🔒 JOB_SEEKER only
UserId is taken from JWT — not from request body.
```json
// Request
{
  "jobId": 1,
  "coverLetter": "I am very interested in this role..."
}

// Response 201
{
  "id": 1, "userId": 4, "jobId": 1,
  "coverLetter": "I am very interested...",
  "status": "APPLIED",
  "appliedAt": "2026-07-25T20:00:00"
}
```
Errors: `400` already applied for this job · `403` not a job seeker

---

### GET /api/applications/{id} 🔒
Response: single application. Errors: `404` not found

---

### GET /api/applications/my 🔒 JOB_SEEKER
Returns authenticated user's applications. Response: array of applications.

---

### GET /api/applications?jobId={jobId} 🔒 RECRUITER | ADMIN
Response: all applications for the specified job.

---

### PATCH /api/applications/{id}/status?status={status} 🔒 RECRUITER | ADMIN

**Status Machine (OCP-compliant — encoded in `ApplicationStatus` enum):**
```
APPLIED     ──► SHORTLISTED, REJECTED
SHORTLISTED ──► HIRED, REJECTED
HIRED       ──► (terminal)
REJECTED    ──► (terminal)
WITHDRAWN   ──► (terminal)
```

```
PATCH /api/applications/1/status?status=SHORTLISTED
```
Response: updated application. Errors: `400` invalid transition

**Side effect:** Publishes Kafka event `→ application-events`:
```json
{"userId":4,"jobId":1,"status":"SHORTLISTED","applicationId":1}
```

---

### PATCH /api/applications/{id}/withdraw 🔒 JOB_SEEKER
Can only withdraw own applications. Cannot withdraw after `HIRED`.  
Response: `204 No Content`. Errors: `400` not owner or already hired.

---

## 6. Notification Service (:8086)

Notifications are created automatically via Kafka events from `application_service`. The POST endpoint exists for admin/testing.

### POST /api/notifications 🔒
```json
// Request
{
  "userId": 4,
  "message": "Your application for job #1 was SHORTLISTED!",
  "type": "APPLICATION_UPDATE"  // APPLICATION_UPDATE|JOB_ALERT|SYSTEM
}

// Response 201
{
  "id": 1, "userId": 4,
  "message": "Your application for job #1 was SHORTLISTED!",
  "type": "APPLICATION_UPDATE",
  "status": "UNREAD",
  "createdAt": "2026-07-25T20:00:00"
}
```

---

### GET /api/notifications 🔒
Returns authenticated user's notifications, newest first.

---

### GET /api/notifications/unread 🔒
Returns only `status=UNREAD` notifications.

---

### GET /api/notifications/unread/count 🔒
```json
// Response 200
{ "count": 3 }
```

---

### PATCH /api/notifications/{id}/read 🔒
```json
// Response 200
{ ..., "status": "READ" }
```

Authorization behavior:
- Admin can mark any notification as read.
- Non-admin users can only mark their own notifications as read.

---

### PATCH /api/notifications/read-all 🔒
Marks all unread notifications as `READ` for the authenticated user.

```json
// Response 200
{ "updated": 5 }
```

---

### DELETE /api/notifications/{id} 🔒
Response: `204 No Content`

Authorization behavior:
- Admin can delete any notification.
- Non-admin users can only delete their own notifications.

---

## Event-Driven Flows

### Application Status Changed → Notification
```
1. RECRUITER: PATCH /api/applications/1/status?status=SHORTLISTED
2. application_service:
   a. Validates: APPLIED → SHORTLISTED ✅ (canTransitionTo())
   b. Saves new status to DB
   c. Publishes to Kafka topic "application-events":
      {"userId":4,"jobId":1,"status":"SHORTLISTED","applicationId":1}
3. notification_service (Kafka consumer):
   a. Reads event from "application-events" topic
   b. Calls notificationService.createApplicationStatusNotification(4,"SHORTLISTED",1)
   c. Service generates: "Your application for job #1 was SHORTLISTED! 🎉"
   d. Saves Notification with status=UNREAD to DB
4. JOB_SEEKER: GET /api/notifications/unread → sees new notification
```

### Circuit Breaker Flow (auth → user)
```
1. JOB_SEEKER: POST /api/auth/register
2. auth_service registers user in DB, generates tokens
3. Calls user_service: POST /api/users/{id}/profile
   ├── user_service UP  → profile created, registration complete ✅
   └── user_service DOWN → circuit breaker OPEN
       → fallback: log warning, return tokens anyway ⚠️
       → user can manually POST /api/users/{id}/profile later
```

---

## Monitoring & Observability

### Health Endpoints
| Service | URL |
|---|---|
| api_gateway | `GET http://localhost:8080/actuator/health` *(secured in hardened setups; may return `401` without token)* |
| auth_service | `GET http://localhost:8081/actuator/health` |
| user_service | `GET http://localhost:8082/actuator/health` |
| job_service | `GET http://localhost:8083/actuator/health` |
| company_service | `GET http://localhost:8084/actuator/health` |
| application_service | `GET http://localhost:8085/actuator/health` |
| notification_service | `GET http://localhost:8086/actuator/health` |

```json
// Response
{ "status": "UP", "components": { "db": { "status": "UP" } } }
```

### Prometheus Metrics
All 6 services expose Prometheus-format metrics at `/actuator/prometheus` (no auth required).

**Sample metrics available:**
- `http_server_requests_seconds_count` — request counts by endpoint, status, method
- `http_server_requests_seconds_sum` — total duration
- `jvm_memory_used_bytes` — JVM heap usage
- `hikaricp_connections_active` — DB connection pool
- `resilience4j_circuitbreaker_state` — circuit breaker state (auth_service)

**Prometheus scrape config:**
```yaml
scrape_configs:
  - job_name: 'job-portal'
    static_configs:
      - targets:
          - 'localhost:8081'  # auth
          - 'localhost:8082'  # user
          - 'localhost:8083'  # job
          - 'localhost:8084'  # company
          - 'localhost:8085'  # application
          - 'localhost:8086'  # notification
    metrics_path: '/actuator/prometheus'
    scrape_interval: 15s
```

### Distributed Tracing
All services configured for Zipkin tracing (`management.tracing.sampling.probability=1.0`).
Start Zipkin: `docker run -p 9411:9411 openzipkin/zipkin`

### Request Logging
`LoggingFilter` + `CorrelationIdFilter` active on all 6 services.  
Each request logs: method, URI, status, duration, and `X-Correlation-ID` header for distributed tracing.

### Monitoring Aggregation Endpoint (Gateway)

`GET /api/monitoring/summary` (via gateway)

- Authorization: `ADMIN` role required
- Optional query parameters:
  - `services=gateway&services=auth&services=user...`
- Returns: sampled service health + selected Prometheus-derived KPIs per service.

Example:
```http
GET /api/monitoring/summary?services=gateway&services=job HTTP/1.1
Authorization: Bearer <admin_token>
```

Non-admin access returns `401` or `403` depending on authentication state.

### Local Validation Scripts

Run through repository-root PowerShell:

- `./scripts/synthetic-checks.ps1`
  - Verifies secured gateway and baseline route guards.
- `./scripts/journey-checks.ps1`
  - Runs candidate/employer journey checks.
  - Admin path gracefully falls back to authorization-guard verification when no admin seed account exists.

---

## Error Handling

All services use a standardized `ErrorResponseDTO`:

```json
{ "message": "string", "status": 400, "timestamp": "2026-07-25T20:00:00" }
```

| HTTP Status | When |
|---|---|
| `400 Bad Request` | Validation failure, invalid status transition, already applied |
| `401 Unauthorized` | Wrong password, invalid/expired token |
| `403 Forbidden` | Missing token, insufficient role (`@PreAuthorize` denied) |
| `404 Not Found` | Resource does not exist |
| `409 Conflict` | Duplicate email, company name, recruiter, skill assignment |
| `500 Internal Server Error` | Unexpected error (logged server-side) |

---

## User Flows

### Job Seeker — Full Flow
```bash
# 1. Register
POST /api/auth/register
{ "email":"alice@example.com","password":"Pass123!","role":"JOB_SEEKER","firstName":"Alice","lastName":"Smith" }

# 2. Build profile
POST /api/users/4/profile
{ "headline":"Java Developer","experienceYears":5,"currentLocation":"Bangalore" }

# 3. Add skills
POST /api/users/4/skills  { "skillName":"Spring Boot" }
POST /api/users/4/skills  { "skillName":"Java" }
POST /api/users/4/skills  { "skillName":"Kafka" }

# 4. Search jobs
GET /api/jobs/search?keyword=Java&minSalary=100000

# 5. Apply
POST /api/applications  { "jobId":5 }

# 6. Check status
GET /api/applications/my

# 7. Read notifications
GET /api/notifications/unread
```

### Recruiter — Full Flow
```bash
# 1. Register
POST /api/auth/register
{ "email":"bob@corp.com","password":"Pass123!","role":"RECRUITER","firstName":"Bob","lastName":"Jones" }

# 2. Create company
POST /api/companies
{ "name":"TechCorp Inc","description":"...","website":"https://techcorp.com","location":"NYC","industry":"TECHNOLOGY" }

# 3. Register self as recruiter
POST /api/companies/1/recruiters
{ "userId":5,"designation":"Senior Recruiter" }

# 4. Post a job
POST /api/jobs
{ "title":"Senior Java Dev","companyId":1,"minSalary":120000,"maxSalary":180000,"jobType":"FULL_TIME","skills":["Java","Spring Boot"] }

# 5. View applicants
GET /api/applications?jobId=5

# 6. Shortlist → Hire
PATCH /api/applications/1/status?status=SHORTLISTED
PATCH /api/applications/1/status?status=HIRED
```

### Admin — Management Flow
```bash
# 1. Login
POST /api/auth/login  { "email":"admin@jobportal.local","password":"Pass123!" }

# 2. View any user
GET /api/auth/users/4

# 3. Delete bad actor
DELETE /api/auth/users/99

# 4. Remove inappropriate company
DELETE /api/companies/3

# 5. Close an old job
PATCH /api/jobs/1/status?status=CLOSED
```

Admin seed note:

- In Docker Compose environments, auth service can seed a deterministic admin user for local/CI testing.
- Default seeded admin credentials are `admin@jobportal.local` / `Pass123!`.
- Seed controls are environment-driven:
  - `ADMIN_SEED_ENABLED`
  - `ADMIN_SEED_EMAIL`
  - `ADMIN_SEED_PASSWORD`
