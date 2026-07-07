#!/usr/bin/env bash
#
# Copyright 2026 Adobe. All rights reserved.
# Licensed under the Apache License, Version 2.0

set -euo pipefail

if [[ -z "${ARTIFACTORY_CLOUD_USER:-}" || -z "${ARTIFACTORY_CLOUD_PASS:-}" ]]; then
    echo "::error::Missing ARTIFACTORY_CLOUD_USER or ARTIFACTORY_CLOUD_PASS repository secrets."
    echo "Diagnostics: ARTIFACTORY_CLOUD_USER set=$([[ -n "${ARTIFACTORY_CLOUD_USER:-}" ]] && echo yes || echo no)"
    echo "Diagnostics: ARTIFACTORY_CLOUD_PASS set=$([[ -n "${ARTIFACTORY_CLOUD_PASS:-}" ]] && echo yes || echo no)"
    exit 1
fi

# Safe diagnostics only — never print secret values.
echo "Diagnostics: ARTIFACTORY_CLOUD_USER length=${#ARTIFACTORY_CLOUD_USER}"
echo "Diagnostics: ARTIFACTORY_CLOUD_PASS length=${#ARTIFACTORY_CLOUD_PASS}"
echo "Diagnostics: testing Artifactory login (repo root + sample SNAPSHOT POM)..."

artifactory_base="https://artifactory-uw2.adobeitc.com/artifactory/maven-adobe-cif-snapshot"
sample_pom="${artifactory_base}/com/adobe/commerce/cif/core-cif-components-parent/2.18.3-SNAPSHOT/core-cif-components-parent-2.18.3-SNAPSHOT.pom"

for test_url in "${artifactory_base}/" "${sample_pom}"; do
    http_code="$(curl -s -o /dev/null -w '%{http_code}' \
        -u "${ARTIFACTORY_CLOUD_USER}:${ARTIFACTORY_CLOUD_PASS}" \
        "${test_url}")"
    echo "Diagnostics: GET ${test_url} -> HTTP ${http_code}"

    if [[ "${http_code}" == "401" || "${http_code}" == "403" ]]; then
        echo "::error::Artifactory login failed (HTTP ${http_code}) — username/password rejected."
        echo "::error::Fix: ask an Adobe admin to copy ARTIFACTORY_CLOUD_USER and ARTIFACTORY_CLOUD_PASS from CircleCI context 'CIF Artifactory Cloud' into GitHub Repository secrets."
        echo "::error::Check: secret names are case-sensitive; remove leading/trailing spaces from values."
        exit 1
    fi
done

echo "Diagnostics: Artifactory credential check passed (401/403 not returned)."

mkdir -p "${HOME}/.m2"

# Write credentials directly — Maven ${env.*} in settings.xml is unreliable on some runners.
python3 <<'PY'
import os
from pathlib import Path
import xml.sax.saxutils as x

user = os.environ["ARTIFACTORY_CLOUD_USER"]
password = os.environ["ARTIFACTORY_CLOUD_PASS"]

settings = f"""<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <servers>
        <server>
            <id>maven-adobe-cif-release</id>
            <username>{x.escape(user)}</username>
            <password>{x.escape(password)}</password>
        </server>
        <server>
            <id>maven-adobe-cif-snapshot</id>
            <username>{x.escape(user)}</username>
            <password>{x.escape(password)}</password>
        </server>
    </servers>
    <profiles>
        <profile>
            <id>artifactory-cloud</id>
            <repositories>
                <repository>
                    <snapshots><enabled>false</enabled></snapshots>
                    <id>maven-adobe-cif-release</id>
                    <url>https://artifactory-uw2.adobeitc.com/artifactory/maven-adobe-cif-release</url>
                </repository>
                <repository>
                    <snapshots />
                    <id>maven-adobe-cif-snapshot</id>
                    <url>https://artifactory-uw2.adobeitc.com/artifactory/maven-adobe-cif-snapshot</url>
                </repository>
            </repositories>
            <pluginRepositories>
                <pluginRepository>
                    <snapshots><enabled>false</enabled></snapshots>
                    <id>maven-adobe-cif-release</id>
                    <url>https://artifactory-uw2.adobeitc.com/artifactory/maven-adobe-cif-release</url>
                </pluginRepository>
                <pluginRepository>
                    <snapshots />
                    <id>maven-adobe-cif-snapshot</id>
                    <url>https://artifactory-uw2.adobeitc.com/artifactory/maven-adobe-cif-snapshot</url>
                </pluginRepository>
            </pluginRepositories>
        </profile>
    </profiles>
    <activeProfiles>
        <activeProfile>artifactory-cloud</activeProfile>
    </activeProfiles>
</settings>
"""

Path(os.path.expanduser("~/.m2/settings.xml")).write_text(settings, encoding="utf-8")
print("Configured ~/.m2/settings.xml for Adobe Artifactory.")
PY
