#!/usr/bin/env bash
#
# Copyright 2026 Adobe. All rights reserved.
# This file is licensed to you under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License. You may obtain a copy
# of the License at http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software distributed under
# the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
# OF ANY KIND, either express or implied. See the License for the specific language
# governing permissions and limitations under the License.
#

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# Artifactory auth for maven-download-plugin in it-tests.js (-Partifactory-cloud).
bash "${repo_root}/.github/ci/maven-settings.sh"

# Wait for QuickProvider's RMI port to come up. This script runs inside the qp container
# started by run-containerized-test.sh, sharing --network host with the aem container, so
# "localhost" reaches it directly.
qp_host="${QP_SERVER_HOSTNAME:-localhost}"
qp_port="${QP_SERVER_PORT:-55555}"
qp_attempts="${QP_WAIT_ATTEMPTS:-60}"
qp_sleep="${QP_WAIT_SLEEP_SECONDS:-5}"
echo "Waiting for QuickProvider at ${qp_host}:${qp_port} (up to $((qp_attempts * qp_sleep))s)..."
for attempt in $(seq 1 "${qp_attempts}"); do
    if (echo >"/dev/tcp/${qp_host}/${qp_port}") >/dev/null 2>&1; then
        echo "QuickProvider is reachable at ${qp_host}:${qp_port} (attempt ${attempt})."
        break
    fi
    if [[ "${attempt}" -eq "${qp_attempts}" ]]; then
        echo "::error::QuickProvider did not become reachable at ${qp_host}:${qp_port}."
        echo "::error::The AEM service container must expose port ${qp_port} on ${qp_host}."
        exit 1
    fi
    echo "Attempt ${attempt}/${qp_attempts}: ${qp_host}:${qp_port} not ready yet..."
    sleep "${qp_sleep}"
done

if [[ "${TYPE:-}" == "selenium" ]]; then
    bash "${repo_root}/.github/ci/install-chrome.sh"
fi

node "${repo_root}/.github/ci/it-tests.js"
