# Job Portal Frontend (Internal Operations Portal)

Last updated: 2026-07-25

This is the internal React + Vite + Tailwind frontend for the Job Portal microservices backend.
It is role-aware, API-mapped to backend services, and designed for authenticated operational use.

## Primary Goal

This frontend must be documented by what it can do (functionalities) and which backend endpoints each functionality maps to.

## Tech Stack

- React + Vite
- Tailwind CSS
- Axios
- React Router

## Local Run

1. Open terminal in frontend folder.
2. Install dependencies:
	 npm install
3. Start dev server:
	 npm run dev
4. Build production bundle:
	 npm run build

## CI / GitHub Actions

Frontend quality gates run via repository workflow:

- file: `.github/workflows/ci.yml`
- jobs include:
	- npm install/test/build
	- eslint lint
	- Playwright smoke tests
	- artifact upload (`frontend-dist`)

## Environment Variables

- VITE_API_BASE_URL
	- Default: http://localhost:8080
	- Purpose: API Gateway base URL
- VITE_ALLOW_SELF_REGISTER
	- Default: false
	- Purpose: Controls whether self-registration UI/actions are enabled

Example (.env):

VITE_API_BASE_URL=http://localhost:8080
VITE_ALLOW_SELF_REGISTER=false

## Access and Auth Behavior

- App is private by default; business routes require authentication.
- Public auth route is /auth.
- Role-restricted routes show an explicit Unauthorized screen with reason and required role messaging.
- Login supports explicit tracks:
	- Candidate login (expects JOB_SEEKER)
	- Employer login (expects RECRUITER)
	- Admin login (expects ADMIN)
- Backend decides actual role from credentials.
- If selected login track does not match actual role, user sees warning toast and is redirected based on actual role.
- Redirects after login:
	- JOB_SEEKER -> /applications
	- RECRUITER -> /jobs
	- ADMIN -> /

## Registration Policy (Frontend + Backend Aligned)

- Self-registration is available only when VITE_ALLOW_SELF_REGISTER=true.
- Allowed self-registration roles:
	- JOB_SEEKER (candidate)
	- RECRUITER (employer)
- ADMIN self-registration is blocked by backend policy.
- If login fails with user-not-found and self-register is enabled, UI offers:
	- Register as Candidate
	- Register as Employer

## Global UX Infrastructure

- Toast notification system:
	- Success, error, warning, info toasts
	- Auto-dismiss with close action
- Unified request-state hook:
	- Shared loading/error/success handling
	- Optional success and error toasts
- Reusable list states:
	- Loading skeleton blocks
	- Empty-state cards

## Functional Modules

### 1. Auth Module

Implemented:

- Login
- Register (conditional by env)
- Refresh access token
- Logout
- Candidate/Employer/Admin login track UX
- User-not-found registration assist

Mapped backend endpoints:

- POST /api/auth/login
- POST /api/auth/register
- POST /api/auth/refresh
- POST /api/auth/logout
- GET /api/auth/me

### 2. Profile and Skills Module

Implemented:

- Load own profile
- Create/update profile
- Load own skills
- Add skill by name
- Remove skill by ID
- Load skill catalog
- Create catalog skill
- Load all profiles directory

Mapped backend endpoints:

- GET /api/users/{userId}/profile
- POST /api/users/{userId}/profile
- PUT /api/users/{userId}/profile
- GET /api/users/{userId}/skills
- POST /api/users/{userId}/skills
- DELETE /api/users/{userId}/skills/{skillId}
- GET /api/users/skills
- POST /api/users/skills
- GET /api/users

### 3. Jobs Module

Implemented:

- List jobs
- Search jobs by optional filters
- Get job by ID
- Get jobs by company
- Create job
- Update job
- Update job status
- Delete job

Mapped backend endpoints:

- GET /api/jobs
- GET /api/jobs/search
- GET /api/jobs/{id}
- GET /api/jobs/company/{companyId}
- POST /api/jobs
- PUT /api/jobs/{id}
- PATCH /api/jobs/{id}/status
- DELETE /api/jobs/{id}

### 4. Companies and Recruiters Module

Implemented:

- List companies
- Get company by ID
- Create company
- Update company
- Delete company (admin role)
- Add recruiter to company
- List company recruiters
- Remove recruiter mapping

Mapped backend endpoints:

- GET /api/companies
- GET /api/companies/{id}
- POST /api/companies
- PUT /api/companies/{id}
- DELETE /api/companies/{id}
- POST /api/companies/{companyId}/recruiters
- GET /api/companies/{companyId}/recruiters
- DELETE /api/companies/{companyId}/recruiters/{recruiterId}

### 5. Applications Module

Implemented:

- Submit application (job seeker)
- Load my applications
- Get application by ID
- Get applications by job ID (recruiter/admin)
- Update application status (recruiter/admin)
- Withdraw application (job seeker)

Mapped backend endpoints:

- POST /api/applications
- GET /api/applications/my
- GET /api/applications/{id}
- GET /api/applications?jobId=...
- PATCH /api/applications/{id}/status
- PATCH /api/applications/{id}/withdraw

### 6. Notifications Module

Implemented:

- Load all notifications
- Load unread notifications
- Load unread count
- Mark notification as read
- Bulk mark-read for filtered list (optimistic update)
- Sort notifications (newest/oldest)
- Filter by status and notification type
- Create notification
- Delete notification
- Bulk mark-read via dedicated endpoint
- React Query caching and optimistic mutation handling for notifications

Mapped backend endpoints:

- GET /api/notifications
- GET /api/notifications/unread
- GET /api/notifications/unread/count
- PATCH /api/notifications/{id}/read
- PATCH /api/notifications/read-all
- POST /api/notifications
- DELETE /api/notifications/{id}

### 7. Auth User Operations (Operational Tools)

Implemented from frontend operational panel:

- Lookup user by ID
- Update password by user ID
- Delete user by user ID (admin only)

Mapped backend endpoints:

- GET /api/auth/users/{userId}
- PUT /api/auth/users/{userId}/password
- DELETE /api/auth/users/{userId}

### 8. Admin Monitoring Dashboard

Implemented:

- Admin-only monitoring route and navigation visibility
- Backend-aggregated multi-service health and Prometheus metric ingestion via gateway
- KPI cards for heap memory, CPU, DB connections, request count, circuit breakers, JVM threads, and GC pauses
- Widget toggles for dashboard customization
- Service-scope filters to monitor selected services only
- Auto-refresh controls (pause/10s/20s/30s/60s)
- Snapshot export to JSON for local analysis
- Threshold badges (healthy/warning/critical) for key saturation metrics
- Layout mode controls (compact/expanded) with persisted preference
- Card pinning and up/down ordering with persisted state

Mapped endpoints:

- GET /api/monitoring/summary?services=... (aggregated by gateway for selected services)

## UI and Styling Notes

- Internal-portal visual identity with clean, low-noise palette
- Responsive layouts for desktop and mobile
- Accessible focus states and reduced-motion handling
- Reusable card and state components for consistency
- Schema-based auth form validation using Zod with inline field-level feedback
- Playwright smoke tests for auth access and protected-route redirect behavior

## Backend Alignment Notes

- Frontend routes and API calls are mapped to current backend controller contracts.
- Profile uses experienceYears field naming aligned with backend DTO.
- Jobs update/create use experienceRequired aligned with backend DTO.
- Company payload excludes industry because backend DTO does not currently expose it.
- Application create payload sends jobId (no coverLetter in current backend DTO).
- Local verification scripts run through gateway and are kept aligned with secured responses:
	- `scripts/synthetic-checks.ps1`
	- `scripts/journey-checks.ps1`

## Context Update Log

Use this section to keep context current after each significant change.

2026-07-25:

- Created frontend app with full module coverage for auth, users, jobs, companies, applications, notifications.
- Hardened private access model and role-aware navigation.
- Added endpoint mapping expansions and DTO-aligned payload fixes.
- Added global toasts, request-state hook, skeletons, and empty states.
- Added candidate/employer/admin login tracks.
- Added user-not-found registration assist (candidate/employer).
- Enforced backend self-registration policy to block ADMIN and allow JOB_SEEKER/RECRUITER only.
- Added admin-only monitoring dashboard with service health, runtime KPIs, widget customization, and snapshot export.
- Added backend-aggregated monitoring summary integration, metric threshold badges, compact/expanded layouts, and persistent card pinning/reordering.
- Added explicit unauthorized screen with role/reason messaging for restricted routes.
- Added frontend unit-test setup (Vitest) with CI test+build execution.
- Added local synthetic checks and startup scripts support for localhost reliability workflows.
- Added eslint baseline tuning for stable CI lint execution while preserving warning visibility.
- Added CI integration smoke and artifact publishing support through GitHub Actions workflow.
- Enhanced notification UX with sorting, status/type filters, bulk mark-read, and optimistic updates.
- Added Zod schema-based auth form validation with inline field errors.
- Added Playwright E2E auth smoke coverage and Vitest/Playwright test-scope separation.
- Added React Query query/mutation integration for notifications with cache invalidation and optimistic updates.
- Added accessibility audit plan at frontend/ACCESSIBILITY_AUDIT.md to drive WCAG-focused remediation.

## How To Keep This File Updated

After each feature or API contract change, update:

1. Functional module section impacted.
2. Endpoint mapping list impacted.
3. Backend alignment notes if DTO/request/response changed.
4. Context Update Log with date and short bullet summary.
