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
# Maven Central deploy + classic package build (GitHub Release upload is handled by GHA).
#
# The CircleCI version wiped ~/.gnupg and ~/.npmrc at the end to avoid leaking the imported GPG
# key / npm token on its reused executors. That cleanup is dropped here on purpose: GitHub Actions
# runners are ephemeral and destroyed after the job, so there is nothing persistent to scrub.

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "${repo_root}"

mvn_opts=(-B -s "${repo_root}/.github/ci/settings.xml")
release_version="$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)"
release_artifact="$(mvn help:evaluate -Dexpression=project.artifactId -q -DforceStdout)"

mkdir -p artifacts

echo "Deploy cloud artifacts to Maven Central (${release_artifact} ${release_version})"
mvn "${mvn_opts[@]}" clean deploy -Prelease,central
cp "all/target/${release_artifact}.all-${release_version}.zip" "artifacts/"

echo "Build classic artifacts for GitHub Release"
mvn "${mvn_opts[@]}" clean install -Pclassic -pl classic/ui.config,classic/ui.content,classic/dispatcher,classic/all
cp "classic/all/target/${release_artifact}.all-classic-${release_version}.zip" "artifacts/"

echo "Release artifacts:"
ls -la artifacts/
