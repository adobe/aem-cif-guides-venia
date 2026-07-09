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

if ! command -v google-chrome >/dev/null 2>&1 || ! command -v chromedriver >/dev/null 2>&1; then
    if command -v apt-get >/dev/null 2>&1; then
        # The base image may already have an unsigned google-chrome apt source configured,
        # which makes apt-get update fail before we get a chance to install the signing key
        # below (which overwrites that source with a properly signed one).
        sudo apt-get update || true
        command -v wget >/dev/null 2>&1 || sudo apt-get install -y wget
        command -v gpg >/dev/null 2>&1 || sudo apt-get install -y gnupg
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
fi

# run-containerized-test.sh runs this container as root (needed to write into the
# bind-mounted workspace), but Chrome's sandbox refuses to run as root, and
# wdio.conf.local.js ("DO NOT MODIFY") never passes --no-sandbox. Without it every
# headless session fails instantly with "DevToolsActivePort file doesn't exist",
# which fails every single spec. Wrap the real binary so any launcher (chromedriver,
# selenium-standalone) picks the flag up transparently, regardless of what invokes it.
chrome_bin="$(readlink -f "$(command -v google-chrome-stable 2>/dev/null || command -v google-chrome)")"
if [[ "$(id -u)" -eq 0 ]] && ! grep -q -- '--no-sandbox' "${chrome_bin}" 2>/dev/null; then
    sudo cp "${chrome_bin}" "${chrome_bin}.real"
    sudo tee "${chrome_bin}" >/dev/null <<WRAPPER
#!/usr/bin/env bash
exec "${chrome_bin}.real" --no-sandbox --disable-dev-shm-usage "\$@"
WRAPPER
    sudo chmod +x "${chrome_bin}"
fi

google-chrome --version
chromedriver --version
