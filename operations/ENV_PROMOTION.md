# Environment Promotion Workflow

Last updated: 2026-07-25

## Objective

Define immutable artifact promotion from dev -> stage -> prod with auditable approvals.

## Promotion Model

1. Build once in CI from commit SHA.
2. Publish immutable artifacts:
   - service jars
   - docker images tagged with SHA and semantic version
3. Promote same artifact across environments. No rebuilds during promotion.

## Stages

- Dev: automatic deployment from main/develop after checks pass.
- Stage: manual approval gate + smoke tests.
- Prod: two-step approval (tech lead + ops owner), rollout plan required.

## Required Gates

- Frontend lint/test/build
- Backend matrix build/test
- Integration smoke
- Security baseline checks
- Artifact publish complete

## Rollback

- Roll back by redeploying previous known-good SHA tag.
- Keep at least last 5 release image tags retained.

## Release Metadata

Every promotion must include:

- commit SHA
- release tag
- changelog summary
- DB migration status
- rollback command snippet

## GitHub Actions Mapping

- Current workflow: .github/workflows/ci.yml
- Add environment-specific deploy workflows referencing this policy.
