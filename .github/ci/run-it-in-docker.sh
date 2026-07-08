#!/usr/bin/env bash
#
# Copyright 2026 Adobe. All rights reserved.
# Licensed under the Apache License, Version 2.0
#
# Emulates CircleCI multi-container executors (QP primary + AEM sidecar on localhost).
# See .circleci/config.yml test_executor_* and integration_test_steps.

set -euo pipefail

if [[ $# -lt 2 ]]; then
    echo "Usage: $0 <qp-image> <aem-image>"
    exit 1
fi

qp_image="$1"
aem_image="$2"

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
registry="docker-adobe-cif-release.dr-uw2.adobeitc.com"
build_dir="/home/circleci/build"
aem_name="aem-sidecar-${GITHUB_RUN_ID:-$$}"

cleanup() {
    docker rm -f "${aem_name}" >/dev/null 2>&1 || true
}
trap cleanup EXIT

if [[ -z "${ARTIFACTORY_CLOUD_USER:-}" || -z "${ARTIFACTORY_CLOUD_PASS:-}" ]]; then
    echo "::error::Missing ARTIFACTORY_CLOUD_USER or ARTIFACTORY_CLOUD_PASS."
    exit 1
fi

echo "${ARTIFACTORY_CLOUD_PASS}" | docker login "${registry}" -u "${ARTIFACTORY_CLOUD_USER}" --password-stdin

docker pull "${aem_image}"
docker pull "${qp_image}"

# CircleCI: AEM is the second container in the executor; ports are on localhost in the QP container.
echo "Starting AEM sidecar (${aem_image})..."
docker run -d --name "${aem_name}" --network host "${aem_image}"

export QP_SERVER_HOSTNAME=localhost
export AEM_LOG_HOST=localhost
export CI_BUILD_PATH="${build_dir}"

bash "${repo_root}/.github/ci/wait-for-quickprovider.sh"

circleci_uid="$(docker run --rm "${qp_image}" id -u circleci)"
circleci_gid="$(docker run --rm "${qp_image}" id -g circleci)"
sudo chown -R "${circleci_uid}:${circleci_gid}" "${GITHUB_WORKSPACE}"
if [[ -d "${HOME}/.m2" ]]; then
    sudo chown -R "${circleci_uid}:${circleci_gid}" "${HOME}/.m2"
fi

docker_env=(
    -e "CI_BUILD_PATH=${build_dir}"
    -e "QP_SERVER_HOSTNAME=localhost"
    -e "AEM_LOG_HOST=localhost"
)
for var in AEM TYPE BROWSER CI_QP_PATH COMMERCE_ENDPOINT COMMERCE_INTEGRATION_TOKEN \
    VENIA_ACCOUNT_EMAIL VENIA_ACCOUNT_PASSWORD ARTIFACTORY_CLOUD_USER ARTIFACTORY_CLOUD_PASS; do
    if [[ -n "${!var:-}" ]]; then
        docker_env+=( -e "${var}=${!var}" )
    fi
done

m2_mount=()
if [[ -d "${HOME}/.m2" ]]; then
    m2_mount=( -v "${HOME}/.m2:/home/circleci/.m2" )
fi

echo "Running tests in QP container (${qp_image}) at ${build_dir}..."
docker run --rm --network host \
    --user circleci \
    "${docker_env[@]}" \
    "${m2_mount[@]}" \
    -v "${GITHUB_WORKSPACE}:${build_dir}" \
    -w "${build_dir}" \
    "${qp_image}" \
    bash "${build_dir}/.github/ci/run-integration-test-inner.sh"
