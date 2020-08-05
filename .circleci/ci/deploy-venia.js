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

ci.stage("Install GHR");
ci.sh("mkdir -p tmp");
ci.sh(
    "curl -L https://github.com/tcnksm/ghr/releases/download/v0.12.1/ghr_v0.12.1_linux_amd64.tar.gz | tar xvz -C ./tmp"
);
ci.sh("mv tmp/**/ghr ./ghr");
ci.sh("chmod +x ghr");

ci.sh("mkdir -p artifacts"); // target folder for all the build artifacts

let profiles = ['cloud', 'classic'];
profiles.forEach(profile => {
	ci.stage(`Build and install Venia '${profile}'`);
	ci.sh(`mvn clean install -P${profile}`);
	ci.sh(`cp all/target/${releaseArtifact}.all-${releaseVersion}-${profile}.zip artifacts`);
})

ci.stage("Deploy Venia Sample Project to GitHub");
ci.sh(`./ghr -t ${ci.env("GITHUB_TOKEN")} \
    -u ${ci.env("CIRCLE_PROJECT_USERNAME")} \
    -r ${ci.env("CIRCLE_PROJECT_REPONAME")} \
    -c ${ci.env("CIRCLE_SHA1")} \
    -replace ${ci.env("CIRCLE_TAG")} artifacts/`);
