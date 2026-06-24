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

if [[ "${TYPE:-}" == "selenium" ]]; then
    sudo apt-get update -qq
    sudo apt-get install -y -qq wget gnupg
    wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | sudo apt-key add -
    echo "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main" | sudo tee /etc/apt/sources.list.d/google-chrome.list
    sudo apt-get update -qq
    sudo apt-get install -y -qq google-chrome-stable
    CHROME_VERSION=$(google-chrome --version | awk '{print $3}' | cut -d. -f1)
    wget -q "https://chromedriver.storage.googleapis.com/LATEST_RELEASE_${CHROME_VERSION}" -O /tmp/chromedriver_version
    CHROMEDRIVER_VERSION=$(cat /tmp/chromedriver_version)
    wget -q "https://chromedriver.storage.googleapis.com/${CHROMEDRIVER_VERSION}/chromedriver_linux64.zip" -O /tmp/chromedriver.zip
    sudo unzip -q /tmp/chromedriver.zip -d /usr/local/bin
    sudo chmod +x /usr/local/bin/chromedriver
fi

node .circleci/ci/it-tests.js
