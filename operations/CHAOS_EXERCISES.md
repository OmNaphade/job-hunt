# Chaos Exercise Plan

Last updated: 2026-07-25

## Objective

Run controlled failure drills and verify graceful degradation and recovery.

## Scenarios

1. Kafka outage:
   - `./scripts/simulate-failure.ps1 -Target kafka -Action down`
   - Observe notification lag and recovery.
2. PostgreSQL outage:
   - `./scripts/simulate-failure.ps1 -Target postgres -Action down`
   - Verify service failure modes and restart sequencing.
3. API gateway outage:
   - `./scripts/simulate-failure.ps1 -Target gateway -Action down`
   - Verify synthetic checks fail and recover after restart.

## Validation

- Run `./scripts/synthetic-checks.ps1` before and after each drill.
- Capture logs and incident notes.

## Exit Criteria

- Recovery steps documented.
- Alert triggers observed and acknowledged.
- Follow-up backlog created for any gaps.
