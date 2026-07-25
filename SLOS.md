# Job Portal SLO and SLI Definitions

Last updated: 2026-07-25

## Objective

Define practical service-level objectives for localhost-first operations and early production readiness.

## Measurement Window

- Default reporting window: rolling 30 days
- Local validation window: rolling 24 hours

## Global SLO Baselines

- Availability SLO (core APIs): 99.5%
- P95 latency SLO (core APIs): <= 500 ms
- Error-rate SLO (5xx responses): <= 1.0%

## Service SLO Matrix

### api_gateway

- Availability SLO: 99.7%
- P95 latency SLO: <= 250 ms (gateway processing only)
- Error-rate SLO: <= 0.8%
- Primary SLIs:
  - Availability: successful health checks / total checks
  - Latency: p95 of `http_server_requests_seconds`
  - Errors: ratio of 5xx responses to total responses

### auth_service

- Availability SLO: 99.5%
- P95 latency SLO:
  - login: <= 400 ms
  - refresh: <= 250 ms
- Error-rate SLO: <= 1.0%
- Primary SLIs:
  - Login success rate
  - Token refresh success rate
  - Auth endpoint p95 latency

### user_service

- Availability SLO: 99.3%
- P95 latency SLO: <= 450 ms
- Error-rate SLO: <= 1.2%
- Primary SLIs:
  - Profile read/update success ratio
  - Skill operations success ratio

### job_service

- Availability SLO: 99.5%
- P95 latency SLO:
  - list/search: <= 450 ms
  - write/update: <= 600 ms
- Error-rate SLO: <= 1.2%
- Primary SLIs:
  - Search latency p95
  - Job write success ratio

### company_service

- Availability SLO: 99.3%
- P95 latency SLO: <= 500 ms
- Error-rate SLO: <= 1.2%
- Primary SLIs:
  - Company CRUD success ratio
  - Recruiter mapping operation latency p95

### application_service

- Availability SLO: 99.4%
- P95 latency SLO:
  - apply/withdraw: <= 550 ms
  - status update: <= 600 ms
- Error-rate SLO: <= 1.1%
- Primary SLIs:
  - Application transition success ratio
  - Apply request p95 latency

### notification_service

- Availability SLO: 99.2%
- P95 latency SLO:
  - read APIs: <= 400 ms
  - write/read transition: <= 500 ms
- Error-rate SLO: <= 1.5%
- Primary SLIs:
  - Notification retrieval success ratio
  - Unread count latency p95

## User Journey SLOs

### Candidate Apply Flow

- Journey: login -> job search -> apply -> notification fetch
- Completion SLO: 97% successful completion rate
- Journey latency SLO: <= 3.0 s p95 (excluding human think time)

### Recruiter Hiring Flow

- Journey: login -> company context -> create job -> review applications -> update status
- Completion SLO: 97% successful completion rate
- Journey latency SLO: <= 4.0 s p95 (excluding human think time)

### Admin Operations Flow

- Journey: login -> monitoring summary load -> user lookup -> password update/delete
- Completion SLO: 98% successful completion rate
- Journey latency SLO: <= 3.0 s p95

## Error Budget Policy

- Monthly error budget by availability SLO:
  - 99.7% => 2h 9m budget
  - 99.5% => 3h 39m budget
  - 99.3% => 5h 2m budget
  - 99.2% => 5h 50m budget
- Policy:
  - At 50% budget consumed: freeze non-critical changes for affected service.
  - At 75% consumed: require rollback plan for all deployments.
  - At 100% consumed: freeze feature work for affected service and prioritize reliability fixes.

## Alert Thresholds (Initial)

- Critical: service availability < 99.0% over last 1h
- Critical: endpoint p95 latency > SLO target x 1.5 for 15m
- Critical: 5xx error rate > 3% for 10m
- Warning: 5xx error rate > 1.5% for 15m
- Warning: CPU > 80% for 15m or heap > 85% for 15m

## Data Sources

- Spring Actuator health and metrics
- Prometheus metric exposition (`/actuator/prometheus`)
- Gateway aggregated monitoring endpoint (`/api/monitoring/summary`) for operational UI
- CI test pass/fail reports for release gates

## Review Cadence

- Weekly: monitor SLI trend and tune thresholds
- Bi-weekly: adjust service SLO targets based on load and incidents
- Monthly: review error budget burn and finalize reliability backlog priorities
