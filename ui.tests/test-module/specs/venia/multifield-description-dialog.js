/*
 *  Copyright 2026 Adobe Systems Incorporated
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

// SITES-50862: when product rows are added to a multifield, each row's fieldDescription tooltip must
// get its own unique id (Coral clones the template row, so without the fix all rows share one id).
// Uses a small test component whose dialog has a product field in a simple multifield WITH a
// fieldDescription, so the info icon + tooltip are guaranteed to render.
describe('Multifield Description Component Dialog', function () {
    const editor_page = `${config.aem.author.base_url}/editor.html`;

    let testing_page;
    let onboardingHdler;

    before(function () {
        browser.setWindowSize(1280, 960);

        browser.AEMForceLogout();
        browser.url(config.aem.author.base_url);
        browser.AEMLogin(config.aem.author.username, config.aem.author.password);

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
        if (onboardingHdler) {
            onboardingHdler.disable();
            browser.AEMDeletePage(testing_page);
        }
    });

    const addComponentToPage = (name = 'Multifield Description Test', group = 'Venia - Commerce') => {
        browser.url(`${editor_page}${testing_page}.html`);
        browser.AEMEditorLoaded();
        browser.EditorOpenSidePanel();

        $('coral-tab[title="Components"]').waitAndClick({ x: 1, y: 1 });

        $('#components-filter coral-select button').waitAndClick();
        browser.pause(200);
        $(`coral-selectlist-item[value="${group}"]`).waitAndClick();
        expect($('#components-filter coral-select [handle=label]')).toHaveText(group);

        const testCmp = $(`div[data-title="${name}"]`);
        expect(testCmp).toBeDisplayed();
        const dropTarget = $(`div[data-path="${testing_page}/jcr:content/root/container/container/*"]`);
        testCmp.dragAndDrop(dropTarget, 1000);
    };

    const openComponentDialog = () => {
        const node = 'multifielddescriptiontest';
        const cmpPlaceholder = $(`div[data-path="${testing_page}/jcr:content/root/container/container/${node}"]`);
        expect(cmpPlaceholder).toBeDisplayed();
        cmpPlaceholder.click();
        const configureButton = $('button[title="Configure"]');
        expect(configureButton).toBeDisplayed();
        configureButton.click();
        const dialog = $('coral-dialog[trackingfeature="venia:multifielddescriptiontest:v1"]');
        expect(dialog).toBeDisplayed();
        return dialog;
    };

    it('gives each added product row a unique description tooltip id', () => {
        const dialog = openComponentDialog();

        const multifield = dialog.$('coral-multifield');
        expect(multifield).toExist();

        // Add three product rows.
        const addButton = multifield.$('button[coral-multifield-add]');
        for (let i = 0; i < 3; i++) {
            addButton.waitForClickable({ timeout: 5000 });
            addButton.click();
            browser.pause(200);
        }
        browser.waitUntil(() => multifield.$$('coral-multifield-item').length >= 3, {
            timeout: 5000,
            timeoutMsg: 'Expected three product rows'
        });

        // Read each row's tooltip id and field association from the DOM.
        const rows = browser.execute(function () {
            // eslint-disable-next-line no-undef
            const items = document.querySelectorAll('coral-multifield coral-multifield-item');
            return Array.prototype.map.call(items, function (item) {
                const field = item.querySelector('product-field');
                const icon = item.querySelector('coral-icon.coral-Form-fieldinfo');
                const tooltip = item.querySelector('coral-tooltip');
                return {
                    hasIcon: !!icon,
                    tooltipId: tooltip ? tooltip.id : null,
                    fieldDescribedBy: field ? field.getAttribute('aria-describedby') : null
                };
            });
        });

        expect(rows.length).toBeGreaterThanOrEqual(3);
        rows.forEach(row => {
            expect(row.hasIcon).toBe(true);
            expect(row.tooltipId).toBeTruthy();
            expect(row.fieldDescribedBy).toBe(row.tooltipId);
        });

        // All tooltip ids are unique.
        const ids = rows.map(row => row.tooltipId);
        expect(new Set(ids).size).toBe(ids.length);

        dialog.$('button.cq-dialog-cancel').click();
    });
});
