# On-Call Runbook

Last updated: 2026-07-25

## Incident Levels

- P1: complete outage / security incident
- P2: major feature unavailable
- P3: degraded but functional

## Initial Triage (First 10 Minutes)

1. Confirm alert details and impacted service.
2. Check gateway and service health endpoints.
3. Review recent deployments and CI results.
4. Identify blast radius (candidate/employer/admin flows).

## Core Commands

```powershell
./scripts/logs-dashboard-lite.ps1
./scripts/synthetic-checks.ps1
docker compose logs -f api-gateway
docker compose logs -f auth-service user-service job-service company-service application-service notification-service
```

## Mitigation Playbook

- Gateway/auth issue: rollback latest image and verify login flow.
- DB saturation: scale down traffic and inspect connection pool usage.
- Kafka lag: restart consumer, validate broker health, monitor lag trend.

## Escalation Matrix

- Platform owner: gateway/infra incidents
- Service owner: domain service failures
- Integration owner: Kafka/event flow failures

## Incident Closure

1. Confirm service restoration with synthetic checks.
2. Update timeline and root cause summary.
3. Create action items with owner + due date.
4. Add lessons to TODO backlog and docs.
