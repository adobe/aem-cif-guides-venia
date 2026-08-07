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

# Start the AEM/QuickProvider container. Readiness (port 55555) is polled inside the client
# container by run-integration-test.sh once it starts.
docker run -d --network host --name "${aem_container}" "${AEM_IMAGE}"

docker pull "${QP_IMAGE}"
# Bind-mount the host's own ~/.m2 into the container's /root/.m2 (it runs as --user root)
# so Maven's downloads land on the runner's filesystem, at a path the actions/cache steps
# (which run directly on the runner, not in this container) can read/write. Without this
# mount /root/.m2/repository only ever exists inside the container's ephemeral layer —
# gone the instant --rm removes it — so every run re-downloads the whole dependency tree.
# (${HOME}/.m2 is created near the top of this script.)
# NOTE: JACOCO_AGENT is consumed by it-tests.js (the -javaagent path passed to qp.sh) but is
# deliberately NOT forwarded here — it is baked into the qp image's own environment. If the qp
# image is ever swapped for one without it, the JaCoCo agent path resolves empty and qp.sh fails.
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
