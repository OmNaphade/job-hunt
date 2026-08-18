# Environments — Dev, UAT, Prod

This repo runs three environments off **one codebase with three long-lived branches, each
paired with a matching Spring/Vite profile** — `develop` for day-to-day development, `uat`
for pre-release testing, `main` for production. Config (profiles + env files) differentiates
the environments; the branch tells you which stage code is currently at.

| Tier | Branch | Runs on | Spring profile | Frontend mode | DB name | Purpose |
|---|---|---|---|---|---|---|
| **dev** | `develop` | Your laptop | `dev` | `development` | `jobapp_dev_db` | Day-to-day coding, integration of feature branches |
| **uat** | `uat` | A dedicated VM/host (not the prod box) | `uat` | `uat` | `jobapp_uat_db` | Pre-release testing with real infra (Eureka, Kafka, etc.) |
| **prod** | `main` | Your existing EC2/OCI instance | `prod` | `production` | `jobapp_db` (unchanged) | Live traffic — **only branch that auto-deploys** |

**Nothing here changes your currently-running prod host unless you opt in.** Every new
`${VAR:-default}` added to `docker-compose.yml` defaults to the exact value that was already
hardcoded there. If that host keeps using its existing `.env` file and just does
`git pull && docker compose up -d --build`, behavior is identical to before. The new pieces
(`SPRING_PROFILES_ACTIVE`, `env/*.env` files, per-profile `.properties`) are additive.

## 1. Branching

- **`develop`** = dev. Feature branches merge here via PR. Runs the `dev` profile. Never
  auto-deploys — CI only builds and tests it.
- **`uat`** = pre-release testing. Promote `develop` → `uat` (via PR or fast-forward merge)
  when you want a build tested against real infra. Runs the `uat` profile. Pushing to `uat`
  triggers the (currently stubbed) `deploy-uat` CI job — the only branch besides `main` that
  triggers a deploy job at all, and even then only once you point it at a real UAT host.
- **`main`** = production, deploy-ready. Promote `uat` → `main` for a release. Runs the `prod`
  profile. **This is the only branch that's meant to actually go live** — `deploy-prod` runs
  automatically on every push to `main`.

Promotion flow: `feature/*` → PR into `develop` → PR/merge `develop` → `uat` → test → PR/merge
`uat` → `main` → release.

You don't strictly need these branches to use the dev/uat/prod *profiles* — those work
standalone via `--spring.profiles.active` or `SPRING_PROFILES_ACTIVE` regardless of branch.
The branch flow is the recommended promotion path once you have somewhere to run UAT.

## 2. Backend: Spring profiles

Every business service (`auth_service`, `user_service`, `job_service`, `company_service`,
`application_service`, `notification_service`) plus `api_gateway` now has:

```
src/main/resources/
  application.properties        # shared base (localhost defaults, used when no profile matches)
  application-dev.properties    # local, standalone run — disables Vault + Eureka
  application-uat.properties    # isolated DB name, DEBUG logging
  application-prod.properties   # unchanged DB name, WARN/INFO logging
```

`auth_service` additionally controls the admin auto-seed per profile (`admin.seed.enabled`):
**on** for dev/uat (convenient known login), **off** by default for prod (see §5, Security).

`service_registry` and `config_server` don't get per-environment files — they're identical
infra in every environment (Eureka registry, and an unused native config server nothing
currently consumes).

**Running a service directly with a profile:**
```powershell
java -jar auth_service\target\auth_service-0.0.1-SNAPSHOT.jar --spring.profiles.active=uat
```
Or in Docker, via `SPRING_PROFILES_ACTIVE` (see §4).

## 3. Frontend: Vite modes

Vite natively loads `.env.[mode]` files. Three now exist in `frontend/`:

- `.env.development` — used by `npm run dev` (default mode `development`)
- `.env.uat` — used by `npm run build -- --mode uat`
- `.env.production` — used by `npm run build` (default mode `production`)

These only matter for a **non-Docker** frontend build/serve. The Docker build path (see below)
gets its `VITE_*` values from build args instead, which Vite treats as already-set and won't
override with the `.env.[mode]` file.

## 4. Docker Compose: one file, three env files

`docker-compose.yml` stays a single file. Environment differences now come from
`env/dev.env`, `env/uat.env`, `env/prod.env` — copy the matching `.example` file and fill in
real secrets. **These are gitignored; never commit them.**

```powershell
# Dev, on your laptop, isolated project name so it won't collide with anything else:
docker compose --env-file env/dev.env -p jobportal-dev up -d --build

# UAT, on its own host:
docker compose --env-file env/uat.env up -d --build

# Prod, on its own host:
docker compose --env-file env/prod.env up -d --build
```

**Important:** every service in `docker-compose.yml` has a fixed `container_name`
(`auth-service`, `jobportal-postgres`, etc.), so you **cannot run two of these stacks on the
same Docker host at once** — that's why UAT needs its own VM/instance, separate from prod.
This mirrors what you're already doing across the OCI (3-VM) and AWS (single-VM) guides: pick
a host per environment, not per stack-on-one-host.

If your existing prod host doesn't create an `env/prod.env` and keeps its current `.env`
file, nothing changes — same defaults, same file it already uses.

## 5. Security notes

- **Admin auto-seed**: `docker-compose.yml`'s default (`ADMIN_SEED_ENABLED=true`) is
  unchanged, so your live prod host isn't affected. But `env/prod.env.example` recommends
  setting it to `false` once you've logged in and created/rotated the real admin account —
  a hardcoded `admin@jobportal.local` / seed password sitting enabled in real prod is worth
  closing off. This is safe to flip any time: seeding is idempotent (skipped if the account
  already exists), so disabling it won't touch an already-seeded database.
- **Secrets**: `env/*.env` (real, filled-in files) are gitignored — only the `.example`
  templates are committed. Use a distinct `JWT_SECRET` per environment (`openssl rand -hex 32`
  or similar) — don't reuse dev's or prod's for UAT.
- **`.env` at repo root**: still your existing local/prod secrets file for the un-split
  compose flow; untouched by this change, still gitignored, still never committed.

## 6. CI/CD status

`.github/workflows/ci.yml` builds and tests `main`, `uat`, and `develop` on every push, but
**only `main` auto-deploys** to a real environment:

- Push to `develop` → build + test only. No deploy job runs at all.
- Push to `uat` → build + test, then `deploy-uat` runs (or trigger it manually via
  `workflow_dispatch` from the Actions tab without pushing).
- Push to `main` → build + test, then `deploy-prod` runs automatically.

Both `deploy-uat` and `deploy-prod` currently call **stub** scripts (no real server configured
yet — they print a message and exit 0 so the pipeline stays green). Renamed for consistency
with this change:

- `scripts/deploy-staging.{sh,ps1}` → `scripts/deploy-uat.{sh,ps1}`
- CI job `deploy-staging` → `deploy-uat`, `deploy-production` → `deploy-prod`
- GitHub secrets, when you're ready to activate: `UAT_DEPLOY_KEY`/`UAT_HOST`/`UAT_USER`,
  `PROD_DEPLOY_KEY`/`PROD_HOST`/`PROD_USER` (see `DEPLOYMENT_SCRIPTS.md` for full activation
  steps — the "REAL DEPLOYMENT" block just needs uncommenting once a server exists).

## 7. Relationship to the OCI/AWS guides

`OCI_DEPLOYMENT_GUIDE.md` and `AWS_DEPLOYMENT_GUIDE.md` describe **where prod's infrastructure
lives** (3-VM Always Free split vs. single paid EC2). This document describes **how the app
differentiates dev/uat/prod once it's running** on any of those hosts. They're complementary:
pick your hosting guide for prod, add a smaller/cheaper second host for UAT (a second OCI
Always Free VM is the natural zero-cost choice if you're already on OCI), and use `env/uat.env`
+ `SPRING_PROFILES_ACTIVE=uat` there instead of `prod`.
