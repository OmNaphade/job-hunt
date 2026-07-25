# Performance and Scaling Strategy

Last updated: 2026-07-25

## Scope

This document advances remaining scalability TODOs with concrete implementation direction.

## 1) Cache Strategy (Redis)

Target read-heavy endpoints:

- GET /api/jobs
- GET /api/jobs/search
- GET /api/companies
- GET /api/users/skills

Approach:

- Introduce Redis as shared cache.
- Use cache-aside pattern in services.
- TTL guidance:
  - jobs list/search: 60s
  - companies list: 120s
  - skills catalog: 300s

Invalidation triggers:

- job create/update/delete/status change
- company create/update/delete
- skill catalog create

## 2) Kafka Throughput Tuning

- Increase partitions for application-events and job-events based on consumer lag profile.
- Configure dedicated consumer group concurrency for notification_service.
- Add retry and DLQ policies per topic.
- Track lag and processing latency in Grafana.

## 3) Pool Sizing Guidelines

- DB pool per service:
  - baseline minIdle=5
  - baseline maxPoolSize=20
  - tune by p95 DB wait time and CPU
- Web thread pool:
  - align max threads to CPU cores and request profile
- Kafka consumer concurrency:
  - start at 2-4 per service, tune by lag and processing time

## 4) Validation Plan

1. Run k6 scenarios from performance/k6.
2. Compare p95 latency and error rate before/after changes.
3. Observe DB and Kafka dashboards for saturation and lag.
4. Record findings and tune values iteratively.
