#!/usr/bin/env python3
#
# Copyright 2026 Adobe. All rights reserved.
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License. You may obtain
# a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
#
# Renders a CircleCI-style test report into the GitHub Actions Job Summary.
#
# GitHub has no native "Tests" tab, so this parses the JUnit XML copied into
# test-reports/ (Failsafe TEST-*.xml for integration jobs, WDIO results-*.xml for
# selenium jobs) and writes a small Markdown report to $GITHUB_STEP_SUMMARY:
#   - an overview table (Total / Passed / Failed / Skipped)
#   - the list of failed test names, plus a link to the uploaded *-reports
#     artifact where the full stack traces and AEM error.log can be analyzed.
# Stack traces and server logs are intentionally NOT inlined here — keeping the
# summary small mirrors how core-cif-components (CircleCI) surfaces results.
#
# The failsafe-summary.xml aggregate file is ignored (its root is not a testsuite).

import glob
import os
import xml.etree.ElementTree as ET

# Hard ceiling on the whole document, leaving headroom under GitHub's ~1 MB limit.
MAX_TOTAL_CHARS = 900000

job = os.environ.get("GITHUB_JOB", "tests")
summary_path = os.environ.get("GITHUB_STEP_SUMMARY")

xml_files = sorted(glob.glob("test-reports/**/*.xml", recursive=True))

total = passed = skipped = 0
failed = []  # list of dicts: classname, name


for path in xml_files:
    try:
        root = ET.parse(path).getroot()
    except ET.ParseError:
        continue
    if root.tag == "testsuites":
        suites = root.findall("testsuite")
    elif root.tag == "testsuite":
        suites = [root]
    else:
        # e.g. failsafe-summary.xml — not a test suite, skip it.
        continue

    for suite in suites:
        for case in suite.findall("testcase"):
            total += 1
            fail = case.find("failure")
            err = case.find("error")
            skip = case.find("skipped")
            problem = fail if fail is not None else err
            if problem is not None:
                failed.append(
                    {
                        "classname": case.get("classname", ""),
                        "name": case.get("name", ""),
                    }
                )
            elif skip is not None:
                skipped += 1
            else:
                passed += 1

out = []
out.append("## 🧪 {} — Test Results\n".format(job))

if not xml_files:
    out.append(
        "> ⚠️ **No JUnit XML reports found.** The tests almost certainly did not "
        "run — this is an environment/startup failure, not a test failure. "
        "Check the `run-containerized-test.sh` step log and the AEM `error.log` "
        "in the uploaded `*-reports` artifact.\n"
    )
else:
    out.append("| Total | ✅ Passed | ❌ Failed | ⏭️ Skipped |")
    out.append("|------:|----------:|----------:|-----------:|")
    out.append("| {} | {} | {} | {} |\n".format(total, passed, len(failed), skipped))

    if failed:
        # Keep the summary small: just list which tests failed. Full stack traces
        # and the captured AEM error.log live in the uploaded *-reports artifact —
        # link to it so the details are one click away ("analyze"), not inlined.
        out.append("### ❌ Failed tests ({})\n".format(len(failed)))
        for tc in failed:
            name = tc["name"] or "(unnamed)"
            if tc["classname"]:
                out.append("- `{}` — `{}`".format(name, tc["classname"]))
            else:
                out.append("- `{}`".format(name))
        out.append("")

        artifact = "{}-reports".format(job)
        server = os.environ.get("GITHUB_SERVER_URL", "https://github.com")
        repo = os.environ.get("GITHUB_REPOSITORY", "")
        run_id = os.environ.get("GITHUB_RUN_ID", "")
        if repo and run_id:
            run_url = "{}/{}/actions/runs/{}".format(server, repo, run_id)
            out.append(
                "🔎 Download the **{}** artifact from the [workflow run]({}) to see "
                "full stack traces and the AEM `error.log`.\n".format(artifact, run_url)
            )
        else:
            out.append(
                "🔎 Download the **{}** artifact to see full stack traces and the "
                "AEM `error.log`.\n".format(artifact)
            )
    else:
        out.append("All tests passed. 🎉\n")

report = "\n".join(out)
if len(report) > MAX_TOTAL_CHARS:
    report = report[:MAX_TOTAL_CHARS] + "\n\n> …(summary truncated to fit GitHub's size limit)…\n"

# Write to the Job Summary page (rendered as Markdown) and echo to the step log.
if summary_path:
    with open(summary_path, "a", encoding="utf-8") as fh:
        fh.write(report + "\n")
print(report)
