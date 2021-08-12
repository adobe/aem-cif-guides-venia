/*
 *  Copyright 2021 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

const config = require('../../lib/config');

describe('Venia Searchbar Component', () => {
    const venia_homepage = `${config.aem.author.base_url}/content/venia/us/en.html`;

    before(() => {
        // AEM Login
        browser.AEMForceLogout();
        browser.url(config.aem.author.base_url);
        browser.AEMLogin(config.aem.author.username, config.aem.author.password);

        // Setup GraphQL client if running on CircleCI
        if (process.env.CIRCLECI) {
            browser.configureGraphqlClient('com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl', {
                identifier: 'default',
                url: `${config.aem.author.base_url}/apps/cif-components-examples/graphql`,
                httpMethod: 'GET',
                acceptSelfSignedCertificates: 'true',
                allowHttpProtocol: 'true'
            });
            browser.pause(10000);
        }
    });

    beforeEach(() => {
        // Set window size to desktop
        browser.setWindowSize(1280, 960);
    });

    it('should render the searchbar', () => {
        // Go to the Venia homepage
        browser.url(venia_homepage);

        // Check that cart trigger button is displayed
        const searchBarTrigger = $('.searchbar__trigger');
        expect(searchBarTrigger).toBeDisplayed();
        // and the searchbar is hidden
        let searchBar = $('.searchbar__body');
        expect(searchBar).not.toBeDisplayed();

        searchBarTrigger.click();
        searchBar = $('.searchbar__body');
        expect(searchBar).toBeDisplayed();
        const input = searchBar.$('.searchbar__input');
        expect(input).toHaveAttr('placeholder', 'Search');
    });
});
