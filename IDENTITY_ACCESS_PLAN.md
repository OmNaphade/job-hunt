# Identity and Access Implementation Plan

Last updated: 2026-07-25

## Phase 13 Scope

- admin invite flow
- email verification
- password reset
- login rate limits
- account lockout
- session management

## 1) Admin Invite Flow

- New table: recruiter_invites(id, email, invited_by, token, expires_at, status)
- Endpoint: POST /api/auth/invites (ADMIN)
- Endpoint: POST /api/auth/invites/accept
- Restrict RECRUITER self-registration when production flag is enabled.

## 2) Email Verification

- New table: email_verification_tokens(user_id, token, expires_at, used)
- Register flow sets user as unverified.
- Endpoint: POST /api/auth/verify-email
- Block login for unverified users (except local/dev bypass flag).

## 3) Password Reset

- New table: password_reset_tokens(user_id, token, expires_at, used)
- Endpoint: POST /api/auth/forgot-password
- Endpoint: POST /api/auth/reset-password

## 4) Login Rate Limits

- Gateway-level limit by IP + email key.
- Auth-level limit by user/email identity.
- Return 429 with retry hint.

## 5) Account Lockout

- Fields on user: failed_attempts, locked_until
- Lock after configurable failed attempts.
- Admin unlock endpoint.

## 6) Session Management

- Expose active refresh sessions by user.
- Endpoint: GET /api/auth/sessions
- Endpoint: DELETE /api/auth/sessions/{sessionId}
