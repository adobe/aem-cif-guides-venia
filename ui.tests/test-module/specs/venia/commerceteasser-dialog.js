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

describe('Commerce Teaser Component Dialog', function () {
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

    beforeEach(function () {
        openComponentDialog();
    });

    afterEach(function () {
        closeComponentDialog();
    });

    const addComponentToPage = (name = 'Commerce Teaser', group = 'Venia - Commerce') => {
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
        const cmp = $(`div[data-title="${name}"]`);
        expect(cmp).toBeDisplayed();
        const dropTarget = $(`div[data-path="${testing_page}/jcr:content/root/container/container/*"]`);
        cmp.dragAndDrop(dropTarget, 1000);
    };

    const openComponentDialog = (
        node = 'teaser',
        trackingId = 'aem:sites:components:dialogs:cif-core-components:teaser:v3'
    ) => {
        // Open edit dialog
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

    const closeComponentDialog = (trackingId = 'aem:sites:components:dialogs:cif-core-components:teaser:v3') => {
        const cancelButton = $('.cq-dialog-cancel');
        expect(cancelButton).toBeClickable();

        cancelButton.click();

        const dialog = $(`coral-dialog[trackingfeature="${trackingId}"]`);
        expect(dialog).not.toBeDisplayed();
    };

    it('checks the component dialog fields', () => {
        $('coral-tab-label=Link & Actions').click();
        expect($('label=Link')).toBeDisplayed();

        const fields = $$('.cq-dialog-content .coral-Form-fieldwrapper');

        expect(fields[1].$('coral-checkbox[name="./linkTarget"]')).toBeDisplayed();

        const actionsCheckBox = fields[2].$('coral-checkbox[name="./actionsEnabled"]');
        expect(actionsCheckBox).toBeDisplayed();
        expect(actionsCheckBox).not.toBeChecked();

        const actionsMultiField = fields[3].$('coral-multifield[data-granite-coral-multifield-name="./actions"]');
        expect(actionsMultiField).toBeDisplayed();

        const addActionButton = actionsMultiField.$('button[coral-multifield-add]');
        expect(addActionButton).toBeDisplayed();
        expect(addActionButton).toBeDisabled();

        expect(actionsCheckBox).toBeClickable();
        actionsCheckBox.click();

        expect(addActionButton).toBeEnabled();
        expect(addActionButton).toBeClickable();
        addActionButton.click();

        expect(actionsMultiField.$('label=Page')).toExist();
        expect(actionsMultiField.$('label=Category')).toExist();
        expect(actionsMultiField.$('label=Product')).toExist();

        const actionFields = actionsMultiField.$$('coral-multifield-item-content .coral-Form-fieldwrapper');

        expect(actionFields.length).toEqual(5);
        expect(actionFields[0].$('foundation-autocomplete[name="./actions/item0/link"]')).toExist();
        expect(actionFields[1].$('coral-checkbox[name="./actions/item0/./linkTarget"]')).toExist();
        expect(actionFields[2].$('product-field')).toExist();
        expect(actionFields[2].$('input[name="./actions/item0/productSku"]')).toExist();
        expect(actionFields[3].$('category-field')).toExist();
        expect(actionFields[3].$('input[name="./actions/item0/categoryId"]')).toExist();
        expect(actionFields[4].$('input[name="./actions/item0/text"]')).toExist();
    });

    it('enables / disables components in multifield', () => {
        const fields = $$('.cq-dialog-content .coral-Form-fieldwrapper');
        const actionsCheckBox = fields[2].$('coral-checkbox[name="./actionsEnabled"]');

        expect(actionsCheckBox).toBeDisplayed();
        expect(actionsCheckBox).not.toBeChecked();

        const actionsMultiField = fields[3].$('coral-multifield[data-granite-coral-multifield-name="./actions"]');
        expect(actionsMultiField).toBeDisplayed();

        const addActionButton = actionsMultiField.$('button[coral-multifield-add]');
        expect(addActionButton).toBeDisplayed();
        expect(addActionButton).toBeDisabled();

        expect(actionsCheckBox).toBeClickable();
        actionsCheckBox.click();

        expect(addActionButton).toBeEnabled();
        expect(addActionButton).toBeClickable();

        addActionButton.click();
        addActionButton.click();

        const checkFields = (parent, enabled) => {
            let actionFields = parent.$$('.coral-Form-fieldwrapper');
            expect(actionFields.length).toEqual(5);
            if (enabled) {
                expect(actionFields[0].$('foundation-autocomplete')).not.toHaveAttribute('disabled');
                expect(actionFields[1].$('coral-checkbox')).not.toHaveElementClass('is-disabled');
                expect(actionFields[2].$('product-field')).not.toHaveAttribute('disabled');
                expect(actionFields[3].$('category-field')).not.toHaveAttribute('disabled');
                expect(actionFields[4].$('input')).not.toHaveAttribute('disabled');
            } else {
                expect(actionFields[0].$('foundation-autocomplete')).toHaveAttribute('disabled');
                expect(actionFields[1].$('coral-checkbox')).toHaveElementClass('is-disabled');
                expect(actionFields[2].$('product-field')).toHaveAttribute('disabled', 'true');
                expect(actionFields[3].$('category-field')).toHaveAttribute('disabled', 'true');
                expect(actionFields[4].$('input')).toHaveAttribute('disabled');
            }
        };

        const itemContents = actionsMultiField.$$('coral-multifield-item-content');
        expect(itemContents.length).toEqual(2);
        itemContents.forEach(content => checkFields(content, true));

        actionsCheckBox.click();
        itemContents.forEach(content => checkFields(content, false));

        actionsCheckBox.click();
        itemContents.forEach(content => checkFields(content, true));
    });

    it('clears non-empty fields in multifield on new selection', () => {
        const fields = $$('.cq-dialog-content .coral-Form-fieldwrapper');
        const actionsCheckBox = fields[2].$('coral-checkbox[name="./actionsEnabled"]');
        const actionsMultiField = fields[3].$('coral-multifield[data-granite-coral-multifield-name="./actions"]');
        const addActionButton = actionsMultiField.$('button[coral-multifield-add]');

        actionsCheckBox.click();
        addActionButton.click();

        const actionFields = actionsMultiField.$$('coral-multifield-item-content .coral-Form-fieldwrapper');
        expect(actionFields.length).toEqual(5);

        browser.GraniteSelectPath(actionFields[0].$('foundation-autocomplete'), 'Venia Demo Store');
        expect(actionFields[0].$('input[name="./actions/item0/link"')).toHaveValue('/content/venia');

        browser.CIFSelectProduct(actionFields[2].$('product-field'), 'Agatha Skirt');
        expect(actionFields[2].$('input[name="./actions/item0/productSku"')).toHaveValue('VSK05');
        expect(actionFields[0].$('foundation-autocomplete input[name="./actions/item0/link"')).toHaveValue('');

        browser.CIFSelectCategory(actionFields[3].$('category-field'), 'Tops');
        expect(actionFields[3].$('input[name="./actions/item0/categoryId"')).toHaveValue('OA==');
        expect(actionFields[2].$('input[name="./actions/item0/productSku"')).toHaveValue('');

        browser.GraniteSelectPath(actionFields[0].$('foundation-autocomplete'), 'Venia Demo Store');
        expect(actionFields[0].$('input[name="./actions/item0/link"')).toHaveValue('/content/venia');
        expect(actionFields[3].$('input[name="./actions/item0/categoryId"')).toHaveValue('');
    });
});
