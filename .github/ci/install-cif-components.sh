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

    # On master, it/site still references parent 2.18.3-SNAPSHOT while parent/pom.xml is
    # 2.18.5-SNAPSHOT. CircleCI succeeds because restore_cache (~/.m2) already contains the
    # older parent POM from prior Adobe builds. Align versions so a cold ~/.m2 can use relativePath.
    if [[ -f it/site/pom.xml && -f parent/pom.xml ]]; then
        parent_version="$(sed -n 's:.*<version>\([^<]*\)</version>.*:\1:p' parent/pom.xml | head -n1)"
        site_parent_version="$(sed -n '/<parent>/,/<\/parent>/s:.*<version>\([^<]*\)</version>.*:\1:p' it/site/pom.xml | head -n1)"
        if [[ -n "${parent_version}" && -n "${site_parent_version}" && "${site_parent_version}" != "${parent_version}" ]]; then
            echo "Aligning it/site parent version ${site_parent_version} -> ${parent_version} (CircleCI ~/.m2 cache workaround)"
            sed -i "s/${site_parent_version}/${parent_version}/g" it/site/pom.xml
        fi
    fi

    mvn -B clean install

    cd react-components
    npm link
    cd ../extensions/product-recs/react-components
    npm link
fi
