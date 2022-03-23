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

const doUpdate = () => {
    // the local branch that will be used to checkout the target branch 
    // it will be force-deleted before the checkout if it exists, be careful
    const LOCAL_BRANCH = "tmp";
    // the target branch that should be updated
    const TARGET_BRANCH = "main"; // ci.env('CIRCLE_BRANCH');
    // the revision to update the TARGET_BRANCH to
    const MERGE_REVISION = ci.env('CIRCLE_TAG'); // ci.env('CIRCLE_SHA1');
    
    const downstreamRemoteExists = ci.sh(
        'git remote get-url downstream 2>/dev/null || echo "no"', true)
        .toString().trim() != "no";
    
    if (!downstreamRemoteExists) {
        ci.sh(`git remote add downstream ${ci.env('GIT_REPO')}`);
    }

    ci.sh('git fetch downstream');
    
    const tmpBranchExists = ci.sh(
        `git rev-parse --verify "${LOCAL_BRANCH}" 2>/dev/null ||  echo "no"`, true)
        .toString().trim() != "no";

    if (tmpBranchExists) {
        ci.sh(`git branch -D ${LOCAL_BRANCH}`);
    } 

    ci.sh(`git checkout -b ${LOCAL_BRANCH} downstream/${TARGET_BRANCH}`);
    ci.sh(`GIT_MERGE_AUTOEDIT=no git merge --no-ff ${MERGE_REVISION}`);
    ci.sh(`git push downstream ${LOCAL_BRANCH}:${TARGET_BRANCH}`);
}

ci.context();

ci.gitCredentials(ci.env('GIT_USER'), ci.env('GIT_PASSWORD'),
    () => ci.gitImpersonate('CircleCI Builds', 'builds@circleci.com',
        () => doUpdate()));