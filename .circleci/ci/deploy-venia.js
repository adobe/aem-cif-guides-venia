/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
 *    This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License. You may obtain a copy
 *    of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under
 *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *    OF ANY KIND, either express or implied. See the License for the specific language
 *    governing permissions and limitations under the License.
 *
 ******************************************************************************/

"use strict";

const ci = new (require("./ci.js"))();

ci.context();

const releaseVersion = ci.sh(`mvn help:evaluate -Dexpression=project.version -q -DforceStdout`, true).toString().trim();
const releaseArtifact = ci.sh(`mvn help:evaluate -Dexpression=project.artifactId -q -DforceStdout`, true).toString().trim();
const repoPath = process.env.CI_REPO_PATH || '/home/circleci/repo';
const mvnOpts = `-B -s ${repoPath}/.circleci/settings.xml`;
const githubOwner = process.env.CIRCLE_PROJECT_USERNAME || process.env.GITHUB_REPOSITORY_OWNER;
const githubRepo = process.env.CIRCLE_PROJECT_REPONAME || (process.env.GITHUB_REPOSITORY || '').split('/')[1];
const githubSha = process.env.CIRCLE_SHA1 || process.env.GITHUB_SHA;
const githubTag = process.env.CIRCLE_TAG || process.env.GITHUB_REF_NAME;

ci.stage("Install GHR");
ci.sh("mkdir -p tmp");
ci.sh(
    "curl -L https://github.com/tcnksm/ghr/releases/download/v0.12.1/ghr_v0.12.1_linux_amd64.tar.gz | tar xvz -C ./tmp"
);
ci.sh("mv tmp/**/ghr ./ghr");
ci.sh("chmod +x ghr");

// target folder for all the build artifacts
ci.sh("mkdir -p artifacts");

ci.stage("Deploy Venia Sample Project to Maven Central");
// build and deploy only the cloud artifacts
ci.sh(`mvn ${mvnOpts} clean deploy -Prelease,central`)
ci.sh(`cp all/target/${releaseArtifact}.all-${releaseVersion}.zip artifacts/${releaseArtifact}.all-${releaseVersion}.zip`);

ci.stage("Deploy Venia Sample Project to GitHub");
// build also the classic artifacts for github
ci.sh(`mvn ${mvnOpts} clean install -Pclassic -pl classic/ui.config,classic/ui.content,classic/dispatcher,classic/all`);
ci.sh(`cp classic/all/target/${releaseArtifact}.all-classic-${releaseVersion}.zip artifacts/${releaseArtifact}.all-classic-${releaseVersion}.zip`);
ci.sh(`./ghr -t ${ci.env("GITHUB_TOKEN")} \
    -u ${githubOwner} \
    -r ${githubRepo} \
    -c ${githubSha} \
    -replace ${githubTag} artifacts/`);

