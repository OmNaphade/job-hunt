#!/bin/bash
# Single-attempt OCI instance launch, meant to be invoked repeatedly by a scheduled
# GitHub Actions workflow (.github/workflows/oci-provision-retry.yml) rather than
# looping internally — the schedule itself provides the retry cadence, so each
# invocation here just tries once and reports the outcome via $GITHUB_OUTPUT.
#
# Exit code convention:
#   0 = nothing unexpected happened (already exists, succeeded, or a normal
#       "out of capacity" miss) — the workflow should not treat this as a failure.
#   1 = a real, unexpected error (bad OCID, auth failure, etc.) that a human
#       should look at, surfaced as a failed Actions run.
set -uo pipefail

AD="${OCI_AVAILABILITY_DOMAIN:?}"
COMPARTMENT_ID="${OCI_COMPARTMENT_ID:?}"
IMAGE_ID="${OCI_IMAGE_ID:?}"
SUBNET_ID="${OCI_SUBNET_ID:?}"
SSH_PUBLIC_KEY="${OCI_SSH_PUBLIC_KEY:?}"
OCPUS="${OCI_OCPUS:-2}"
MEMORY_GB="${OCI_MEMORY_GB:-12}"
DISPLAY_NAME="job-portal-prod"

# Idempotency guard: if an instance with this name already exists and isn't
# terminated, there's nothing to do — prevents a stray extra run from launching
# a second instance after a previous run already succeeded.
existing=$(oci compute instance list \
  --compartment-id "$COMPARTMENT_ID" \
  --display-name "$DISPLAY_NAME" \
  --query "data[?\"lifecycle-state\" != 'TERMINATED']" \
  --raw-output)

if [ "$existing" != "[]" ] && [ -n "$existing" ]; then
  echo "Instance already exists — nothing to do."
  echo "already_exists=true" >> "$GITHUB_OUTPUT"
  exit 0
fi

echo "$SSH_PUBLIC_KEY" > /tmp/ssh_key.pub

output=$(oci compute instance launch \
  --availability-domain "$AD" \
  --compartment-id "$COMPARTMENT_ID" \
  --shape "VM.Standard.A1.Flex" \
  --shape-config "{\"ocpus\": $OCPUS, \"memoryInGBs\": $MEMORY_GB}" \
  --image-id "$IMAGE_ID" \
  --subnet-id "$SUBNET_ID" \
  --assign-public-ip true \
  --ssh-authorized-keys-file /tmp/ssh_key.pub \
  --display-name "$DISPLAY_NAME" \
  --wait-for-state RUNNING 2>&1)
status=$?

if [ "$status" -eq 0 ]; then
  public_ip=$(echo "$output" | grep -A1 '"public-ip"' | grep -oE '"[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+"' | tr -d '"')
  echo "SUCCESS: instance is RUNNING at $public_ip"
  echo "success=true" >> "$GITHUB_OUTPUT"
  echo "public_ip=$public_ip" >> "$GITHUB_OUTPUT"
  exit 0
fi

if echo "$output" | grep -qi "Out of capacity"; then
  echo "Out of capacity this attempt — will retry on the next schedule."
  echo "success=false" >> "$GITHUB_OUTPUT"
  exit 0
fi

echo "UNEXPECTED ERROR (not a capacity miss — needs a human to look at it):"
echo "$output"
exit 1
