# Job Portal Manual Regression Checklist

Last updated: 2026-07-25

## Scope

Use this checklist before merges to main and before release tags.

## 1. Candidate Journey

- Register as JOB_SEEKER when self-register is enabled.
- Login with candidate track and verify redirect to applications view.
- Create/update profile and verify persistence.
- Add/remove skill and verify skill list updates.
- Search jobs with multiple filter combinations.
- Apply to a job and verify application appears in My Applications.
- Withdraw an application and verify status transition.
- Open notifications and verify unread/read transitions.

## 2. Employer Journey

- Login with employer track (RECRUITER).
- Create company, update company, and verify list reflects update.
- Add recruiter mapping and remove recruiter mapping.
- Create job, edit full job payload, change job status.
- Review applications by job and update candidate status.
- Verify domain toasts and request-state feedback on all actions.

## 3. Admin Journey

- Login with admin track.
- Access monitoring dashboard and verify route is accessible.
- Validate monitoring filters, layout mode, pinning, and snapshot export.
- Access auth operational tools: lookup user, update password, delete user.
- Confirm admin-only route is hidden/inaccessible for non-admin users.

## 4. Security and Guards

- Verify unauthenticated access to protected routes redirects to /auth.
- Verify unauthorized role sees explicit Unauthorized screen.
- Validate logout clears session and blocks protected route access.

## 5. API Contract and Data Integrity

- Validate profile payload uses experienceYears.
- Validate jobs create/update payload uses experienceRequired.
- Validate company payload does not rely on unsupported DTO fields.
- Validate application payload uses jobId and passes backend validation.

## 6. Reliability Smoke

- Verify /api/monitoring/summary returns data for admin token.
- Verify gateway routes for all 6 domains respond through /api.
- Verify notification unread count endpoint updates after mark-read.

## 7. Build and Test Gates

- Frontend: npm run test
- Frontend: npm run build
- Backend: compile all services and gateway
- CI workflow: confirm latest run green for frontend and backend jobs

## Signoff

- Executor:
- Date:
- Commit/PR:
- Notes:
