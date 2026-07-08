#!/usr/bin/env bash
#
# Copyright 2026 Adobe. All rights reserved.
# Licensed under the Apache License, Version 2.0
#
# CircleCI integration_test_steps inside the QP container (after attach_workspace):
#   - browser-tools/install-chrome
#   - browser-tools/install-chromedriver
#   - node .circleci/ci/it-tests.js
#
# Artifactory auth for maven-download-plugin uses -s .circleci/settings.xml -Partifactory-cloud
# inside it-tests.js (same as CircleCI — no ~/.m2/settings.xml here).

set -euo pipefail

build_dir="${CI_BUILD_PATH:-/home/circleci/build}"

# CircleCI installs Chrome + ChromeDriver for every integration_test_steps job.
bash "${build_dir}/.github/ci/install-chrome.sh"

node "${build_dir}/.circleci/ci/it-tests.js"
