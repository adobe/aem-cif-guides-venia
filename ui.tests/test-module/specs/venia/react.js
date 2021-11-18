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

const itif = condition => (condition ? it : it.skip);

describe('Venia React Components', () => {
    const venia_homepage = `${config.aem.author.base_url}/content/venia/us/en.html`;

    before(() => {
        // AEM Login
        browser.AEMForceLogout();
        browser.url(config.aem.author.base_url);
        browser.AEMLogin(config.aem.author.username, config.aem.author.password);
    });

    beforeEach(() => {
        // Set window size to desktop
        browser.setWindowSize(1280, 960);
    });

    it('should render the minicart component', () => {
        // Go to the Venia homepage
        browser.url(venia_homepage);

        // Check that cart trigger button is displayed
        const cartTrigger = $('.cmp-VeniaHeader__cartTrigger__trigger');
        expect(cartTrigger).toBeDisplayed();

        // Open minicart
        cartTrigger.click();
        const minicart = $('.cmp-VeniaMiniCart__miniCart__contents');
        expect(minicart).toBeDisplayed();
    });

    it('should redirect to homepage from address book page without login', () => {
        // Go to address book page
        browser.url(`${config.aem.author.base_url}/content/venia/us/en/my-account/address-book.html`);
        browser.waitUntil(() => browser.getUrl() === venia_homepage, {
            timeout: 5000,
            timeoutMsg: 'expected browser to navigate to homepage after 5s'
        });
    });

    it('should redirect to homepage from account info page without login', () => {
        // Go to address book page
        browser.url(`${config.aem.author.base_url}/content/venia/us/en/my-account/account-details.html`);
        browser.waitUntil(() => browser.getUrl() === venia_homepage, {
            timeout: 5000,
            timeoutMsg: 'expected browser to navigate to homepage after 5s'
        });
    });

    it('should redirect to homepage from order history page without login', () => {
        // Go to address book page
        browser.url(`${config.aem.author.base_url}/content/venia/us/en/my-account/order-history.html`);
        browser.waitUntil(() => browser.getUrl() === venia_homepage, {
            timeout: 5000,
            timeoutMsg: 'expected browser to navigate to homepage after 5s'
        });
    });

    itif(config.venia && config.venia.email && config.venia.password)(
        'should render the sign in component in the header',
        () => {
            // Go to the Venia homepage
            browser.url(venia_homepage);

            // Check sign in button
            const signInTrigger = $('.cmp-VeniaHeader__accountTrigger__trigger');
            expect(signInTrigger).toBeDisplayed();

            // Check sign in form
            signInTrigger.click();
            const signInForm = $('.cmp-VeniaSignIn__signIn__form');
            expect(signInForm).toBeDisplayed();

            $('input[autocomplete="email"]').setValue(config.venia.email);
            $('input[type="password"]').setValue(config.venia.password);

            $('button[type="submit"]').click();

            expect($('.cmp-VeniaAccountMenu__accountMenuItems__root')).toBeDisplayed();
        }
    );

    it('should render the address book component', () => {
        // Go to address book page
        browser.url(`${config.aem.author.base_url}/content/venia/us/en/my-account/address-book.html`);

        expect($('.cmp-VeniaAddressBookPage__addressBookPage__root')).toBeDisplayed();
    });

    it('should render the account details', () => {
        // Go to account details page
        browser.url(`${config.aem.author.base_url}/content/venia/us/en/my-account/account-details.html`);

        expect($('.cmp-VeniaAccountInformationPage__accountInformationPage__root')).toBeDisplayed();
    });

    it('should render the order history component', () => {
        // Go to order history page
        browser.url(`${config.aem.author.base_url}/content/venia/us/en/my-account/order-history.html`);

        expect($('.cmp-VeniaOrderHistoryPage__orderHistoryPage__root')).toBeDisplayed();
    });

    it('should render the cart page', () => {
        // Go to password cart page
        browser.url(`${config.aem.author.base_url}/content/venia/us/en/cart-details.html`);

        expect($('.cmp-VeniaCartPage__cartPage__root')).toBeDisplayed();
    });

    it('should render the checkout component', () => {
        // Go to checkout page
        browser.url(`${config.aem.author.base_url}/content/venia/us/en/checkout.html`);

        expect($('.cmp-VeniaCheckoutPage__checkoutPage__root')).toBeDisplayed();
    });
});
