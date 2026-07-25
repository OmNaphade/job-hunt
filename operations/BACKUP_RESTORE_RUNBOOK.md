# Backup and Restore Runbook

Last updated: 2026-07-25

## Scope

Covers logical backup and restore for local PostgreSQL environment.

## Prerequisites

- PostgreSQL client tools installed (`pg_dump`, `psql`)
- Database credentials available
- Backup directory write access

## Backup

PowerShell helper:

- scripts/backup-db.ps1

Manual command template:

```powershell
pg_dump -h localhost -p 5432 -U postgres -F c -b -v -f backup.dump jobapp_db
```

## Restore

PowerShell helper:

- scripts/restore-db.ps1

Manual command template:

```powershell
psql -h localhost -p 5432 -U postgres -d jobapp_db -f backup.sql
```

## Validation Checklist

1. Verify users/jobs/applications row counts.
2. Run scripts/synthetic-checks.ps1.
3. Run frontend smoke tests if applicable.

## Recovery Targets

- RPO (local target): <= 24h
- RTO (local target): <= 60m
