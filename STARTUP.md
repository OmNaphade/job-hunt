# Job Portal — Local Startup Guide

## Prerequisites

- Java 21+ (project targets Java 21; runs on Java 25)
- PostgreSQL 14+ running on `localhost:5432`
- Maven (or use `mvnw.cmd` wrapper)
- *(Optional)* Kafka for notification events

## 1. Database Setup

Create the database (if not using Docker):
```sql
-- Connect as postgres user
CREATE DATABASE jobapp_db;
-- OR run the init script for per-service databases:
psql -U postgres -f init-db.sql
```

Default credentials used by all services:
- Host: `localhost:5432`
- Database: `jobapp_db`
- Username: `postgres`
- Password: `manager`

## 2. Build All Services

From each service directory, run:
```powershell
cd auth_service
.\mvnw.cmd package -DskipTests
```

Or build all at once (PowerShell):
```powershell
$services = @("auth_service","user_service","job_service","company_service","application_service","notification_service")
foreach ($s in $services) {
    cd "d:\PRACTICE\New folder\$s"
    .\mvnw.cmd package -DskipTests
}
```

## 3. Start Services

Start each service with the `dev` profile (disables Vault and Eureka):
```powershell
java -jar auth_service\target\auth_service-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
java -jar user_service\target\user_service-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
java -jar job_service\target\job_service-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
java -jar company_service\target\company_service-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
java -jar application_service\target\application_service-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
java -jar notification_service\target\notification_service-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

## 4. Service Ports

| Service | Port |
|---|---|
| auth_service | 8081 |
| user_service | 8082 |
| job_service | 8083 |
| company_service | 8084 |
| application_service | 8085 |
| notification_service | 8086 |
| service_registry (Eureka) | 8761 |
| config_server | 8888 |
| api_gateway | 8080 |

## 5. Verify All Services Are Up

```powershell
@(8081,8082,8083,8084,8085,8086) | ForEach-Object {
    $h = Invoke-RestMethod "http://localhost:$_/actuator/health"
    Write-Host ":$_ => $($h.status)"
}
```

## 6. Environment Variables (Optional)

| Variable | Default | Description |
|---|---|---|
| `JWT_SECRET` | `default-dev-secret-key-change-in-production-32chars` | JWT signing secret |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173,http://localhost:4173,http://127.0.0.1:4173` | API Gateway allowed frontend origins (comma-separated) |
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `jobapp_db` | Database name |
| `DB_USER` | `postgres` | Database username |
| `DB_PASSWORD` | `manager` | Database password |

Set via `.env.example` (copy to `.env`) or system environment.

## 7. Docker Compose (Full Stack)

To run everything including Kafka, Zookeeper, and all services:
```bash
docker-compose up -d
```

This starts: PostgreSQL, Zookeeper, Kafka, all 9 Spring services.

## 8. Infrastructure Services (Optional for dev)

### Eureka Service Registry
```powershell
cd service_registry
.\mvnw.cmd spring-boot:run
# Available at http://localhost:8761
```

### Config Server
```powershell
cd config_server
.\mvnw.cmd spring-boot:run
# Available at http://localhost:8888
```

### API Gateway
```powershell
cd api_gateway
.\mvnw.cmd spring-boot:run
# Proxy at http://localhost:8080 — routes to all business services
```

## 9. Quick Test

Register and login:
```powershell
# Register
$r = Invoke-RestMethod "http://localhost:8081/api/auth/register" -Method POST `
  -ContentType "application/json" `
  -Body '{"email":"test@example.com","password":"Pass123!","role":"JOB_SEEKER","firstName":"Test","lastName":"User"}'

# Login
$login = Invoke-RestMethod "http://localhost:8081/api/auth/login" -Method POST `
  -ContentType "application/json" `
  -Body '{"email":"test@example.com","password":"Pass123!"}'

$token = $login.accessToken
$h = @{ Authorization = "Bearer $token" }

# Get profile
Invoke-RestMethod "http://localhost:8081/api/auth/me" -Headers $h

```

## 10) Local Operations Scripts

PowerShell scripts are available in the scripts folder for localhost productivity:

- `./scripts/validate-env.ps1` - validate required tooling and core env variables
- `./scripts/start-local.ps1` - one-command bootstrap for compose plus frontend checks/dev
- `./scripts/synthetic-checks.ps1` - quick synthetic checks through gateway routes
- `./scripts/seed-demo-data.ps1` - apply reproducible demo seed data to local database
- `./scripts/logs-dashboard-lite.ps1` - quick health+logs view for local observability
- `./scripts/backup-db.ps1` - create a timestamped logical backup dump
- `./scripts/restore-db.ps1 -BackupFile <path>` - restore from selected backup dump
- `./scripts/journey-checks.ps1` - run candidate/employer/admin journey smoke checks
- `./scripts/reset-local-state.ps1` - clean local reset using compose volume recreation
- `./scripts/simulate-failure.ps1` - stop/start selected dependencies for failure drills

See `API_DOCS.md` for full endpoint reference.
