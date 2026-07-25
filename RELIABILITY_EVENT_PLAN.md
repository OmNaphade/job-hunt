# Reliability and Event Consistency Plan

Last updated: 2026-07-25

## Phase 15 Scope

- outbox pattern
- idempotent consumers
- DLQ and retries
- schema versioning
- saga compensation

## 1) Outbox Pattern

- Add outbox table in job_service and application_service.
- Persist domain write + outbox message in same transaction.
- Background publisher dispatches outbox records to Kafka.

## 2) Idempotent Consumers

- Add processed_event table keyed by eventId + consumerGroup.
- Skip duplicate events.

## 3) DLQ and Retry

- Main topics + retry topics + dead-letter topics.
- Exponential backoff retry policy.

## 4) Schema Versioning

- Include schemaVersion in event envelope.
- Maintain backward-compatible evolution policy.

## 5) Saga Compensation

- Define compensating transitions for cross-service failures.
- Track saga state and compensating action results.
