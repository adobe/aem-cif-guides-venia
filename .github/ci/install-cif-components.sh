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

# Use head branch on pull_request; fall back to ref name on push (CircleCI: CIRCLE_BRANCH).
branch="${GITHUB_HEAD_REF:-${GITHUB_REF_NAME:-${CIRCLE_BRANCH:-}}}"
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

cd "${repo_root}"
cif_version="$(mvn help:evaluate -Dexpression=core.cif.components.version -q -DforceStdout)"
echo "CIF components version: ${cif_version}"

use_fed_dev=false
if [[ -n "${branch}" && "${branch}" != "main" && "${GITHUB_REF:-}" != refs/tags/* && "${cif_version}" == *-SNAPSHOT* ]]; then
    use_fed_dev=true
fi

if [[ -n "${GITHUB_ENV:-}" ]]; then
    echo "USE_FED_DEV=${use_fed_dev}" >> "${GITHUB_ENV}"
fi

if [[ "${use_fed_dev}" != "true" ]]; then
    echo "Skipping CIF components source build (release version or main branch)."
    exit 0
fi

if [[ -z "${ARTIFACTORY_CLOUD_USER:-}" || -z "${ARTIFACTORY_CLOUD_PASS:-}" ]]; then
    echo "ERROR: ARTIFACTORY_CLOUD_USER and ARTIFACTORY_CLOUD_PASS secrets are required for SNAPSHOT CIF builds."
    exit 1
fi

mkdir -p "${repo_root}/dependencies"
cd "${repo_root}/dependencies"

git clone https://github.com/adobe/aem-core-cif-components.git
cd aem-core-cif-components

if git ls-remote --heads origin "${branch}" | grep -q "${branch}"; then
    git fetch
    git checkout "${branch}"
fi

mvn -B clean install -s "${repo_root}/.circleci/settings.xml" -Partifactory-cloud

cd react-components
npm ci --legacy-peer-deps
npm link
cd ../extensions/product-recs/react-components
npm ci --legacy-peer-deps
npm link
