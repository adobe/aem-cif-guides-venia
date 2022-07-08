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
const { OnboardingDialogHandler } = require('../../lib/commons');

describe('Product Collection Component', function () {
    let onboardingHdler;

    before(() => {
        // Set window size to desktop
        browser.setWindowSize(1280, 960);

        // AEM Login
        browser.AEMForceLogout();
        browser.url(config.aem.author.base_url);
        browser.AEMLogin(config.aem.author.username, config.aem.author.password);

        // Enable helper to handle onboarding dialog popup
        onboardingHdler = new OnboardingDialogHandler(browser);
        onboardingHdler.enable();
    });

    after(function () {
        // Disable helper to handle onboarding dialog popup
        onboardingHdler.disable();
    });

    it('Closes an open product filter by clicking', () => {
        // perform a search
        browser.url(`${config.aem.author.base_url}/content/venia/us/en/search.html?search_query=dress`);

        const categoryFilterList = 'label[for="category_id"] ~ .productcollection__filter-items';

        // check category filter is closed
        const categoryFilter = $('label[for="category_id"]');
        expect(categoryFilter).toBeDisplayed();
        expect($(categoryFilterList)).not.toBeDisplayed();

        // open the category filter
        categoryFilter.click();
        expect($(categoryFilterList)).toBeDisplayed();

        // close the category filter
        categoryFilter.click();
        expect($(categoryFilterList)).not.toBeDisplayed();
    });

    it('Displays category filters', () => {
        // Accessories should have three category filters
        browser.url(
            `${config.aem.author.base_url}/content/venia/us/en/products/category-page.html/venia-accessories.html`
        );
        expect($('label[for="category_id"]')).toBeDisplayed();
        const categoryFilterItems = $$(
            'label[for="category_id"] ~ .productcollection__filter-items .productcollection__filter-item'
        );
        expect(categoryFilterItems.length).toBe(3);

        // Jewelry shouldn't have category filters
        browser.url(
            `${config.aem.author.base_url}/content/venia/us/en/products/category-page.html/venia-accessories/venia-jewelry.html`
        );
        expect($('label[for="category_id"]')).not.toBeDisplayed();
    });
});
