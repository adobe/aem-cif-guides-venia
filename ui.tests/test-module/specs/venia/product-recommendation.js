/*
 *  Copyright 2025 Adobe Systems Incorporated
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

let testing_page;
describe('Product recommendation', function () {
    let onboardingHandler;
    let addedNodeName = null;
    before(() => {
        try {
            // Set window size to desktop
            browser.setWindowSize(1280, 960);

            // AEM Login
            browser.AEMForceLogout();
            browser.url(config.aem.author.base_url);
            browser.AEMLogin(config.aem.author.username, config.aem.author.password);

            // Enable helper to handle onboarding dialog popup
            onboardingHandler = new OnboardingDialogHandler(browser);
            onboardingHandler.enable();

            // Create a random test page with product page template
            const pageName = `product-test-${randomString()}`;
            testing_page = `/content/venia/us/en/products/${pageName}`;
            browser.AEMCreatePage({
                title: 'Product Test Page',
                name: pageName,
                parent: '/content/venia/us/en/products',
                template: '/conf/venia/settings/wcm/templates/product-page'
            });

            // Configure the product detail component with a specific product
            configureProductDetailComponent();
        } catch (error) {
            // If setup fails, still record the testing_page for cleanup
            console.error('Setup failed:', error.message);
            throw error; // Re-throw to fail the test properly
        }
    });

    afterEach(function () {
        // Clean up any open dialogs after each test to prevent interference
        try {
            const dialogs = $$('.cq-dialog');
            dialogs.forEach(dialog => {
                if (dialog.isDisplayed()) {
                    const closeButton = dialog.$('button.cq-dialog-cancel, button[title="Cancel"], .coral-Icon--close');
                    if (closeButton.isDisplayed()) {
                        closeButton.click();
                    }
                }
            });
        } catch (e) {
            // Ignore dialog cleanup errors
        }
    });

    after(function () {
        try {
            if (onboardingHandler) {
                onboardingHandler.disable();
            }
        } catch (e) {
            // Continue cleanup even if onboarding handler fails
        }

        try {
            if (testing_page) {
                browser.AEMDeletePage(testing_page);
            }
        } catch (e) {
            // Log cleanup failure but don't throw to avoid masking test failures
            console.warn('Failed to delete test page:', testing_page, e.message);
        }
    });

    const configureProductDetailComponent = () => {
        browser.url(`${config.aem.author.base_url}/editor.html${testing_page}.html`);
        browser.AEMEditorLoaded();

        const productComponent = $('div[data-path*="/jcr:content/root/container/container/product"]');
        productComponent.waitForDisplayed({ timeout: 5000 });
        expect(productComponent).toBeDisplayed();

        productComponent.waitAndClick();

        const configureButton = $('button[title="Configure"]');
        configureButton.waitForDisplayed({ timeout: 5000 });
        expect(configureButton).toBeDisplayed();
        configureButton.waitAndClick();

        const productField = $('product-field');
        productField.waitForDisplayed({ timeout: 5000 });
        browser.CIFSelectProduct(productField, 'Alexia Maxi Dress');

        clickDoneButton();
    };

    const addComponentToPage = (group = 'Venia - Commerce') => {
        browser.url(`${config.aem.author.base_url}/editor.html${testing_page}.html`);
        browser.AEMEditorLoaded();
        browser.EditorOpenSidePanel();

        $('coral-tab[title="Components"]').waitAndClick({ x: 1, y: 1 });

        $('#components-filter coral-select button').waitAndClick();
        $(`coral-selectlist-item[value="${group}"]`).waitForDisplayed({ timeout: 5000 });
        $(`coral-selectlist-item[value="${group}"]`).waitAndClick();
        expect($('#components-filter coral-select [handle=label]')).toHaveText(group);

        const name = 'Product Recommendations';
        const componentPath = '/apps/venia/components/commerce/productrecommendations';

        const cmpSelector = `div.editor-ComponentBrowser-component[data-title="${name}"][data-group="${group}"][data-path="${componentPath}"]`;
        const cmp = $(cmpSelector);
        expect(cmp).toBeDisplayed();

        cmp.scrollIntoView();

        const dropTarget = $(`div[data-path="${testing_page}/jcr:content/root/container/container/*"]`);
        cmp.dragAndDrop(dropTarget, 1000);

        browser.waitUntil(
            () => {
                const addedComponents = $$(
                    `div[data-path*="${testing_page}/jcr:content/root/container/container/"][data-path*="productrecommendatio"]`
                );
                return addedComponents.length > 0;
            },
            { timeout: 5000, timeoutMsg: 'Component was not added to the page' }
        );

        const addedComponents = $$(
            `div[data-path*="${testing_page}/jcr:content/root/container/container/"][data-path*="productrecommendatio"]`
        );

        let addedNodeName = 'productrecommendatio';
        if (addedComponents.length > 0) {
            const lastComponent = addedComponents[addedComponents.length - 1];
            const fullPath = lastComponent.getAttribute('data-path');
            const pathParts = fullPath.split('/');
            addedNodeName = pathParts[pathParts.length - 1];
        }

        return addedNodeName;
    };

    const openComponentDialog = (node = 'productrecommendatio') => {
        const cmpPlaceholder = $(`div[data-path="${testing_page}/jcr:content/root/container/container/${node}"]`);
        expect(cmpPlaceholder).toBeDisplayed();
        cmpPlaceholder.waitAndClick();
        const configureButton = $('button[title="Configure"]');
        expect(configureButton).toBeDisplayed();
        configureButton.waitAndClick();
    };

    const clickDoneButton = () => {
        let doneButton;
        try {
            doneButton = $('button.cq-dialog-submit[title="Done"]');
            if (!doneButton.isDisplayed()) {
                doneButton = $('button.cq-dialog-submit[icon="check"]');
            }
        } catch (e) {
            doneButton = $('button.cq-dialog-submit[icon="check"]');
        }

        expect(doneButton).toBeDisplayed();
        doneButton.waitAndClick();

        browser.waitUntil(() => !$('.cq-dialog').isDisplayed(), {
            timeout: 5000,
            timeoutMsg: 'Dialog did not close'
        });
    };

    const openPublishedPageAndScroll = () => {
        const productPageUrl = `${config.aem.author.base_url}${testing_page}.html?wcmmode=disabled`;
        browser.url(productPageUrl);

        browser.execute(() => {
            /* eslint-disable-next-line no-undef */
            window.scrollTo(0, document.body.scrollHeight * 0.6);
        });
    };

    const validateRecommendationsTitle = expectedTitle => {
        const titleElement = $('.cmp-ProductRecsGallery__ProductRecsGallery__title');
        titleElement.waitForDisplayed({ timeout: 5000 });
        expect(titleElement).toBeDisplayed();

        const titleText = titleElement.getText();
        expect(titleText).not.toBe('');
        expect(titleText).not.toBe(null);

        if (expectedTitle) {
            expect(titleText).toBe(expectedTitle);
        }

        return titleText;
    };

    const validateProductCards = (minCount = 3) => {
        const productCards = $$('.cmp-ProductRecsGallery__ProductCard__card');
        expect(productCards.length).toBeGreaterThan(minCount);

        productCards.forEach(card => {
            const productLink = card.$('a');
            expect(productLink).toBeDisplayed();

            const productTitleDiv = productLink.$('div:nth-child(2)');
            const productTitle = productTitleDiv.getText();
            expect(productTitle).not.toBe('');
            expect(productTitle).not.toBe(null);

            const priceElement = card.$('.cmp-ProductRecsGallery__ProductCard__price');
            expect(priceElement).toBeDisplayed();
            const priceText = priceElement.getText();
            expect(priceText).not.toBe('');
        });

        return productCards.length;
    };

    const configureComponentDialog = config => {
        const { preconfigured, title, recommendationType } = config;

        const preconfiguredCheckbox = $('coral-checkbox[name="./preconfigured"]');
        expect(preconfiguredCheckbox).toBeDisplayed();

        const checkboxInput = preconfiguredCheckbox.$('input[type="checkbox"]');
        const isChecked = checkboxInput.isSelected();

        if (preconfigured && !isChecked) {
            preconfiguredCheckbox.waitAndClick();
        } else if (!preconfigured && isChecked) {
            preconfiguredCheckbox.waitAndClick();
        }

        if (!preconfigured && title) {
            const titleInput = $('input[name="./jcr:title"]');
            expect(titleInput).toBeDisplayed();
            titleInput.clearValue();
            titleInput.setValue(title);
        }

        if (!preconfigured && recommendationType) {
            const recommendationTypeDropdown = $('coral-select[name="./recommendationType"]');
            expect(recommendationTypeDropdown).toBeDisplayed();
            recommendationTypeDropdown.waitAndClick();

            const option = $(`coral-selectlist-item[value="${recommendationType}"]`);
            option.waitForDisplayed({ timeout: 5000 });
            option.waitAndClick();
        }

        clickDoneButton();
    };

    it('adding component successfully to page test', () => {
        addedNodeName = addComponentToPage();
        openComponentDialog(addedNodeName);

        configureComponentDialog({ preconfigured: true });
    });

    it('should display product recommendations with title and products', () => {
        openPublishedPageAndScroll();
        validateRecommendationsTitle();
        validateProductCards(3);
    });

    it('product recommendation custom configuration test', () => {
        browser.url(`${config.aem.author.base_url}/editor.html${testing_page}.html`);
        browser.AEMEditorLoaded();
        openComponentDialog(addedNodeName);

        configureComponentDialog({
            preconfigured: false,
            title: 'Recommended products test',
            recommendationType: 'more-like-this'
        });

        openPublishedPageAndScroll();
        validateRecommendationsTitle('Recommended products test');
        validateProductCards(3);
    });
});
