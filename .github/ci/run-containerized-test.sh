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
docker run -d --network host --name "${aem_container}" "${AEM_IMAGE}"

docker pull "${QP_IMAGE}"
docker run --rm --network host --user root \
    -e AEM -e TYPE -e BROWSER -e CI_QP_PATH -e QP_SERVER_HOSTNAME -e AEM_LOG_HOST \
    -e ARTIFACTORY_CLOUD_USER -e ARTIFACTORY_CLOUD_PASS \
    -e COMMERCE_ENDPOINT -e COMMERCE_INTEGRATION_TOKEN \
    -e VENIA_ACCOUNT_EMAIL -e VENIA_ACCOUNT_PASSWORD \
    -e CI_BUILD_PATH="${GITHUB_WORKSPACE}" \
    -e GITHUB_EVENT_NAME -e GITHUB_HEAD_REF -e GITHUB_REF -e GITHUB_REF_NAME \
    -v "${GITHUB_WORKSPACE}:${GITHUB_WORKSPACE}" -w "${GITHUB_WORKSPACE}" \
    "${QP_IMAGE}" bash .github/ci/run-integration-test.sh
