# Performance Toolkit

## k6 Scenarios

- login-search-apply.js
  - candidate login
  - job search
  - apply flow

- notifications-read.js
  - candidate login
  - unread list and unread count checks

## Run Examples

```powershell
k6 run .\performance\k6\login-search-apply.js -e BASE_URL=http://localhost:8080 -e USER_EMAIL=candidate@jobportal.local -e USER_PASSWORD=Pass123!
k6 run .\performance\k6\notifications-read.js -e BASE_URL=http://localhost:8080 -e USER_EMAIL=candidate@jobportal.local -e USER_PASSWORD=Pass123!
```

## Initial Targets

- error rate under 3%
- p95 latency under 700ms for mixed flow
- p95 latency under 500ms for notification reads
