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
# Builds the matching aem-core-cif-components branch into ./dependencies so a PR that
# depends on unreleased component changes can build against them.

set -euo pipefail

# Resolve the current branch: GITHUB_HEAD_REF on pull requests, GITHUB_REF_NAME on branch
# pushes, empty otherwise (e.g. tag builds).
if [[ "${GITHUB_EVENT_NAME:-}" == "pull_request" ]]; then
    branch="${GITHUB_HEAD_REF:-}"
elif [[ "${GITHUB_REF:-}" == refs/heads/* ]]; then
    branch="${GITHUB_REF_NAME:-}"
else
    branch=""
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
    # 2.18.5-SNAPSHOT. That only works when ~/.m2 already contains the older parent POM from a
    # prior build; align the versions so a cold ~/.m2 can resolve the parent via relativePath.
    if [[ -f it/site/pom.xml && -f parent/pom.xml ]]; then
        parent_version="$(sed -n 's:.*<version>\([^<]*\)</version>.*:\1:p' parent/pom.xml | head -n1)"
        site_parent_version="$(sed -n '/<parent>/,/<\/parent>/s:.*<version>\([^<]*\)</version>.*:\1:p' it/site/pom.xml | head -n1)"
        if [[ -n "${parent_version}" && -n "${site_parent_version}" && "${site_parent_version}" != "${parent_version}" ]]; then
            echo "Aligning it/site parent version ${site_parent_version} -> ${parent_version} (cold ~/.m2 workaround)"
            sed -i "s/${site_parent_version}/${parent_version}/g" it/site/pom.xml
        fi
    fi

    mvn -B clean install

    cd react-components
    npm link
    cd ../extensions/product-recs/react-components
    npm link
fi
