/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
 *    This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License. You may obtain a copy
 *    of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under
 *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *    OF ANY KIND, either express or implied. See the License for the specific language
 *    governing permissions and limitations under the License.
 *
 ******************************************************************************/

"use strict";

const e = require("child_process");
const fs = require("fs");

module.exports = class CI {
    /**
     * Print build context to stdout.
     */
    context() {
        this.sh("java -version");
        this.sh("mvn -v");
        console.log("Node version: %s", process.version);
        this.sh('printf "NPM version: $(npm --version)"', false, false);
    }

    /**
     * Switch working directory for the scope of the given function.
     */
    dir(dir, func) {
        let currentDir = process.cwd();
        process.chdir(dir);
        console.log("// Changed directory to: " + process.cwd());
        try {
            func();
        } finally {
            process.chdir(currentDir);
            console.log("// Changed directory back to: " + currentDir);
        }
    }

    /**
     * Checkout git repository with the given branch into the given folder.
     */
    checkout(repo, branch = "main", folder = "") {
        this.sh("git clone -b " + branch + " " + repo + " " + folder);
    }

    /**
     * Run shell command and attach to process stdio.
     */
    sh(command, returnStdout = false, print = true) {
        if (print) {
            console.log(command);
        }
        if (returnStdout) {
            return e.execSync(command).toString().trim();
        }
        return e.execSync(command, { stdio: "inherit" });
    }

    /**
     * Return value of given environment variable.
     */
    env(key) {
        return process.env[key];
    }

    /**
     * Print stage name.
     */
    stage(name) {
        console.log(
            "\n------------------------------\n" +
                "--\n" +
                "-- %s\n" +
                "--\n" +
                "------------------------------\n",
            name
        );
    }

    /**
     * Configure a git impersonation for the scope of the given function.
     */
    gitImpersonate(user, mail, func) {
        try {
            this.sh(
                "git config --local user.name " +
                    user +
                    " && git config --local user.email " +
                    mail,
                false,
                false
            );
            func();
        } finally {
            this.sh(
                "git config --local --unset user.name && git config --local --unset user.email",
                false,
                false
            );
        }
    }

    /**
     * Configure git credentials for the scope of the given function.
     */
    gitCredentials(repo, func) {
        try {
            this.sh(
                "git config credential.helper 'store --file .git-credentials'"
            );
            fs.writeFileSync(".git-credentials", repo);
            console.log("// Created file .git-credentials.");
            func();
        } finally {
            this.sh("git config --unset credential.helper");
            fs.unlinkSync(".git-credentials");
            console.log("// Deleted file .git-credentials.");
        }
    }

    /**
     * Writes given content to a file.
     */
    writeFile(fileName, content) {
        console.log(`// Write to file ${fileName}`);
        fs.writeFileSync(fileName, content, { encoding: "utf8" });
    }
};
