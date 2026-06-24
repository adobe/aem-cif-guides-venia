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

BRANCH="${GITHUB_REF_NAME:-${CIRCLE_BRANCH:-}}"
REF_TYPE="${GITHUB_REF_TYPE:-branch}"

if [[ "${REF_TYPE}" == "branch" && -n "${BRANCH}" && "${BRANCH}" != "main" ]]; then
    mkdir -p dependencies
    cd dependencies
    git clone https://github.com/adobe/aem-core-cif-components.git
    cd aem-core-cif-components
    components_branch=$(git ls-remote --heads origin "${BRANCH}")
    if [[ -n "${components_branch}" ]]; then
        git fetch
        git checkout "${BRANCH}"
    fi
    mvn -B clean install
    cd react-components
    npm link
    cd ../extensions/product-recs/react-components
    npm link
fi
