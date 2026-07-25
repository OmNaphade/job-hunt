# API Governance and Contract Policy

Last updated: 2026-07-25

## Versioning Strategy

- External-facing contracts should use `/api/v1/...`.
- Existing `/api/...` endpoints remain as compatibility aliases until migration is complete.
- Breaking changes require new major version path (`/api/v2/...`).

## Error Standard

All services should return a normalized error payload:

```json
{
  "timestamp": "2026-07-25T12:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "path": "/api/v1/jobs",
  "traceId": "optional-trace-id"
}
```

## Pagination and Sorting

List endpoints should support:

- `page` (default 0)
- `size` (default 20, max 100)
- `sort` (for example `createdAt,desc`)

## Idempotency

Critical POST endpoints should support `Idempotency-Key` header:

- POST `/api/v1/applications`
- POST `/api/v1/notifications`

Behavior:

- Repeated request with same key and same payload returns previously created response.
- Repeated request with same key and different payload returns 409 conflict.

## Contract Tests

- Add provider-side contract verification in CI for gateway-exposed APIs.
- Add consumer-side tests for frontend assumptions (auth, jobs, notifications).

## Deprecation Policy

- Mark endpoints deprecated in OpenAPI before removal.
- Minimum deprecation window: 60 days for non-critical contracts.
