#!/bin/bash
# Single-attempt cost check, meant to be invoked repeatedly by a scheduled
# GitHub Actions workflow (.github/workflows/oci-cost-killswitch.yml) — same
# pattern as scripts/oci-provision-retry.sh, just checking spend instead of
# capacity. OCI Budgets can only email an alert; it can't stop anything on its
# own, so this script is what actually enforces the free-tier ceiling.
#
# Exit code convention (matches oci-provision-retry.sh):
#   0 = nothing unexpected happened (still under budget, or the trip was
#       handled cleanly) — the workflow should not treat this as a failure.
#   1 = a real, unexpected error (bad OCID, auth failure, etc.) that a human
#       should look at, surfaced as a failed Actions run.
set -uo pipefail

COMPARTMENT_ID="${OCI_COMPARTMENT_ID:?}"
NOTIFICATION_TOPIC_ID="${OCI_NOTIFICATION_TOPIC_ID:?}"
COST_LIMIT_USD="${OCI_COST_LIMIT_USD:-1.00}"
DRY_RUN="${DRY_RUN:-false}"

INSTANCE_NAMES=("job-portal-core" "job-portal-edge" "job-portal-aux")

# Month-to-date window, UTC.
TIME_STARTED="$(date -u +%Y-%m-01T00:00:00Z)"
TIME_ENDED="$(date -u -d '+1 day' +%Y-%m-%dT00:00:00Z)"

cost=$(oci usage-api request-summarized-usages \
  --tenant-id "$COMPARTMENT_ID" \
  --time-usage-started "$TIME_STARTED" \
  --time-usage-ended "$TIME_ENDED" \
  --granularity MONTHLY \
  --query "sum(data.items[].\"computed-amount\")" \
  --raw-output 2>&1)
status=$?

if [ "$status" -ne 0 ]; then
  echo "UNEXPECTED ERROR querying usage (needs a human to look at it):"
  echo "$cost"
  exit 1
fi

# An empty/null sum (no usage records yet this month) means $0, not an error.
if [ "$cost" = "null" ] || [ -z "$cost" ]; then
  cost="0"
fi

echo "Month-to-date cost: \$$cost (limit: \$$COST_LIMIT_USD)"

over_limit=$(awk -v a="$cost" -v b="$COST_LIMIT_USD" 'BEGIN{print (a > b) ? "true" : "false"}')

if [ "$over_limit" != "true" ]; then
  echo "Within budget — nothing to do."
  echo "triggered=false" >> "$GITHUB_OUTPUT"
  exit 0
fi

echo "OVER BUDGET — stopping all instances and notifying."

for name in "${INSTANCE_NAMES[@]}"; do
  instance_id=$(oci compute instance list \
    --compartment-id "$COMPARTMENT_ID" \
    --display-name "$name" \
    --query "data[?\"lifecycle-state\" != 'TERMINATED'] | [0].id" \
    --raw-output)

  if [ -z "$instance_id" ] || [ "$instance_id" = "null" ]; then
    echo "  $name: not found — skipping."
    continue
  fi

  if [ "$DRY_RUN" = "true" ]; then
    echo "  $name ($instance_id): DRY_RUN — would stop this instance."
    continue
  fi

  echo "  $name ($instance_id): stopping."
  oci compute instance action --instance-id "$instance_id" --action STOP >/dev/null
done

message="RESOURCES EXCEED FREE LIMIT — all instances stopped automatically at $(date -u +%Y-%m-%dT%H:%M:%SZ). Month-to-date cost was \$$cost against a \$$COST_LIMIT_USD limit."

if [ "$DRY_RUN" = "true" ]; then
  echo "DRY_RUN — would publish notification: $message"
else
  oci ons message publish \
    --topic-id "$NOTIFICATION_TOPIC_ID" \
    --title "resources exceed free limit" \
    --body "$message" >/dev/null
fi

echo "triggered=true" >> "$GITHUB_OUTPUT"
echo "cost=$cost" >> "$GITHUB_OUTPUT"
exit 0
