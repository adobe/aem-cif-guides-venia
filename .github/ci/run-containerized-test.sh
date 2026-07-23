#!/usr/bin/env bash
#
# Copyright 2026 Adobe. All rights reserved.
# Licensed under the Apache License, Version 2.0
#
# Runs run-integration-test.sh inside the QuickProvider client image, against an AEM
# server image started as a sibling container. Both containers use --network host so
# that "localhost" means the same thing to both — required because QuickProvider's RMI
# protocol advertises "localhost" as its own callback address.
#
# GitHub Actions' `container:` + `services:` keys can't express this: combining
# --network host with a `services:` container crashes the runner before the job even
# starts ("Error: Value cannot be null. (Parameter 'ContainerId')"). So this script
# drives both containers directly via the Docker CLI on the bare runner instead.

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

# Create ~/.m2 up front (before any docker pull/login that might fail) so the runner-side
# path the actions/cache save step targets always exists — otherwise a job that fails
# early logs a spurious "Path(s) specified do(es) not exist ... Cache save failed" warning.
mkdir -p "${HOME}/.m2"

: "${QP_IMAGE:?QP_IMAGE must be set}"
: "${AEM_IMAGE:?AEM_IMAGE must be set}"
: "${ARTIFACTORY_CLOUD_USER:?}"
: "${ARTIFACTORY_CLOUD_PASS:?}"

registry="${QP_IMAGE%%/*}"
echo "${ARTIFACTORY_CLOUD_PASS}" | docker login "${registry}" -u "${ARTIFACTORY_CLOUD_USER}" --password-stdin

aem_container="aem-${GITHUB_RUN_ID:-local}-${GITHUB_JOB:-job}"

cleanup() {
    docker logs "${aem_container}" || true
    docker rm -f "${aem_container}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

docker pull "${AEM_IMAGE}"

# Bring the AEM/QuickProvider container up and confirm its RMI port (55555) is actually
# listening on the host (reachable directly thanks to --network host) BEFORE launching the
# client container. On a small runner the heavier AEM-LTS quickstart can lose a startup race
# and have its QuickProvider RMI agent die with a BindException; once it's dead, the
# in-container wait-for-quickprovider.sh would just poll a corpse until it times out. If the
# port doesn't come up (or the container exits), start a FRESH container — which clears any
# stale X lock / half-bound socket from the failed boot — and retry.
qp_port="${QP_SERVER_PORT:-55555}"
aem_start_attempts="${AEM_START_ATTEMPTS:-3}"
aem_ready_polls="${AEM_READY_POLLS:-60}"            # 60 * 5s = 5 min per attempt
aem_ready_sleep="${AEM_READY_SLEEP_SECONDS:-5}"

wait_for_qp() {
    local attempt running
    for attempt in $(seq 1 "${aem_ready_polls}"); do
        if (echo >"/dev/tcp/localhost/${qp_port}") >/dev/null 2>&1; then
            echo "QuickProvider is up on localhost:${qp_port} (poll ${attempt})."
            return 0
        fi
        # No point waiting the full window if the container already exited/crashed.
        running="$(docker inspect -f '{{.State.Running}}' "${aem_container}" 2>/dev/null || echo false)"
        if [[ "${running}" != "true" ]]; then
            echo "AEM container is no longer running; aborting this attempt."
            return 1
        fi
        sleep "${aem_ready_sleep}"
    done
    echo "Timed out waiting for QuickProvider on localhost:${qp_port}."
    return 1
}

for start_attempt in $(seq 1 "${aem_start_attempts}"); do
    echo "Starting AEM container (attempt ${start_attempt}/${aem_start_attempts})..."
    docker rm -f "${aem_container}" >/dev/null 2>&1 || true
    docker run -d --network host --name "${aem_container}" "${AEM_IMAGE}"
    if wait_for_qp; then
        break
    fi
    echo "QuickProvider did not become reachable on attempt ${start_attempt}; recent AEM logs:"
    docker logs --tail 100 "${aem_container}" 2>&1 || true
    if [[ "${start_attempt}" -eq "${aem_start_attempts}" ]]; then
        echo "::error::QuickProvider failed to start after ${aem_start_attempts} attempts."
        exit 1
    fi
    echo "Retrying with a fresh AEM container..."
done

docker pull "${QP_IMAGE}"
# Bind-mount the host's own ~/.m2 into the container's /root/.m2 (it runs as --user root)
# so Maven's downloads land on the runner's filesystem, at a path the actions/cache steps
# (which run directly on the runner, not in this container) can read/write. Without this
# mount /root/.m2/repository only ever exists inside the container's ephemeral layer —
# gone the instant --rm removes it — so every run re-downloads the whole dependency tree.
# (${HOME}/.m2 is created near the top of this script.)
docker run --rm --network host --user root \
    -e AEM -e TYPE -e BROWSER -e CI_QP_PATH -e QP_SERVER_HOSTNAME -e AEM_LOG_HOST \
    -e ARTIFACTORY_CLOUD_USER -e ARTIFACTORY_CLOUD_PASS \
    -e COMMERCE_ENDPOINT -e COMMERCE_INTEGRATION_TOKEN \
    -e VENIA_ACCOUNT_EMAIL -e VENIA_ACCOUNT_PASSWORD \
    -e CI_BUILD_PATH="${GITHUB_WORKSPACE}" \
    -e GITHUB_EVENT_NAME -e GITHUB_HEAD_REF -e GITHUB_REF -e GITHUB_REF_NAME \
    -v "${GITHUB_WORKSPACE}:${GITHUB_WORKSPACE}" -w "${GITHUB_WORKSPACE}" \
    -v "${HOME}/.m2:/root/.m2" \
    "${QP_IMAGE}" bash .github/ci/run-integration-test.sh
