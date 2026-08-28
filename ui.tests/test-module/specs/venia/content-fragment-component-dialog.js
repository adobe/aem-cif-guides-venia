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
const { OnboardingDialogHandler, randomString, getAuthenticatedRequestOptions } = require('../../lib/commons');
const request = require('request-promise');
const url = require('url');

describe('Commerce Content Fragment Component Dialog', function () {
    const editor_page = `${config.aem.author.base_url}/editor.html`;

    const conteFragmentModelJson = `{
        "jcr:primaryType": "cq:Template",
        "allowedPaths": [
            "/content/entities(/.*)?"
        ],
        "ranking": 100,
        "jcr:content": {
            "jcr:primaryType": "cq:PageContent",
            "jcr:title": "Testing Model",
            "status": "enabled",
            "sling:resourceSuperType": "dam/cfm/models/console/components/data/entity",
            "cq:scaffolding": "/conf/core-components-examples/settings/dam/cfm/models/product-specs/jcr:content/model",
            "cq:templateType": "/libs/settings/dam/cfm/model-types/fragment",
            "sling:resourceType": "dam/cfm/models/console/components/data/entity/default",
            "metadata": {
                "jcr:primaryType": "nt:unstructured"
            },
            "model": {
                "jcr:primaryType": "cq:PageContent",
                "dataTypesConfig": "/mnt/overlay/settings/dam/cfm/models/formbuilderconfig/datatypes",
                "sling:resourceType": "wcm/scaffolding/components/scaffolding",
                "maxGeneratedOrder": "20",
                "cq:targetPath": "/content/entities",
                "cq:dialog": {
                    "jcr:primaryType": "nt:unstructured",
                    "sling:resourceType": "cq/gui/components/authoring/dialog",
                    "content": {
                        "jcr:primaryType": "nt:unstructured",
                        "sling:resourceType": "granite/ui/components/coral/foundation/fixedcolumns",
                        "items": {
                            "jcr:primaryType": "nt:unstructured",
                            "maxGeneratedOrder": "22",
                            "1621410803663": {
                                "jcr:primaryType": "nt:unstructured",
                                "listOrder": "21",
                                "valueType": "string",
                                "showEmptyInReadOnly": "true",
                                "metaType": "product-reference",
                                "required": "on",
                                "name": "productSku",
                                "fieldLabel": "Product SKU",
                                "sling:resourceType": "cif/cfm/admin/components/productreference",
                                "renderReadOnly": "false"
                            },
                            "1621410824776": {
                                "jcr:primaryType": "nt:unstructured",
                                "listOrder": "22",
                                "valueType": "string/multiline",
                                "showEmptyInReadOnly": "true",
                                "metaType": "text-multi_ft_toggle",
                                "cfm-element": "Product Specs",
                                "name": "productSpecs",
                                "checked": "false",
                                "sling:resourceType": "dam/cfm/admin/components/authoring/contenteditor/multieditor",
                                "renderReadOnly": "false",
                                "default-mime-type": "text/html"
                            }
                        }
                    }
                }
            }
        }
    }`;

    let testing_page;
    let testing_model1;
    let testing_model2;
    let onboardingHdler;

    before(function () {
        // Only run this test suite for AEM cloud
        if (config.aem.type === 'classic' || config.aem.type === 'lts') {
            this.skip();
        }

        // Set window size to desktop
        browser.setWindowSize(1280, 960);

        // AEM Login
        browser.AEMForceLogout();
        browser.url(config.aem.author.base_url);
        browser.AEMLogin(config.aem.author.username, config.aem.author.password);

        // Enable helper to handle onboarding dialog popup
        onboardingHdler = new OnboardingDialogHandler(browser);
        onboardingHdler.enable();

        // Create the models
        let modelName = `model-${randomString()}`;
        const modelsPath = '/conf/venia/settings/dam/cfm/models';
        testing_model1 = modelsPath + '/' + modelName;

        let options = getAuthenticatedRequestOptions(browser);
        Object.assign(options, {
            formData: {
                ':operation': 'import',
                ':contentType': 'json',
                ':name': modelName,
                ':content': conteFragmentModelJson
            }
        });
        request.post(url.resolve(config.aem.author.base_url, modelsPath), options);

        modelName = `model-${randomString()}`;
        testing_model2 = modelsPath + '/' + modelName;

        options = getAuthenticatedRequestOptions(browser);
        Object.assign(options, {
            formData: {
                ':operation': 'import',
                ':contentType': 'json',
                ':name': modelName,
                ':content': conteFragmentModelJson
            }
        });
        request.post(url.resolve(config.aem.author.base_url, modelsPath), options);
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

            // Clean up models and page
            browser.AEMDeletePage(testing_model1);
            browser.AEMDeletePage(testing_model2);
            browser.AEMDeletePage(testing_page);
        }
    });

    const addComponentToPage = (name = 'Commerce Content Fragment', group = 'Venia - Commerce') => {
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

        // Drag category carousel component on page
        const carouselCmp = $(`div[data-title="${name}"]`);
        expect(carouselCmp).toBeDisplayed();
        const dropTarget = $(`div[data-path="${testing_page}/jcr:content/root/container/container/*"]`);
        carouselCmp.dragAndDrop(dropTarget, 1000);
    };

    const openComponentDialog = (
        node = 'contentfragment',
        trackingId = 'aem:sites:components:dialogs:cif-core-components:contentfragment:v1'
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

    // Returns the currently selected panel of the dialog (the Properties tab), so fields can be
    // located by their stable name attribute rather than by position/count (which changes between
    // CIF Core Component releases).
    const propertiesPanel = dialog => dialog.$('coral-tabview coral-panelstack coral-panel.is-selected');

    it('opens the initial empty dialog', () => {
        let dialog = openComponentDialog();
        let header = dialog.$('coral-dialog-header');

        expect(header).toHaveText('Commerce Content Fragment');
        let tabs = dialog.$$('coral-tab');
        expect(tabs.length).toEqual(2);
        expect(tabs[0]).toHaveText('Properties');
        expect(tabs[0]).toBeDisplayed();
        expect(tabs[1]).toHaveAttr('hidden');

        // initial empty state: the expected fields are present
        expect(propertiesPanel(dialog).$('coral-select[name="./modelPath"]')).toBeDisplayed();
        expect(propertiesPanel(dialog).$('coral-select[name="./linkElement"]')).toHaveAttr('disabled');
        expect(propertiesPanel(dialog).$('foundation-autocomplete[name="./parentPath"]')).toBeDisplayed();
        expect(propertiesPanel(dialog).$('input[name="./id"]')).toBeExisting();
        expect(dialog.$('coral-radio[value="multi"][checked]')).toBeDisplayed();

        // select singleText display mode: the single element selector appears
        dialog.$('coral-radio[value="singleText"]').click();
        propertiesPanel(dialog).$('coral-select[name="./elementNames"]').waitForDisplayed();
        expect(tabs[1]).toBeDisplayed();
        expect(tabs[1]).toHaveText('Paragraph Control');

        // check paragraph control defaults
        tabs[1].click();
        expect(dialog.$('coral-panel[selected] coral-radio[name="./paragraphScope"][checked] input')).toHaveValue(
            'all'
        );
        expect(dialog.$('coral-panel[selected] input[name="./paragraphRange"]')).toBeDisabled();
        expect(dialog.$('coral-panel[selected] input[name="./paragraphHeadings"]')).toBeDisabled();

        // select multi display mode: the elements multifield appears
        tabs[0].click();
        dialog.$('coral-radio[value="multi"]').click();
        propertiesPanel(dialog).$('button[coral-multifield-add]').waitForDisplayed();
        expect(tabs[1]).toHaveAttr('hidden');

        // close the dialog
        dialog.$('button.cq-dialog-cancel[variant="default"]').waitAndClick();
        expect(dialog.isExisting()).toBe(false);
    });

    it('saves data in multi display mode', () => {
        let dialog = openComponentDialog();

        // select model
        propertiesPanel(dialog).$('coral-select[name="./modelPath"]').click();
        expect($('coral-popover.is-open')).toBeDisplayed();
        let model = $(`coral-popover.is-open coral-selectlist-item[value="${testing_model1}"]`);
        expect(model).toBeClickable();
        model.click();

        // link element becomes enabled once a model is selected
        expect(propertiesPanel(dialog).$('coral-select[name="./linkElement"]')).toBeEnabled();

        // try saving with empty mandatory field
        let doneButton = dialog.$('button.cq-dialog-submit[variant="primary"]');
        expect(doneButton).toBeClickable();
        doneButton.click();
        // check error highlighting
        expect(propertiesPanel(dialog).$('coral-select[name="./linkElement"]')).toHaveAttr('invalid');

        // select value for linkElement
        propertiesPanel(dialog).$('coral-select[name="./linkElement"]').click();
        expect($('coral-popover.is-open')).toBeDisplayed();
        let linkElement = $('coral-popover.is-open coral-selectlist-item[value="productSku"]');
        expect(linkElement).toBeClickable();
        linkElement.click();

        // select parentPath
        let pickerButton = propertiesPanel(dialog).$(
            'foundation-autocomplete[name="./parentPath"] button[title="Open Selection Dialog"]'
        );
        expect(pickerButton).toBeClickable();
        pickerButton.click();
        let pickerDialog = $('coral-dialog.foundation-picker-collection[open]');
        expect(pickerDialog).toBeDisplayed();
        pickerDialog
            .$('coral-columnview-item[data-foundation-collection-item-id="/content/dam/venia"] coral-checkbox')
            .waitAndClick();
        let selectButton = pickerDialog.$('button.granite-pickerdialog-submit');
        expect(selectButton).toBeEnabled();
        selectButton.waitAndClick();
        expect(pickerDialog.isExisting()).toBe(false);
        expect(
            propertiesPanel(dialog).$('foundation-autocomplete[name="./parentPath"] input[name="./parentPath"]')
        ).toHaveValue('/content/dam/venia');

        // select elements to be displayed
        let addButton = propertiesPanel(dialog).$('button[coral-multifield-add]');
        expect(addButton).toBeClickable();
        addButton.click();

        let elementSelect = propertiesPanel(dialog).$(
            'coral-multifield-item[aria-posinset="1"] coral-select[name="./elementNames"]'
        );
        expect(elementSelect).toBeDisplayed();
        expect(elementSelect).toBeClickable();
        elementSelect.click();

        expect($('coral-popover.is-open')).toBeDisplayed();
        let element = $('coral-popover.is-open coral-selectlist-item[value="productSku"]');
        expect(element).toBeClickable();
        element.click();

        addButton.click();

        elementSelect = propertiesPanel(dialog).$(
            'coral-multifield-item[aria-posinset="2"] coral-select[name="./elementNames"]'
        );
        expect(elementSelect).toBeDisplayed();
        expect(elementSelect).toBeClickable();
        elementSelect.click();

        expect($('coral-popover.is-open')).toBeDisplayed();
        element = $('coral-popover.is-open coral-selectlist-item[value="productSpecs"]');
        expect(element).toBeClickable();
        element.click();

        propertiesPanel(dialog).$('input[name="./id"]').setValue('anId');

        // save the setting and close the dialog
        doneButton.click();
        browser.waitUntil(function () {
            return !dialog.isExisting();
        });

        // re-open dialog
        dialog = openComponentDialog();

        // check the fields
        expect(propertiesPanel(dialog).$('input[name="./modelPath"]')).toHaveValue(testing_model1);
        expect(propertiesPanel(dialog).$('coral-select[name="./linkElement"]')).toBeEnabled();
        expect(propertiesPanel(dialog).$('input[name="./linkElement"]')).toHaveValue('productSku');
        expect(
            propertiesPanel(dialog).$('foundation-autocomplete[name="./parentPath"] input[name="./parentPath"]')
        ).toHaveValue('/content/dam/venia');
        expect(propertiesPanel(dialog).$$('coral-multifield-item').length).toEqual(2);
        expect(
            propertiesPanel(dialog).$('coral-multifield-item[aria-posinset="1"] input[name="./elementNames"]')
        ).toHaveValue('productSku');
        expect(
            propertiesPanel(dialog).$('coral-multifield-item[aria-posinset="2"] input[name="./elementNames"]')
        ).toHaveValue('productSpecs');
        expect(propertiesPanel(dialog).$('input[name="./id"]')).toHaveValue('anId');

        // close the dialog
        dialog.$('button.cq-dialog-cancel[variant="default"]').waitAndClick();
        expect(dialog.isExisting()).toBe(false);
    });

    it('handles model change warning', () => {
        let dialog = openComponentDialog();

        // change model
        propertiesPanel(dialog).$('coral-select[name="./modelPath"]').click();
        expect($('coral-popover.is-open')).toBeDisplayed();
        let model = $(`coral-popover.is-open coral-selectlist-item[value="${testing_model2}"]`);
        expect(model).toBeClickable();
        model.click();

        // check warning dialog and cancel it
        let warningDialog = $('coral-dialog.is-open[variant="warning"]');
        expect(warningDialog).toBeDisplayed();
        expect(warningDialog.$('button[variant="default"]')).toBeClickable();
        warningDialog.$('button[variant="default"]').click();

        expect(warningDialog.isExisting()).toBe(false);
        expect(propertiesPanel(dialog).$('input[name="./modelPath"]')).toHaveValue(testing_model1);
        expect(propertiesPanel(dialog).$('input[name="./linkElement"]')).toHaveValue('productSku');

        // change model again
        propertiesPanel(dialog).$('coral-select[name="./modelPath"]').click();
        expect($('coral-popover.is-open')).toBeDisplayed();
        model = $(`coral-popover.is-open coral-selectlist-item[value="${testing_model2}"]`);
        expect(model).toBeClickable();
        model.click();

        // check warning dialog and confirm it
        warningDialog = $('coral-dialog.is-open[variant="warning"]');
        expect(warningDialog).toBeDisplayed();
        expect(warningDialog.$('button[variant="primary"]')).toBeClickable();
        warningDialog.$('button[variant="primary"]').click();

        expect(warningDialog.isExisting()).toBe(false);
        expect(propertiesPanel(dialog).$('input[name="./modelPath"]')).toHaveValue(testing_model2);
        expect(propertiesPanel(dialog).$('input[name="./linkElement"]').getValue()).toBeFalsy();
        expect(propertiesPanel(dialog).$$('coral-multifield-item').length).toEqual(0);

        // close the edit dialog
        dialog.$('button.cq-dialog-cancel[variant="default"]').waitAndClick();
        expect(dialog.isExisting()).toBe(false);
    });

    it('saves data in singleText display mode', () => {
        let dialog = openComponentDialog();

        // change display mode
        dialog.$('coral-radio[value="singleText"]').waitAndClick();
        browser.pause(1000);
        // try to save with empty mandatory field for input validation error
        let doneButton = dialog.$('button.cq-dialog-submit[variant="primary"]');
        doneButton.click();
        expect(propertiesPanel(dialog).$('coral-select[name="./elementNames"]')).toHaveAttr('invalid');

        // select link element
        propertiesPanel(dialog).$('coral-select[name="./linkElement"]').click();
        expect($('coral-popover.is-open')).toBeDisplayed();
        let linkElement = $('coral-popover.is-open coral-selectlist-item[value="productSku"]');
        expect(linkElement).toBeClickable();
        linkElement.click();

        // select element
        propertiesPanel(dialog).$('coral-select[name="./elementNames"]').waitAndClick();
        expect($('coral-popover.is-open')).toBeDisplayed();
        $('coral-popover.is-open coral-selectlist-item[value="productSpecs"]').waitAndClick();

        // set paragraph control values
        let tabs = dialog.$$('coral-tab');
        expect(tabs.length).toEqual(2);
        expect(tabs[1]).toHaveText('Paragraph Control');
        expect(tabs[1]).toBeDisplayed();
        tabs[1].click();
        dialog.$('coral-panel[selected] input[value="range"]').waitAndClick();
        let range = dialog.$('coral-panel[selected] input[name="./paragraphRange"]');
        expect(range).toBeEnabled();
        range.setValue('1;3');
        dialog.$('coral-panel[selected] coral-checkbox[name="./paragraphHeadings"]').waitAndClick();

        // save
        doneButton.click();
        browser.waitUntil(function () {
            return !dialog.isExisting();
        });

        // re-open dialog
        dialog = openComponentDialog();

        // check state
        expect(propertiesPanel(dialog).$('input[name="./modelPath"]')).toHaveValue(testing_model1);
        expect(propertiesPanel(dialog).$('coral-select[name="./linkElement"]')).toBeEnabled();
        expect(propertiesPanel(dialog).$('input[name="./linkElement"]')).toHaveValue('productSku');
        expect(
            propertiesPanel(dialog).$('foundation-autocomplete[name="./parentPath"] input[name="./parentPath"]')
        ).toHaveValue('/content/dam/venia');
        expect(propertiesPanel(dialog).$('coral-radio[name="./displayMode"][checked]')).toHaveValue('singleText');
        expect(
            propertiesPanel(dialog).$('coral-select[name="./elementNames"] input[name="./elementNames"]')
        ).toHaveValue('productSpecs');
        expect(propertiesPanel(dialog).$('input[name="./id"]')).toHaveValue('anId');
        tabs = dialog.$$('coral-tab');
        expect(tabs.length).toEqual(2);
        tabs[1].click();
        expect(
            dialog.$('coral-panel[selected] coral-radio[name="./paragraphScope"][checked] input[value="range"]')
        ).toBeDisplayed();
        expect(dialog.$('coral-panel[selected] input[name="./paragraphRange"]')).toHaveValue('1;3');
        expect(dialog.$('coral-panel[selected] input[name="./paragraphHeadings"]')).toBeChecked();

        // close the edit dialog
        dialog.$('button.cq-dialog-cancel[variant="default"]').waitAndClick();
        expect(dialog.isExisting()).toBe(false);
    });
});
