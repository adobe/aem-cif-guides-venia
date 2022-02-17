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
const { OnboardingDialogHandler, randomString } = require('../../lib/commons');

describe('Product Carousel Component Dialog', function () {
    const editor_page = `${config.aem.author.base_url}/editor.html`;

    let testing_page;
    let onboardingHdler;

    before(function () {
        // Set window size to desktop
        browser.setWindowSize(1280, 960);

        // AEM Login
        browser.AEMForceLogout();
        browser.url(config.aem.author.base_url);
        browser.AEMLogin(config.aem.author.username, config.aem.author.password);

        // Enable helper to handle onboarding dialog popup
        onboardingHdler = new OnboardingDialogHandler(browser);
        onboardingHdler.enable();

        const pageName = `testing-${randomString()}`;
        testing_page = `/content/venia/us/en/${pageName}`;
        browser.AEMCreatePage({
            title: 'Testing Page',
            name: pageName,
            parent: '/content/venia/us/en',
            template: '/conf/venia/settings/wcm/templates/page-content'
        });
        browser.pause(1000);
        addComponentToPage();
    });

    after(function () {
        // Disable helper to handle onboarding dialog popup
        if (onboardingHdler) {
            onboardingHdler.disable();

            // Clean up page
            browser.AEMDeletePage(testing_page);
        }
    });

    const addComponentToPage = (name = 'Product Carousel', group = 'Venia - Commerce') => {
        browser.url(`${editor_page}${testing_page}.html`);
        browser.AEMEditorLoaded();
        browser.EditorOpenSidePanel();

        // Open the Components tab
        $('coral-tab[title="Components"]').waitAndClick({ x: 1, y: 1 });

        // Filter for Commerce components
        $('#components-filter coral-select button').waitAndClick();
        browser.pause(200);
        $(`coral-selectlist-item[value="${group}"]`).waitAndClick();
        expect($('#components-filter coral-select [handle=label]')).toHaveText(group);

        // Drag and drop component on the page
        const carouselCmp = $(`div[data-title="${name}"]`);
        expect(carouselCmp).toBeDisplayed();
        const dropTarget = $(`div[data-path="${testing_page}/jcr:content/root/container/container/*"]`);
        carouselCmp.dragAndDrop(dropTarget, 1000);
    };

    const openComponentDialog = (
        node = 'productcarousel',
        trackingId = 'aem:sites:components:dialogs:cif-core-components:productcarousel:v1'
    ) => {
        // Open component dialog
        const cmpPlaceholder = $(`div[data-path="${testing_page}/jcr:content/root/container/container/${node}"]`);
        expect(cmpPlaceholder).toBeDisplayed();
        cmpPlaceholder.click();
        const configureButton = $('button[title="Configure"]');
        expect(configureButton).toBeDisplayed();
        configureButton.click();
        const dialog = $(`coral-dialog[trackingfeature="${trackingId}"]`);
        expect(dialog).toBeDisplayed();
        return dialog;
    };

    it('checks the component dialog', () => {
        let dialog = openComponentDialog();
        let header = dialog.$('coral-dialog-header');

        expect(header).toHaveText('Product Carousel');

        let fields = dialog.$$('.cq-dialog-content .coral-Form-fieldwrapper');

        // check fields
        expect(fields.length).toEqual(6);
        expect(fields[0].$('label')).toHaveText('Title');
        expect(fields[0].$('input[name="./jcr:title"]')).toExist();
        expect(fields[1].$('label')).toHaveText('The HTML tag used to display the title.');
        expect(fields[1].$('coral-select[name="./titleType"]')).toExist();
        expect(fields[2].$('label')).toHaveText('Selection Type');
        let productRadio = fields[2].$('coral-radio[name="./selectionType"][value="product"]');
        expect(productRadio).toExist();
        let categoryRadio = fields[2].$('coral-radio[name="./selectionType"][value="category"]');
        expect(categoryRadio).toExist();
        expect(fields[3].$('category-field')).toExist();
        expect(fields[3].$('input[name="./category"]')).toExist();
        expect(fields[4].$('input[name="./productCount"]')).toExist();
        expect(fields[5].$('label')).toHaveText('ID');
        expect(fields[5].$('input[name="./id"]')).toExist();

        let productField = dialog.$(
            '.cq-dialog-content coral-multifield[data-granite-coral-multifield-name="./product"]'
        );
        expect(productField).toExist();

        // check switching product / category selection
        expect(productRadio.$('input[type="radio"]')).toBeSelected();
        expect(productField).toBeDisplayed();

        expect(categoryRadio).toBeClickable();
        categoryRadio.click();

        expect(categoryRadio.$('input[type="radio"]')).toBeSelected();
        expect(productField).not.toBeDisplayed();
        expect(fields[3].$('label')).toBeDisplayed();
        expect(fields[3].$('label')).toHaveText('Category');
        expect(fields[4].$('label')).toBeDisplayed();
        expect(fields[4].$('label')).toHaveText('Number of displayed products');

        expect(productRadio).toBeClickable();
        productRadio.click();

        expect(productRadio.$('input[type="radio"]')).toBeSelected();
        expect(productField).toBeDisplayed();
        expect(fields[3].$('label')).not.toBeDisplayed();
        expect(fields[4].$('label')).not.toBeDisplayed();

        // close the dialog
        dialog.$('button[title="Cancel"]').click();
        expect(dialog.isExisting()).toBe(false);
    });
});
