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

# This script runs inside the qp container started by run-containerized-test.sh,
# sharing --network host with the aem container, so "localhost" reaches it directly.
bash "${repo_root}/.github/ci/wait-for-quickprovider.sh"

if [[ "${TYPE:-}" == "selenium" ]]; then
    bash "${repo_root}/.github/ci/install-chrome.sh"
fi

node "${repo_root}/.github/ci/it-tests.js"
