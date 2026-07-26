# Job Portal Platform

Production-style job portal platform with microservices backend and role-based frontend, built for hiring workflows across candidates, recruiters, and admins.

[![Build Status](https://github.com/OmNaphade/job-portal/actions/workflows/ci.yml/badge.svg)](https://github.com/OmNaphade/job-portal/actions/workflows/ci.yml)
[![Version](https://img.shields.io/badge/version-1.0.0-informational.svg)](https://github.com/OmNaphade/job-portal)
[![Runtime](https://img.shields.io/badge/java-21-orange.svg)](https://www.oracle.com/java/)

## Table of Contents

- [About](#about)
- [Built With](#built-with)
- [Features](#features)
- [Demo](#demo)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Configuration](#configuration)
- [Usage](#usage)
- [Project Structure](#project-structure)
- [Testing](#testing)
- [Deployment](#deployment)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [Security](#security)
- [License](#license)
- [Acknowledgements](#acknowledgements)
- [Contact](#contact)

## About

This project provides a complete hiring lifecycle system: authentication, profiles, jobs, companies, applications, and notifications behind an API Gateway. It is designed for teams that want a modular microservices backend with a modern React frontend and Docker-first local setup.

## Built With

- Java 21, Spring Boot 3.5.11, Spring Cloud 2025.0.1
- React + Vite + Tailwind CSS
- PostgreSQL, Kafka, Zookeeper
- Docker Compose, GitHub Actions

## Features

- Role-based auth (JOB_SEEKER, RECRUITER, ADMIN) with JWT
- Microservices architecture with gateway, config server, and service registry
- End-to-end flows for candidate applications and recruiter job management
- Admin monitoring summary endpoint via API gateway
- CI pipeline for backend/frontend builds and integration smoke checks

## Demo

Local demo via Docker Compose:

```bash
docker compose up -d --build
```

Then open:

- API Gateway: http://localhost:8080
- Eureka: http://localhost:8761

## Getting Started

### Prerequisites

- Java 21+
- Node.js 22+
- Docker + Docker Compose
- PowerShell (for provided scripts)

### Installation

```bash
git clone https://github.com/OmNaphade/job-portal.git
cd job-portal
```

For frontend local development:

```bash
cd frontend
npm install
```

### Configuration

```bash
cp .env.example .env
```

Key variables:

| Variable | Description | Default |
|---|---|---|
| `JWT_SECRET` | JWT signing secret for services | `default-dev-secret-key-change-in-production-32chars` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated frontend origins allowed by API Gateway CORS | `http://localhost:5173,http://127.0.0.1:5173,http://localhost:4173,http://127.0.0.1:4173` |
| `ADMIN_SEED_ENABLED` | Enable deterministic admin seed in auth service | `true` (compose) |
| `ADMIN_SEED_EMAIL` | Seeded admin email | `admin@jobportal.local` |
| `ADMIN_SEED_PASSWORD` | Seeded admin password | `Pass123!` |

## Usage

Start full local stack:

```bash
docker compose up -d --build
```

Run validation scripts:

```powershell
./scripts/synthetic-checks.ps1
./scripts/journey-checks.ps1
```

Start frontend only:

```bash
cd frontend
npm run dev
```

## Project Structure

```text
.
├── api_gateway/
├── auth_service/
├── user_service/
├── job_service/
├── company_service/
├── application_service/
├── notification_service/
├── service_registry/
├── config_server/
├── frontend/
├── scripts/
├── docker-compose.yml
├── API_DOCS.md
├── STARTUP.md
└── README.md
```

## Testing

Backend compile example:

```powershell
cd auth_service
.\mvnw.cmd clean compile
```

Frontend tests:

```bash
cd frontend
npm test
```

Integration smoke (local):

```powershell
./scripts/synthetic-checks.ps1
./scripts/journey-checks.ps1
```

## Deployment

Containerized deployment is supported via Dockerfiles per service and root compose orchestration.

```bash
docker compose up -d --build
```

For production, externalize secrets and service URLs via environment variables and use managed Postgres/Kafka infrastructure.

## Roadmap

- [ ] Expand integration test coverage for failure scenarios
- [ ] Add production-grade observability dashboards and alerts
- [ ] Add release versioning and changelog automation

## Contributing

Contributions are welcome.

1. Fork the repository
2. Create a branch (`git checkout -b feature/your-change`)
3. Commit (`git commit -m "feat: add your change"`)
4. Push (`git push origin feature/your-change`)
5. Open a Pull Request

## Security

Do not open public issues for sensitive vulnerabilities. Please contact the maintainer directly and include reproduction steps and impact.

## License

License file is not currently included in the repository. Add a `LICENSE` file before publishing formal redistribution terms.

## Acknowledgements

- Spring Boot and Spring Cloud ecosystem
- React, Vite, and Tailwind maintainers
- PostgreSQL and Kafka open-source communities

## Contact

Maintainer:
**Om Naphade** · [LinkedIn](https://linkedin.com/in/omnaphade) · [Portfolio](https://om-naphade.netlify.app) · [GitHub](https://github.com/OmNaphade)
