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

# Break the URL into parts so we can spot scheme/host/path/trailing-slash issues at a
# glance (e.g. http vs https, apex vs www, a path that isn't /graphql). GitHub masks
# whatever matches the secret; the shape is still informative.
scheme="${COMMERCE_ENDPOINT%%://*}"
rest="${COMMERCE_ENDPOINT#*://}"
host="${rest%%/*}"
path="/${rest#*/}"
[[ "${rest}" != */* ]] && path="(none)"
echo "Diagnostics: COMMERCE_ENDPOINT scheme=${scheme}"
echo "Diagnostics: COMMERCE_ENDPOINT host=${host}"
echo "Diagnostics: COMMERCE_ENDPOINT path=${path}"
echo "Diagnostics: COMMERCE_ENDPOINT path ends with slash=$([[ "${path}" == */ ]] && echo yes || echo no)"
echo "Diagnostics: COMMERCE_ENDPOINT path ends with /graphql=$([[ "${path}" == */graphql ]] && echo yes || echo no)"

# DNS reachability of the host, informational only — the HTTP probes below are the
# authoritative signal (getent may be unavailable even when curl works via a proxy).
if getent hosts "${host%%:*}" >/dev/null 2>&1; then
    echo "Diagnostics: host DNS resolves=yes ($(getent hosts "${host%%:*}" | awk '{print $1}' | paste -sd, -))"
else
    echo "Diagnostics: host DNS resolves=no (getent) — see HTTP probes below for the real result"
fi

query='{"query":"{ __typename }"}'
body_file="$(mktemp)"
head_file="$(mktemp)"

# Reusable probe: prints status, key response headers, timing, curl error (if any) and a
# body snippet. -D dumps response headers so the Location/Server/Content-Type are visible
# — Location is the definitive redirect target. -w exposes curl's own view (effective URL,
# redirect target, timings) even when the server sends no body.
probe() {
    local label="$1"; shift
    : >"${body_file}"; : >"${head_file}"
    local w
    w="$(curl -sS -o "${body_file}" -D "${head_file}" \
        -w 'HTTPCODE=%{http_code} REDIRECT=%{redirect_url} EFFECTIVE=%{url_effective} CONTENTTYPE=%{content_type} TIME=%{time_total}s SIZE=%{size_download}b' \
        "$@" 2>"${body_file}.err" || echo "HTTPCODE=curl_failed")"
    echo "----- probe: ${label} -----"
    echo "Diagnostics: ${w}"
    if [[ -s "${body_file}.err" ]]; then
        echo "Diagnostics: curl stderr: $(head -c 300 "${body_file}.err")"
    fi
    echo "Diagnostics: response headers:"
    # Only the interesting headers, to keep the log readable.
    grep -iE '^(HTTP/|location:|server:|content-type:|content-length:|www-authenticate:|x-magento|cf-ray|via:|set-cookie:)' "${head_file}" | sed 's/^/    /' || true
    echo "Diagnostics: response body (first 400 chars): $(head -c 400 "${body_file}")"
    rm -f "${body_file}.err"
}

# 1) POST without -L — reproduces exactly what GraphqlClientImpl sees (it never follows 3xx).
probe "POST, no auth, no -L (what the GraphQL client actually sees)" \
    -X POST "${COMMERCE_ENDPOINT}" -H 'Content-Type: application/json' -d "${query}"
http_code="$(grep -m1 '^HTTP/' "${head_file}" | awk '{print $2}')"

# 2) GET without -L — the production config uses "httpMethod": "GET", so probe that too.
probe "GET, no auth, no -L (production config uses GET)" \
    -X GET "${COMMERCE_ENDPOINT}?query=%7B__typename%7D"

# 3) If it redirects, follow it and confirm the target returns real JSON. The Location
#    header from probe #1 plus REDIRECT= above is the value to put in the secret.
if [[ "${http_code:-}" =~ ^3[0-9][0-9]$ ]]; then
    echo "::warning::COMMERCE_ENDPOINT returned HTTP ${http_code} (redirect). GraphqlClientImpl does not follow redirects, so every query fails with 'Expected BEGIN_OBJECT but was STRING'. Set the COMMERCE_ENDPOINT secret to the Location/REDIRECT target shown above."
    probe "POST, no auth, WITH -L (following the redirect chain)" \
        -L -X POST "${COMMERCE_ENDPOINT}" -H 'Content-Type: application/json' -d "${query}"
fi

# 4) With the integration token, to distinguish an auth problem from a URL problem.
if [[ -n "${COMMERCE_INTEGRATION_TOKEN:-}" ]]; then
    probe "POST, WITH Bearer token, no -L" \
        -X POST "${COMMERCE_ENDPOINT}" -H 'Content-Type: application/json' \
        -H "Authorization: Bearer ${COMMERCE_INTEGRATION_TOKEN}" -d "${query}"
fi

rm -f "${body_file}" "${head_file}"
