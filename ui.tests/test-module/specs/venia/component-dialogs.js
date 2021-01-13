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
const { OnboardingDialogHandler, randomString } = require('../../lib/commons');

describe('Category Carousel', () => {
    const editor_page = `${config.aem.author.base_url}/editor.html`;

    let testing_page;
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

    beforeEach(() => {
        const pageName = `testing-${randomString()}`;
        testing_page = `/content/venia/us/en/${pageName}`;
        browser.AEMCreatePage({
            title: 'Testing Page',
            name: pageName,
            parent: '/content/venia/us/en',
            template: '/conf/venia/settings/wcm/templates/page-content'
        });
        browser.pause(1000);
    });

    afterEach(() => {
        browser.AEMDeletePage(testing_page);
    });

    const addComponentToPage = (name, group = 'Venia - Commerce') => {
        browser.url(`${editor_page}${testing_page}.html`);
        browser.AEMEditorLoaded();
        browser.EditorOpenSidePanel();

        // Open the Components tab
        $('coral-tab[title="Components"]').waitAndClick({ x: 1, y: 1 });

        // Filter for Commerce components
        $('#components-filter coral-select').waitAndClick();
        $(`coral-selectlist-item[value="${group}"]`).waitAndClick();

        // Drag category carousel component on page
        const carouselCmp = $(`div[data-title="${name}"]`);
        expect(carouselCmp).toBeDisplayed();
        const dropTarget = $(`div[data-path="${testing_page}/jcr:content/root/container/container/*"]`);
        carouselCmp.dragAndDrop(dropTarget, 1000);
    };

    const openComponentDialog = (node, trackingId) => {
        // Open component dialog
        const cmpPlaceholder = $(
            `div[data-path="${testing_page}/jcr:content/root/container/container/${node}"]`
        );
        expect(cmpPlaceholder).toBeDisplayed();
        cmpPlaceholder.click();
        const configureButton = $('button[title="Configure"]');
        expect(configureButton).toBeDisplayed();
        configureButton.click();
        const dialog = $(`coral-dialog[trackingfeature="${trackingId}"]`);
        expect(dialog).toBeDisplayed();
    };

    it('opens the category carousel dialog', () => {
        addComponentToPage('Category Carousel');
        openComponentDialog('categorycarousel', 'aem:sites:components:dialogs:cif-core-components:categorycarousel:v1');
    });

    it('opens the commerce experience fragment dialog', () => {
        addComponentToPage('Commerce Experience Fragment');
        openComponentDialog('experiencefragment', 'aem:sites:components:dialogs:cif-core-components:experiencefragment:v1');
    });

    it('opens the commerce teaser dialog', () => {
        addComponentToPage('Commerce Teaser');
        openComponentDialog('teaser', 'aem:sites:components:dialogs:cif-core-components:teaser:v1');
    });

    it('opens the featured categories dialog', () => {
        addComponentToPage('Featured Categories');
        openComponentDialog('featuredcategorylist', 'aem:sites:components:dialogs:cif-core-components:featuredcategorylist:v1');
    });

    it('opens the product carousel dialog', () => {
        addComponentToPage('Product Carousel');
        openComponentDialog('productcarousel', 'aem:sites:components:dialogs:cif-core-components:productcarousel:v1');
    });

    it('opens the product teaser dialog', () => {
        addComponentToPage('Product Teaser');
        openComponentDialog('productteaser', 'aem:sites:components:dialogs:cif-core-components:productteaser:v1');
    });

    it('opens the product teaser dialog', () => {
        addComponentToPage('Related Products');
        openComponentDialog('relatedproducts', 'aem:sites:components:dialogs:cif-core-components:relatedproducts:v1');
    });

});