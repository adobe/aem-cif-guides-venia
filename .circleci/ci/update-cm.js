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
    const tmpBranchExists = ci.sh(
        'git rev-parse --verify "tmp" 2>/dev/null ||  echo "no"', true)
        .toString().trim() != "no";
    const downstreamRemoteExists = ci.sh(
        'git remote get-url asdf 2>/dev/null || echo "no"', true)
        .toString().trim() != "no";
    

    if (!downstreamRemoteExists) {
        ci.sh(`git remote add downstream ${ci.env('GIT_REPO')}`);
    }

    ci.sh('git fetch downstream');
    
    if (tmpBranchExists) {
        ci.sh('git branch -D tmp')
    } 

    //ci.sh('git checkout -b tmp downstream/main');
    //ci.sh(`git merge ${ci.env('CIRCLE_TAG')}`);
    ci.sh(`git checkout -b tmp downstream/${ci.env('CIRCLE_BRANCH')}`);
    ci.sh(`GIT_MERGE_AUTOEDIT=no git merge ${ci.env('CIRCLE_SHA1')}`);
    ci.sh(`git push downstream ${ci.env('CIRCLE_BRANCH')}`);
}

ci.context();

ci.gitCredentials(ci.env('GIT_USER'), ci.env('GIT_PASSWORD'),
    () => ci.gitImpersonate('CircleCI Builds', 'builds@circleci.com',
        () => doUpdate()));