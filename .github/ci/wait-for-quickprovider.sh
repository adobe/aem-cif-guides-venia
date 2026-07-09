#!/usr/bin/env bash
#
# Copyright 2026 Adobe. All rights reserved.
# Licensed under the Apache License, Version 2.0

set -euo pipefail

# CircleCI exposes secondary container ports on localhost; GitHub Actions needs
# --network host on both job and service containers for the same behavior.
host="${QP_SERVER_HOSTNAME:-localhost}"
port="${QP_SERVER_PORT:-55555}"
max_attempts="${QP_WAIT_ATTEMPTS:-60}"
sleep_seconds="${QP_WAIT_SLEEP_SECONDS:-5}"

echo "Waiting for QuickProvider at ${host}:${port} (up to $((max_attempts * sleep_seconds))s)..."

for attempt in $(seq 1 "${max_attempts}"); do
    if (echo >"/dev/tcp/${host}/${port}") >/dev/null 2>&1; then
        echo "QuickProvider is reachable at ${host}:${port} (attempt ${attempt})."
        exit 0
    fi
    echo "Attempt ${attempt}/${max_attempts}: ${host}:${port} not ready yet..."
    sleep "${sleep_seconds}"
done

echo "::error::QuickProvider did not become reachable at ${host}:${port}."
echo "::error::The AEM service container (circleci-aem) must expose port 55555 on localhost."
exit 1
