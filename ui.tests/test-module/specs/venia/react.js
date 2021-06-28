/*
 *  Copyright 2020 Adobe Systems Incorporated
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

describe('Venia React Components', () => {
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

    it('should render the minicart component', () => {
        // Go to the Venia homepage
        browser.url(venia_homepage);

        // Check that cart trigger button is displayed
        const cartTrigger = $('.cmp-CartTrigger__cartTrigger__root');
        expect(cartTrigger).toBeDisplayed();

        // Open minicart
        cartTrigger.click();
        const minicart = $('.cmp-Minicart__header__root');
        expect(minicart).toBeDisplayed();
    });

    it('should render the sign in component in the header', () => {
        // Go to the Venia homepage
        browser.url(venia_homepage);

        // Check sign in button
        const signInButton = $('.cmp-AccountContainer__accountTrigger__root');
        expect(signInButton).toBeDisplayed();

        // Check sign in form
        signInButton.click();
        const signInForm = $('.cmp-SignIn__signIn__form');
        expect(signInForm).toBeDisplayed();
    });

    it('should render the address book component', () => {
        // Go to address book page
        browser.url(`${config.aem.author.base_url}/content/venia/us/en/my-account/address-book.html`);

        expect($('.cmp-AddressBook__addressBook__root')).toBeDisplayed();
    });

    it('should render the account details', () => {
        // Go to account details page
        browser.url(`${config.aem.author.base_url}/content/venia/us/en/my-account/account-details.html`);

        expect($('.cmp-AccountDetails__accountDetails__messageText')).toBeDisplayed();
    });

    it('should render the password reset component', () => {
        // Go to password reset page
        browser.url(`${config.aem.author.base_url}/content/venia/us/en/reset-password.html?token=abc`);

        expect($('.cmp-ResetPassword__ResetPassword__root')).toBeDisplayed();
    });
});
