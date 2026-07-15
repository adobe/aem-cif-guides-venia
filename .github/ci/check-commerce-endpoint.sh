#!/usr/bin/env bash
#
# Copyright 2026 Adobe. All rights reserved.
# Licensed under the Apache License, Version 2.0
#
# Diagnostic-only: hits COMMERCE_ENDPOINT directly, before AEM ever configures its
# GraphqlClientImpl against it, so the CI log shows exactly what the endpoint returns.
# Every navigation/search/category IT test has been failing with "Expected BEGIN_OBJECT
# but was STRING" from every single GraphQL query — the signature of the endpoint
# returning a bare error string instead of a real {"data": ...} response. This script
# never fails the build; it only logs diagnostics so a bad URL/token can be told apart
# from any other cause.

set -uo pipefail

echo "Diagnostics: COMMERCE_ENDPOINT set=$([[ -n "${COMMERCE_ENDPOINT:-}" ]] && echo yes || echo no)"
echo "Diagnostics: COMMERCE_ENDPOINT length=${#COMMERCE_ENDPOINT}"
echo "Diagnostics: COMMERCE_INTEGRATION_TOKEN set=$([[ -n "${COMMERCE_INTEGRATION_TOKEN:-}" ]] && echo yes || echo no)"
echo "Diagnostics: COMMERCE_INTEGRATION_TOKEN length=${#COMMERCE_INTEGRATION_TOKEN}"

if [[ -z "${COMMERCE_ENDPOINT:-}" ]]; then
    echo "::warning::COMMERCE_ENDPOINT is empty — every GraphQL-backed IT/selenium test will fail."
    exit 0
fi

# Flag common copy/paste mistakes without ever printing the value itself.
trimmed="$(printf '%s' "${COMMERCE_ENDPOINT}" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')"
echo "Diagnostics: COMMERCE_ENDPOINT has leading/trailing whitespace=$([[ "${trimmed}" != "${COMMERCE_ENDPOINT}" ]] && echo yes || echo no)"
echo "Diagnostics: COMMERCE_ENDPOINT contains a literal quote character=$([[ "${COMMERCE_ENDPOINT}" == *'"'* || "${COMMERCE_ENDPOINT}" == *"'"* ]] && echo yes || echo no)"
echo "Diagnostics: COMMERCE_ENDPOINT starts with http(s)=$([[ "${COMMERCE_ENDPOINT}" =~ ^https?:// ]] && echo yes || echo no)"

query='{"query":"{ __typename }"}'
body_file="$(mktemp)"

http_code="$(curl -sS -o "${body_file}" -w '%{http_code}' \
    -X POST "${COMMERCE_ENDPOINT}" \
    -H 'Content-Type: application/json' \
    -d "${query}" 2>&1 || echo "curl_failed")"
echo "Diagnostics: POST (no auth header) COMMERCE_ENDPOINT -> HTTP ${http_code}"
echo "Diagnostics: response body (first 300 chars): $(head -c 300 "${body_file}")"

if [[ -n "${COMMERCE_INTEGRATION_TOKEN:-}" ]]; then
    http_code_auth="$(curl -sS -o "${body_file}" -w '%{http_code}' \
        -X POST "${COMMERCE_ENDPOINT}" \
        -H 'Content-Type: application/json' \
        -H "Authorization: Bearer ${COMMERCE_INTEGRATION_TOKEN}" \
        -d "${query}" 2>&1 || echo "curl_failed")"
    echo "Diagnostics: POST (with Bearer token) COMMERCE_ENDPOINT -> HTTP ${http_code_auth}"
    echo "Diagnostics: response body (first 300 chars): $(head -c 300 "${body_file}")"
fi

rm -f "${body_file}"
