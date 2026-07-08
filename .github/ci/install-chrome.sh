#!/usr/bin/env bash
#
# Copyright 2026 Adobe. All rights reserved.
# Licensed under the Apache License, Version 2.0

set -euo pipefail

if command -v google-chrome >/dev/null 2>&1 && command -v chromedriver >/dev/null 2>&1; then
    google-chrome --version
    chromedriver --version
    exit 0
fi

install_chrome_for_testing() {
    local install_root="${HOME}/.chrome-for-testing"
    mkdir -p "${install_root}"

    local meta_url="https://googlechromelabs.github.io/chrome-for-testing/last-known-good-versions-with-downloads.json"
    local version
    version="$(curl -fsSL "${meta_url}" | python3 -c "import json,sys; print(json.load(sys.stdin)['channels']['Stable']['version'])")"

    local chrome_zip="${install_root}/chrome-linux64.zip"
    curl -fsSL "https://storage.googleapis.com/chrome-for-testing-public/${version}/linux64/chrome-linux64.zip" -o "${chrome_zip}"
    unzip -qo "${chrome_zip}" -d "${install_root}"
    sudo ln -sf "${install_root}/chrome-linux64/chrome" /usr/local/bin/google-chrome

    local driver_zip="${install_root}/chromedriver-linux64.zip"
    curl -fsSL "https://storage.googleapis.com/chrome-for-testing-public/${version}/linux64/chromedriver-linux64.zip" -o "${driver_zip}"
    unzip -qo "${driver_zip}" -d "${install_root}"
    sudo ln -sf "${install_root}/chromedriver-linux64/chromedriver" /usr/local/bin/chromedriver
    sudo chmod +x /usr/local/bin/chromedriver
}

if command -v curl >/dev/null 2>&1 && command -v python3 >/dev/null 2>&1 && command -v unzip >/dev/null 2>&1; then
    install_chrome_for_testing
else
    if command -v apt-get >/dev/null 2>&1; then
        # Remove stale lists from CI images before apt update (avoids NO_PUBKEY on dl.google.com).
        sudo rm -f /etc/apt/sources.list.d/google-chrome.list
        sudo apt-get update
        sudo apt-get install -y wget gnupg unzip curl python3
        wget -q -O - https://dl.google.com/linux/linux_signing_key.pub | sudo gpg --dearmor -o /usr/share/keyrings/google-chrome.gpg
        echo "deb [arch=amd64 signed-by=/usr/share/keyrings/google-chrome.gpg] https://dl.google.com/linux/chrome/deb/ stable main" | sudo tee /etc/apt/sources.list.d/google-chrome.list
        sudo apt-get update
        sudo apt-get install -y google-chrome-stable
    fi

    if ! command -v chromedriver >/dev/null 2>&1; then
        install_chrome_for_testing
    fi
fi

google-chrome --version
chromedriver --version
