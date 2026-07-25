# Database Architecture

Each microservice owns its own database (database-per-service pattern).

| Service | Database | Port |
|---|---|---|
| auth_service | auth_db | 5432 |
| user_service | user_db | 5432 |
| job_service | job_db | 5432 |
| company_service | company_db | 5432 |
| application_service | application_db | 5432 |
| notification_service | notification_db | 5432 |

## Setup
Run `psql -U postgres -f init-db.sql` to create all databases.

Or when using Docker Compose:
```bash
docker compose up postgres -d
docker exec -i jobportal-postgres psql -U postgres < init-db.sql
```

## Per-service Connection Strings
Update each service's `application.properties` to use its own database when ready to migrate from shared `jobapp_db`:
- auth_service: `jdbc:postgresql://localhost:5432/auth_db`
- user_service: `jdbc:postgresql://localhost:5432/user_db`
- job_service: `jdbc:postgresql://localhost:5432/job_db`
- company_service: `jdbc:postgresql://localhost:5432/company_db`
- application_service: `jdbc:postgresql://localhost:5432/application_db`
- notification_service: `jdbc:postgresql://localhost:5432/notification_db`
