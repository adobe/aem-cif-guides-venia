#!/usr/bin/env bash
#
# Copyright 2026 Adobe. All rights reserved.
# Licensed under the Apache License, Version 2.0
#
# Legacy entry point — GHA integration jobs use run-it-in-docker.sh instead.

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
exec bash "${repo_root}/.github/ci/run-integration-test-inner.sh"
