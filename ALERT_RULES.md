# Job Portal Alert Rules

Last updated: 2026-07-25

## Purpose

This document defines actionable, low-noise alert rules for local/staging/prod operations.

## Rule Format

- Severity: warning or critical
- Signal: metric expression
- Window: evaluation period
- Action: immediate operator action

## Gateway Alerts

1. Critical - Gateway unavailable
- Signal: `up{job="api_gateway"} == 0`
- Window: 2m
- Action: restart gateway, verify route table and JWT secret loading.

2. Warning - High gateway latency
- Signal: `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{application="api_gateway"}[5m])) by (le)) > 0.25`
- Window: 10m
- Action: inspect downstream saturation and recent deploy changes.

3. Critical - Gateway 5xx surge
- Signal: `(sum(rate(http_server_requests_seconds_count{application="api_gateway",status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count{application="api_gateway"}[5m]))) > 0.03`
- Window: 10m
- Action: rollback latest release or route traffic to known-good version.

## Service Alerts (auth/user/job/company/application/notification)

4. Critical - Service unavailable
- Signal: `up{job=~"auth_service|user_service|job_service|company_service|application_service|notification_service"} == 0`
- Window: 2m
- Action: restart failed service and verify DB/Kafka dependencies.

5. Warning - Error rate elevated
- Signal: `(sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (application) / sum(rate(http_server_requests_seconds_count[5m])) by (application)) > 0.015`
- Window: 15m
- Action: inspect logs/traces and identify failing endpoint.

6. Critical - Error rate severe
- Signal: `(sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (application) / sum(rate(http_server_requests_seconds_count[5m])) by (application)) > 0.03`
- Window: 10m
- Action: trigger incident process and consider rollback.

7. Warning - CPU sustained high
- Signal: `process_cpu_usage > 0.8`
- Window: 15m
- Action: review hot endpoints and thread pool utilization.

8. Warning - Heap pressure high
- Signal: `(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) > 0.85`
- Window: 15m
- Action: inspect memory churn and GC behavior.

9. Critical - DB pool saturation
- Signal: `hikaricp_connections_active / hikaricp_connections_max > 0.9`
- Window: 10m
- Action: reduce load or increase DB pool/resources after validation.

## Kafka Alerts

10. Critical - Consumer lag severe
- Signal: `kafka_consumergroup_lag_sum{consumergroup=~"notification.*"} > 5000`
- Window: 10m
- Action: scale consumers and inspect broker health.

11. Warning - Consumer lag elevated
- Signal: `kafka_consumergroup_lag_sum{consumergroup=~"notification.*"} > 1000`
- Window: 15m
- Action: monitor trend and investigate throughput bottlenecks.

## Alert Routing

- Warning: send to team chat channel.
- Critical: page on-call + open incident ticket.

## Suppression Policy

- Suppress duplicate alerts for same entity/severity for 15 minutes.
- Keep critical alerts unsuppressed if status toggles more than 3 times in 10 minutes.

## Ownership

- Gateway and security alerts: platform owner
- Domain service alerts: service owner
- Kafka alerts: integration owner
