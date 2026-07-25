# Zero-Downtime Delivery Strategy

Last updated: 2026-07-25

## Objective

Define rolling deployment and database migration safety for backward-compatible releases.

## Service Deployment Strategy

- Deploy one instance at a time (rolling update).
- Keep N-1 and N versions API compatible during transition.
- Health probe must pass before routing traffic to new instance.

## Compatibility Rules

- Do not remove request fields in same release introducing replacement.
- Additive response fields only for minor releases.
- Gateway routes should support both old and new endpoints during migration windows.

## Database Migration Rules

- Expand/contract model:
  1. Expand schema (add nullable columns/tables/indexes)
  2. Deploy app version using both old/new schema
  3. Backfill data
  4. Contract schema in later release
- Never combine destructive migration with first app rollout.

## Runtime Guardrails

- Circuit breaker fallback active for inter-service calls.
- Deployment abort conditions:
  - 5xx error rate > 3% for 10m
  - p95 latency > 1.5x SLO for 15m

## Rollback Procedure

1. Stop current rollout.
2. Redeploy previous image tag.
3. Verify health + synthetic checks.
4. If migration included, execute approved rollback/forward-fix SQL.
