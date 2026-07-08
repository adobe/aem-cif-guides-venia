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

if command -v google-chrome >/dev/null 2>&1 && command -v chromedriver >/dev/null 2>&1; then
    google-chrome --version
    chromedriver --version
    exit 0
fi

if command -v apt-get >/dev/null 2>&1; then
    sudo apt-get update
    sudo apt-get install -y wget gnupg
    wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | sudo gpg --dearmor -o /usr/share/keyrings/google-chrome.gpg
    echo "deb [arch=amd64 signed-by=/usr/share/keyrings/google-chrome.gpg] http://dl.google.com/linux/chrome/deb/ stable main" | sudo tee /etc/apt/sources.list.d/google-chrome.list
    sudo apt-get update
    sudo apt-get install -y google-chrome-stable
fi

if ! command -v chromedriver >/dev/null 2>&1; then
    chrome_version="$(google-chrome --version | awk '{print $3}' | cut -d. -f1)"
    driver_version="$(curl -s "https://googlechromelabs.github.io/chrome-for-testing/LATEST_RELEASE_${chrome_version}")"
    curl -sSL "https://storage.googleapis.com/chrome-for-testing-public/${driver_version}/linux64/chromedriver-linux64.zip" -o /tmp/chromedriver.zip
    sudo unzip -o /tmp/chromedriver.zip -d /tmp
    sudo mv /tmp/chromedriver-linux64/chromedriver /usr/local/bin/chromedriver
    sudo chmod +x /usr/local/bin/chromedriver
fi

google-chrome --version
chromedriver --version
