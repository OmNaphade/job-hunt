# Local Operations Scripts

## Available Scripts

- `start-local.ps1`
  - One-command local bootstrap.
  - Starts docker compose and validates frontend by default.
  - Optional `-StartFrontendDev` to run frontend dev server.

- `validate-env.ps1`
  - Validates required local tools and critical env variables.

- `synthetic-checks.ps1`
  - Runs quick synthetic route and guard checks through the gateway.

- `seed-demo-data.ps1`
  - Applies reproducible demo records (users/companies/jobs/applications/notifications).

- `logs-dashboard-lite.ps1`
  - Prints multi-service health summary and quick log-tail commands.

- `backup-db.ps1`
  - Creates PostgreSQL logical backup dump with timestamped filename.

- `restore-db.ps1`
  - Restores a selected PostgreSQL backup dump.

- `journey-checks.ps1`
  - Runs candidate/employer/admin token-based journey smoke checks through gateway.

- `reset-local-state.ps1`
  - Recreates local containers and volumes for clean-state testing.
  - Use `-Force` to skip confirmation.

- `simulate-failure.ps1`
  - Stops/starts a selected dependency or service for resilience testing.

## Typical Flow

1. `./scripts/validate-env.ps1`
2. `./scripts/start-local.ps1`
3. `./scripts/synthetic-checks.ps1`
4. `./scripts/seed-demo-data.ps1`
4. Optional fault test:
   - `./scripts/simulate-failure.ps1 -Target kafka -Action down`
   - `./scripts/simulate-failure.ps1 -Target kafka -Action up`
5. Backup and restore drill:
  - `./scripts/backup-db.ps1`
  - `./scripts/restore-db.ps1 -BackupFile .\\backups\\jobapp_db-<timestamp>.dump`
