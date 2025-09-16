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

'use strict';

const ci = new (require('./ci.js'))();
ci.context();
const qpPath = '/home/circleci/cq';
const buildPath = '/home/circleci/build';
const { TYPE, BROWSER, COMMERCE_ENDPOINT, VENIA_ACCOUNT_EMAIL, VENIA_ACCOUNT_PASSWORD } = process.env;

const updateGraphqlClientConfiguration = (pid, ranking = 100) => {
    if (!pid) {
        // create new configuration
        pid = encodeURIComponent('[Temporary PID replaced by real PID upon save]');
    } else {
        pid = 'com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl~' + pid;
    }

    ci.sh(`curl -v "http://localhost:4502/system/console/configMgr/${pid}" \
                -u "admin:admin" \
                -d "apply=true" \
                -d "factoryPid=com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl" \
                -d "propertylist=identifier,url,httpMethod,httpHeaders,service.ranking,cacheConfigurations" \
                -d "identifier=default" \
                -d "url=${COMMERCE_ENDPOINT}" \
                -d "httpMethod=GET" \
                -d "service.ranking=${ranking}" \
                -d "cacheConfigurations=venia/components/commerce/navigation:true:5:300" \
                -d "cacheConfigurations=com.adobe.cq.commerce.core.search.services.SearchFilterService:true:10:300" \
                -d "cacheConfigurations=venia/components/commerce/breadcrumb:true:1000:1000" \
                -d "cacheConfigurations=venia/components/commerce/product:true:50:1000" \
                -d "cacheConfigurations=venia/components/commerce/productlist:true:50:1000"
    `)
}

const updateGraphqlProxyServlet = () => {
    ci.sh(`curl -v "http://localhost:4502/system/console/configMgr/com.adobe.cq.cif.proxy.GraphQLProxyServlet" \
                -u "admin:admin" \
                -d "apply=true" \
                -d "propertylist=graphQLOriginUrl" \
                -d "graphQLOriginUrl=${COMMERCE_ENDPOINT}"
    `)
}

const configureCifCacheInvalidation = () => {
    // 1. Enable cache invalidation servlet (author only) - /bin/cif/invalidate-cache
    ci.sh(`curl -v "http://localhost:4502/system/console/configMgr/com.adobe.cq.cif.cacheinvalidation.internal.InvalidateCacheNotificationImpl" \
                -u "admin:admin" \
                -d "apply=true" \
                -d "factoryPid=com.adobe.cq.cif.cacheinvalidation.internal.InvalidateCacheNotificationImpl"
    `)
    
    // 2. Enable cache invalidation listener (both author and publish)
    ci.sh(`curl -v "http://localhost:4502/system/console/configMgr/com.adobe.cq.commerce.core.cacheinvalidation.internal.InvalidateCacheSupport" \
                -u "admin:admin" \
                -d "apply=true" \
                -d "factoryPid=com.adobe.cq.commerce.core.cacheinvalidation.internal.InvalidateCacheSupport" \
                -d "propertylist=enableDispatcherCacheInvalidation,dispatcherBasePathConfiguration,dispatcherUrlPathConfiguration,dispatcherBaseUrl" \
                -d "enableDispatcherCacheInvalidation=true" \
                -d "dispatcherBasePathConfiguration=/content/venia/([a-z]{2})/([a-z]{2}):/content/venia/$1/$2" \
                -d "dispatcherUrlPathConfiguration=productUrlPath:/products/product-page.html/(.+):/p/$1" \
                -d "dispatcherUrlPathConfiguration=categoryUrlPath:/products/category-page.html/(.+):/c/$1" \
                -d "dispatcherUrlPathConfiguration=productUrlPath-1:/products/product-page.html/(.+):/pp/$1" \
                -d "dispatcherUrlPathConfiguration=categoryUrlPath-1:/products/category-page.html/(.+):/cc/$1" \
                -d "dispatcherBaseUrl=http://localhost:80"
    `)
}



try {
    ci.stage("Integration Tests");
    let veniaVersion = ci.sh('mvn help:evaluate -Dexpression=project.version -q -DforceStdout', true);
    let cifVersion = ci.sh('mvn help:evaluate -Dexpression=core.cif.components.version -q -DforceStdout', true);
    let wcmVersion = ci.sh('mvn help:evaluate -Dexpression=core.wcm.components.version -q -DforceStdout', true);
    let classifier = process.env.AEM;

    ci.dir(qpPath, () => {
        // Connect to QP
        ci.sh('./qp.sh -v bind --server-hostname localhost --server-port 55555');

        let extras;
        if (classifier == 'classic') {
            // Download latest add-on for AEM 6.5 release from artifactory
            ci.sh(`mvn -s ${buildPath}/.circleci/settings.xml com.googlecode.maven-download-plugin:download-maven-plugin:1.6.3:artifact -Partifactory-cloud -DgroupId=com.adobe.cq.cif -DartifactId=commerce-addon-aem-650-all -Dversion=LATEST -Dtype=zip -DoutputDirectory=${buildPath}/dependencies -DoutputFileName=addon-650.zip`);
            extras = ` --install-file ${buildPath}/dependencies/addon-650.zip`;

            // The core components are already installed in the Cloud SDK
            extras += ` --bundle com.adobe.cq:core.wcm.components.all:${wcmVersion}:zip`;
            extras += ` --install-file ${buildPath}/classic/all/target/aem-cif-guides-venia.all-classic-${veniaVersion}.zip`

        } else if (classifier == 'cloud') {
            // Download latest add-on release from artifactory
            ci.sh(`mvn -s ${buildPath}/.circleci/settings.xml com.googlecode.maven-download-plugin:download-maven-plugin:1.6.3:artifact -Partifactory-cloud -DgroupId=com.adobe.cq.cif -DartifactId=cif-cloud-ready-feature-pkg -Dversion=LATEST -Dtype=far -Dclassifier=cq-commerce-addon-authorfar -DoutputDirectory=${buildPath}/dependencies -DoutputFileName=addon.far`);
            extras = ` --install-file ${buildPath}/dependencies/addon.far`;
            extras += ` --install-file ${buildPath}/all/target/aem-cif-guides-venia.all-${veniaVersion}.zip`
        }

        // Install SNAPSHOT or current version of CIF examples bundle
        if (cifVersion.endsWith('-SNAPSHOT')) {
            let jar = `core-cif-components-examples-bundle-${cifVersion}.jar`;
            extras += ` --install-file ${buildPath}/dependencies/aem-core-cif-components/examples/bundle/target/${jar}`;
        } else {
            extras += ` --bundle com.adobe.commerce.cif:core-cif-components-examples-bundle:${cifVersion}:jar`;
        }

        // Start CQ
        ci.sh(`./qp.sh -v start --id author --runmode author --port 4502 --qs-jar /home/circleci/cq/author/cq-quickstart.jar \
            --bundle org.apache.sling:org.apache.sling.junit.core:1.0.23:jar \
            ${extras} \
            --vm-options \\\"-Xmx1536m -XX:MaxPermSize=256m -Djava.awt.headless=true -javaagent:${process.env.JACOCO_AGENT}=destfile=crx-quickstart/jacoco-it.exec\\\"`);
    });

    // Configure GraphQL Endpoint for classic, in cloud the environment variable should be used directly
    if (classifier == 'classic') {
        // the configuration contained in venia may not yet be available so create a new one
        updateGraphqlClientConfiguration();
    } else {
        // update the existing default endpoint
        updateGraphqlClientConfiguration('default');
    }

    // Configure GraphQL Proxy
    updateGraphqlProxyServlet();
    
    // Configure CIF Cache Invalidation
    configureCifCacheInvalidation();

    // Run integration tests
    if (TYPE === 'integration') {
        let excludedCategory = classifier === 'classic' ? 'com.venia.it.category.IgnoreOn65' : 'com.venia.it.category.IgnoreOnCloud';
        ci.dir('it.tests', () => {
            ci.sh(`mvn clean verify -U -B -Plocal -Dexclude.category=${excludedCategory}`); // The -Plocal profile comes from the AEM archetype
        });
    }
    if (TYPE === 'selenium') {
        // Get version of ChromeDriver
        let chromedriver = ci.sh('chromedriver --version', true); // Returns something like ChromeDriver 80.0.3987.16 (320f6526c1632ad4f205ebce69b99a062ed78647-refs/branch-heads/3987@{#185})
        chromedriver = chromedriver.split(' ');
        chromedriver = chromedriver.length >= 2 ? chromedriver[1] : '';

        ci.dir('ui.tests', () => {
            ci.sh(`CHROMEDRIVER=${chromedriver} mvn test -U -B -Pui-tests-local-execution -DAEM_VERSION=${classifier} -DHEADLESS_BROWSER=true -DSELENIUM-BROWSER=${BROWSER} -DVENIA_ACCOUNT_EMAIL=${VENIA_ACCOUNT_EMAIL} -DVENIA_ACCOUNT_PASSWORD=${VENIA_ACCOUNT_PASSWORD}`);
        });
    }

    ci.dir(qpPath, () => {
        // Stop CQ
        ci.sh('./qp.sh -v stop --id author');
    });

} finally {
    // Copy tests results
    ci.sh('mkdir test-reports');
    if (TYPE === 'integration') {
        ci.sh('cp -r it.tests/target/failsafe-reports test-reports/it.tests');
        // Copy screenshots if they exist
        ci.sh('if [ -d "it.tests/screenshots" ]; then cp -r it.tests/screenshots test-reports/screenshots; fi');
    }
    if (TYPE === 'selenium') {
        ci.sh('cp -r ui.tests/test-module/reports test-reports/ui.tests');
    }

    // Always download logs from AEM container
    ci.sh('mkdir logs');
    ci.dir('logs', () => {
        // A webserver running inside the AEM container exposes the logs folder, so we can download log files as needed.
        ci.sh('curl -O -f http://localhost:3000/crx-quickstart/logs/error.log');
        ci.sh('curl -O -f http://localhost:3000/crx-quickstart/logs/stdout.log');
        ci.sh('curl -O -f http://localhost:3000/crx-quickstart/logs/stderr.log');
        ci.sh(`find . -name '*.log' -type f -size +32M -exec echo 'Truncating: ' {} \\; -execdir truncate --size 32M {} +`);
    });
}
