# Branch Protection Configuration

Last updated: 2026-07-25

## Objective

Enforce required checks, review gates, and status checks before merge to protected branches.

## Target Branches

- main
- develop

## Required Settings

1. Require a pull request before merging.
2. Require approvals:
   - main: minimum 2 approvals
   - develop: minimum 1 approval
3. Dismiss stale approvals when new commits are pushed.
4. Require status checks to pass before merge:
   - frontend-build-test
   - build-and-test
   - build-infra
   - security-baseline
   - ci-contract-and-lint
   - ci-integration-smoke
5. Require branches to be up to date before merging.
6. Restrict force pushes and branch deletion.

## Manual Steps (GitHub UI)

1. Open repository settings.
2. Navigate to Branches -> Branch protection rules.
3. Add rule for main and develop.
4. Select the required checks listed above.
5. Save and verify with a test PR.

## Verification Checklist

- Merge button blocked when checks fail.
- Merge button blocked without required approvals.
- Direct push to protected branch denied.
