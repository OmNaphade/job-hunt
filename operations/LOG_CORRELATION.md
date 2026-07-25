# Log Correlation Strategy

Last updated: 2026-07-25

## Goal

Propagate and log a stable trace identifier across gateway and all backend services.

## Header Convention

- Primary header: `X-Trace-Id`
- If absent at ingress, gateway should generate one.
- Services must include trace id in structured logs.

## Logging Pattern Requirements

- Include trace id in every request log line.
- Include userId (if authenticated), method, path, status, latency.

## Verification

1. Trigger request through gateway.
2. Confirm same trace id appears in gateway and downstream logs.
3. Confirm trace id is returned in response headers.

## Next Implementation Steps

- Gateway servlet filter implemented: api_gateway TraceIdFilter sets/propagates `X-Trace-Id` and echoes in response header.
- Logging MDC integration standardized in all business services using `traceId` key.
- Optional next hardening: add CI smoke assertion for trace-id header round-trip.
