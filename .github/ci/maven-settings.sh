#!/usr/bin/env bash
#
# Copyright 2026 Adobe. All rights reserved.
# Licensed under the Apache License, Version 2.0
#
# Generates ~/.m2/settings.xml with Adobe Artifactory (cloud) credentials.
#
# Used by: the integration-test build (it-tests.js), which resolves the CIF commerce
# add-on artifacts from Adobe Artifactory via the maven-download-plugin
# (-Partifactory-cloud) and therefore needs authenticated <server> entries in ~/.m2.
#
# Why generate it here instead of committing a settings.xml that reads
# ${env.ARTIFACTORY_CLOUD_USER/PASS}: Maven's ${env.*} interpolation inside <server>
# credentials is not resolved reliably on all runners, so the secret values are inlined
# into the generated file instead.

set -euo pipefail

if [[ -z "${ARTIFACTORY_CLOUD_USER:-}" || -z "${ARTIFACTORY_CLOUD_PASS:-}" ]]; then
    echo "::error::Missing ARTIFACTORY_CLOUD_USER or ARTIFACTORY_CLOUD_PASS repository secrets."
    exit 1
fi

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

        <profile>
            <id>central</id>
            <activation>
                <activeByDefault>true</activeByDefault>
            </activation>
        </profile>
    </profiles>
</settings>
"""

Path(os.path.expanduser("~/.m2/settings.xml")).write_text(settings, encoding="utf-8")
print("Configured ~/.m2/settings.xml for Adobe Artifactory.")
PY
