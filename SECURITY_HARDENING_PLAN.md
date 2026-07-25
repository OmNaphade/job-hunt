# Security Hardening Plan

Last updated: 2026-07-25

## Phase 14 Scope

- JWT key rotation
- secret rotation
- dependency scan hard gates
- security headers
- immutable audit trail
- threat modeling

## 1) JWT Key Rotation

- Add key-id (`kid`) to JWT header.
- Maintain active + previous signing keys.
- Verification checks by `kid` map.

## 2) Secret Rotation

- Move JWT/DB/SMTP credentials to managed secret store.
- Define periodic rotation schedule.
- Add fallback and rollout protocol to avoid downtime.

## 3) Dependency Scan Hard Gates

- Maven and npm checks with explicit severity threshold.
- Break build for high/critical CVEs unless allowlisted with expiry.

## 4) Security Headers

- Implemented at gateway filter level.
- Extend policy verification in integration tests.

## 5) Audit Trail

- Add append-only audit table for auth/admin actions.
- Record actor, action, target, traceId, timestamp.

## 6) Threat Model

- STRIDE matrix per service.
- Top abuse cases and mitigations tracked as backlog items.
