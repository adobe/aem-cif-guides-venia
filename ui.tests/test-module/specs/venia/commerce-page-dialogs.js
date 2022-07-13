/*
 *  Copyright 2022 Adobe Systems Incorporated
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

describe('Commerce Page Component Dialog', function () {
    const page_properties = `${config.aem.author.base_url}/mnt/overlay/wcm/core/content/sites/properties.html`;
    const landing_page = '/content/venia/us/en';
    const category_page = '/content/venia/us/en/products/category-page';
    const product_page = '/content/venia/us/en/products/product-page';
    const content_page = '/content/venia/us/en/cart-details';

    let onboardingHdler;

    this.retries(2);

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

    it('shows dialog extensions on landing page', () => {
        // Landing page properties
        browser.url(`${page_properties}?item=${encodeURIComponent(`${landing_page}`)}`);

        // Open Commerce tab
        let commerceTab = $('coral-tab-label=Commerce');
        expect(commerceTab).toBeClickable();
        commerceTab.click();

        let commercePanel = $('coral-panelstack > coral-panel[selected]');
        expect(commercePanel).toBeDisplayed();

        expect(commercePanel.$('.coral-Form-fieldset-legend=Commerce Pages')).toBeDisplayed();
        expect(commercePanel.$('.coral-Form-fieldset-legend=Associated Content')).not.toBeDisplayed();

        let fields = commercePanel.$$('.coral-Form-fieldwrapper');

        // check fields
        expect(fields.length).toEqual(5);
        expect(fields[0].$('label')).toHaveText('Product Page');
        expect(fields[0].$('foundation-autocomplete[name="./cq:cifProductPage"]')).toExist();
        expect(fields[1].$('label')).toHaveText('Category Page');
        expect(fields[1].$('foundation-autocomplete[name="./cq:cifCategoryPage"]')).toExist();
        expect(fields[2].$('label')).toHaveText('Search Results Page');
        expect(fields[2].$('foundation-autocomplete[name="./cq:cifSearchResultsPage"]')).toExist();
        expect(fields[3].$('label')).toHaveText('Address Book Page');
        expect(fields[3].$('foundation-autocomplete[name="./cq:cifAddressBookPage"]')).toExist();
        expect(fields[4].$('label')).toHaveText('My Account Page');
        expect(fields[4].$('foundation-autocomplete[name="./cq:cifMyAccountPage"]')).toExist();
    });

    it('shows dialog extensions on category page', () => {
        // Category page properties
        browser.url(`${page_properties}?item=${encodeURIComponent(`${category_page}`)}`);

        // Open Commerce tab
        let commerceTab = $('coral-tab-label=Commerce');
        expect(commerceTab).toBeClickable();
        commerceTab.click();

        let commercePanel = $('coral-panelstack > coral-panel[selected]');
        expect(commercePanel).toBeDisplayed();

        expect(commercePanel.$('.coral-Form-fieldset-legend=Commerce Settings')).toBeDisplayed();
        expect(commercePanel.$('.coral-Form-fieldset-legend=Associated Content')).not.toBeDisplayed();

        let fields = commercePanel.$$('.coral-Form-fieldwrapper');

        // check fields
        expect(fields.length).toEqual(2);
        const categoryField = fields[0].$('category-field');
        expect(categoryField).toExist();
        expect(categoryField).toHaveAttribute('trackingelement', 'category filter');
        expect(categoryField).toHaveAttribute('trackingfeature', 'aem:cif:specificcategorytemplate');
        expect(fields[0].$('coral-taglist[name="./selectorFilter"]')).toExist();
        expect(fields[1].$('coral-checkbox')).toExist();
        expect(fields[1].$('input[name="./includesSubCategories"]')).toExist();
    });

    it('shows dialog extensions on product page', () => {
        // Product page properties
        browser.url(`${page_properties}?item=${encodeURIComponent(`${product_page}`)}`);

        // Open Commerce tab
        let commerceTab = $('coral-tab-label=Commerce');
        expect(commerceTab).toBeClickable();
        commerceTab.click();

        let commercePanel = $('coral-panelstack > coral-panel[selected]');
        expect(commercePanel).toBeDisplayed();

        expect(commercePanel.$('.coral-Form-fieldset-legend=Commerce Settings')).toBeDisplayed();
        expect(commercePanel.$('.coral-Form-fieldset-legend=Associated Content')).not.toBeDisplayed();

        let fields = commercePanel.$$('.coral-Form-fieldwrapper');

        // check fields
        expect(fields.length).toEqual(3);
        const productField = fields[0].$('product-field');
        expect(productField).toExist();
        expect(productField).toHaveAttribute('trackingelement', 'product filter');
        expect(productField).toHaveAttribute('trackingfeature', 'aem:cif:specificproducttemplate');

        expect(fields[0].$('product-field')).toExist();
        expect(fields[0].$('coral-taglist[name="./selectorFilter"]')).toExist();
        const categoryField = fields[1].$('category-field');
        expect(categoryField).toExist();
        expect(categoryField).toHaveAttribute('trackingelement', 'category filter');
        expect(categoryField).toHaveAttribute('trackingfeature', 'aem:cif:specificproducttemplate');
        expect(fields[1].$('coral-taglist[name="./useForCategories"]')).toExist();
        expect(fields[2].$('coral-checkbox')).toExist();
        expect(fields[2].$('input[name="./includesSubCategories"]')).toExist();
    });

    it('shows dialog extensions on content page', () => {
        // Content page properties
        browser.url(`${page_properties}?item=${encodeURIComponent(`${content_page}`)}`);

        // Open Commerce tab
        let commerceTab = $('coral-tab-label=Commerce');
        expect(commerceTab).toBeClickable();
        commerceTab.click();

        let commercePanel = $('coral-panelstack > coral-panel[selected]');
        expect(commercePanel).toBeDisplayed();

        // Verify associated content fields
        expect(commercePanel.$('.coral-Form-fieldset-legend=Associated Content')).toBeDisplayed();

        let fieldset = commercePanel.$('.coral-Form-fieldset');
        expect(fieldset).toBeDisplayed();

        const productField = fieldset.$('product-field');
        expect(productField).toBeDisplayed();
        expect(productField).toHaveAttribute('trackingElement', 'associated product');
        expect(productField).toHaveAttribute('trackingFeature', 'aem:cif:associatedcontentpage');

        const categoryField = fieldset.$('category-field');
        expect(categoryField).toBeDisplayed();
        expect(categoryField).toHaveAttribute('trackingElement', 'associated category');
        expect(categoryField).toHaveAttribute('trackingFeature', 'aem:cif:associatedcontentpage');
    });
});
