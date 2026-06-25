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
# Same logic as .circleci/config.yml install_components (working_directory: ./dependencies).

set -euo pipefail

# CircleCI: CIRCLE_BRANCH = source branch name on PRs; empty on tag builds.
if [[ "${GITHUB_EVENT_NAME:-}" == "pull_request" ]]; then
    branch="${GITHUB_HEAD_REF:-}"
elif [[ "${GITHUB_REF:-}" == refs/heads/* ]]; then
    branch="${GITHUB_REF_NAME:-}"
else
    branch="${CIRCLE_BRANCH:-}"
fi

if [[ -n "${branch}" && "${branch}" != "main" ]]; then
    repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
    mkdir -p "${repo_root}/dependencies"
    cd "${repo_root}/dependencies"

    git clone https://github.com/adobe/aem-core-cif-components.git
    cd aem-core-cif-components

    if git ls-remote --heads origin "${branch}" | grep -q "${branch}"; then
        git fetch
        git checkout "${branch}"
    fi

    if [[ -n "${ARTIFACTORY_CLOUD_USER:-}" && -f "${repo_root}/.circleci/settings.xml" ]]; then
        mvn -B clean install -s "${repo_root}/.circleci/settings.xml" -Partifactory-cloud
    else
        mvn -B clean install
    fi

    cd react-components
    npm link
    cd ../extensions/product-recs/react-components
    npm link
fi
