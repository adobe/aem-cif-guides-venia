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
    });

    after(function () {
        onboardingHandler.disable();
        if (testing_page) {
            browser.AEMDeletePage(testing_page);
        }
    });

    function productVariantSelection(productField, productName) {
        expect(productField).toBeDisplayed();

        const pickerButton = productField.$('button[aria-label="Open Picker"]');
        if (!pickerButton.isDisplayed()) {
            return false;
        }

        pickerButton.waitForEnabled();
        pickerButton.click();
        expect($('h3=Add Product')).toBeDisplayed();

        return browser.waitUntil(
            () => {
                try {
                    const productElement = $(`//div[contains(text(),"${productName}")]`);

                    if (productElement.isDisplayed() && productElement.isClickable()) {
                        productElement.click();

                        const submitButton = $('span=Add').parentElement();
                        if (submitButton.isDisplayed()) {
                            submitButton.waitForEnabled();
                            let enabled = submitButton.isEnabled();
                            if (enabled) {
                                submitButton.click();
                                return true;
                            }
                        }
                    }
                    return false;
                } catch (e) {
                    return false;
                }
            },
            {
                timeout: 15000,
                timeoutMsg: `Failed to select product directly: ${productName}`
            }
        );
    }

    function manualProductSelection() {
        let pickerButton = $('button[aria-label="Open Picker"]');
        if (!pickerButton.isDisplayed()) {
            pickerButton = $('.spectrum-ActionButton_e2d99e.PickerButton__position_right__p_skQ');
        }
        if (!pickerButton.isDisplayed()) {
            pickerButton = $('button.spectrum-ActionButton_e2d99e');
        }

        if (pickerButton.isDisplayed()) {
            pickerButton.waitAndClick();

            const searchField = $('input[type="search"][placeholder="Search"], input[aria-label="Search"]');
            if (searchField.isDisplayed()) {
                searchField.setValue('Alexia');

                const product = $('//div[contains(text(),"Alexia Maxi Dress")]');
                if (product.isDisplayed()) {
                    product.click();

                    const addButton = $('span=Add').parentElement();
                    if (addButton.isDisplayed()) {
                        addButton.click();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    const configureProductDetailComponent = () => {
        browser.url(`${config.aem.author.base_url}/editor.html${testing_page}.html`);
        browser.AEMEditorLoaded();

        const productComponent = $('div[data-path*="/jcr:content/root/container/container/product"]');
        expect(productComponent).toBeDisplayed();

        productComponent.waitAndClick();

        const configureButton = $('button[title="Configure"]');
        expect(configureButton).toBeDisplayed();
        configureButton.waitAndClick();

        const productField = $('product-field');
        if (productField.isDisplayed()) {
            const success = productVariantSelection(productField, 'Alexia Maxi Dress');
            if (!success) {
                manualProductSelection();
            }
        } else {
            manualProductSelection();
        }

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
            { timeout: 10000, timeoutMsg: 'Component was not added to the page' }
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

    it('adding component successfully to page test', () => {
        addedNodeName = addComponentToPage();
        openComponentDialog(addedNodeName);

        const preconfiguredCheckbox = $('coral-checkbox[name="./preconfigured"]');
        expect(preconfiguredCheckbox).toBeDisplayed();

        const checkboxInput = preconfiguredCheckbox.$('input[type="checkbox"]');
        const isChecked = checkboxInput.isSelected();
        if (!isChecked) {
            preconfiguredCheckbox.click();
        }

        clickDoneButton();
    });

    it('should capture preconfigured POST response and validate component data', () => {
        const intercept = browser.mock('**/recs/v1/precs/preconfigured*', {
            method: 'POST'
        });

        const productPageUrl = `${config.aem.author.base_url}${testing_page}.html?wcmmode=disabled`;
        browser.url(productPageUrl);

        browser.execute(() => {
            /* eslint-disable-next-line no-undef */
            window.scrollTo(0, document.body.scrollHeight * 0.6);
        });

        const titleElement = $('.cmp-ProductRecsGallery__ProductRecsGallery__title');
        titleElement.waitForDisplayed({ timeout: 20000 });

        const titleText = titleElement.getText();
        const productCards = $$('.cmp-ProductRecsGallery__ProductCard__card');

        const componentProductNames = [];
        productCards.forEach(card => {
            const productLink = card.$('a');
            const productTitleDiv = productLink.$('div:nth-child(2)');
            const productTitle = productTitleDiv.getText();
            componentProductNames.push(productTitle);
        });

        const calls = intercept.calls;
        expect(calls.length).toBeGreaterThan(0, 'Should intercept API calls');

        const call = calls[0];
        const requestData = call.body;
        const firstResult = requestData.results[0];
        const apiTitle = firstResult.storefrontLabel || firstResult.unitName;
        const apiProductNames = firstResult.products.map(product => product.name);

        const titleMatch = apiTitle === titleText;
        const allApiProductsPresent = apiProductNames.every(apiName => componentProductNames.includes(apiName));

        expect(titleMatch).toBe(true, 'API title should match component title');
        expect(allApiProductsPresent).toBe(true, 'All API products should be present in component');
    });

    it('product recommendation custom configuration test', () => {
        try {
            const editorUrl = `${config.aem.author.base_url}/editor.html${testing_page}.html`;
            browser.url(editorUrl);
            browser.AEMEditorLoaded();
            openComponentDialog(addedNodeName);

            const preconfiguredCheckbox = $('coral-checkbox[name="./preconfigured"]');
            expect(preconfiguredCheckbox).toBeDisplayed();

            const checkboxInput = preconfiguredCheckbox.$('input[type="checkbox"]');
            const isChecked = checkboxInput.isSelected();

            if (isChecked) {
                preconfiguredCheckbox.waitAndClick();
            }

            const titleInput = $('input[name="./jcr:title"]');
            expect(titleInput).toBeDisplayed();
            titleInput.clearValue();
            titleInput.setValue('Recommended products test');

            const recommendationTypeDropdown = $('coral-select[name="./recommendationType"]');
            expect(recommendationTypeDropdown).toBeDisplayed();
            recommendationTypeDropdown.waitAndClick();

            const moreLikeThisOption = $('coral-selectlist-item[value="more-like-this"]');
            moreLikeThisOption.waitForDisplayed({ timeout: 5000 });
            moreLikeThisOption.waitAndClick();

            clickDoneButton();

            const testProductUrl = `${config.aem.author.base_url}${testing_page}.html?wcmmode=disabled`;
            browser.url(testProductUrl);

            const productName = $('.productFullDetail__productName span[role="name"]');
            expect(productName).toBeDisplayed();
            expect(productName).toHaveText('Alexia Maxi Dress');

            const productSku = $('.productFullDetail__sku strong[role="sku"]');
            expect(productSku).toBeDisplayed();
            expect(productSku).toHaveText('VD09');

            const productPrice = $('.productFullDetail__price .price');
            expect(productPrice).toBeDisplayed();

            const recommendationsComponent = $('[data-is-product-recs]');
            expect(recommendationsComponent).toBeDisplayed();

            browser.execute(() => {
                /* eslint-disable-next-line no-undef */
                window.scrollTo(0, document.body.scrollHeight * 0.7);
            });

            const recommendationsTitle = $('.cmp-ProductRecsGallery__ProductRecsGallery__title');
            expect(recommendationsTitle).toBeDisplayed();
            expect(recommendationsTitle).toHaveText('Recommended products test');

            const recommendationCards = $$('.cmp-ProductRecsGallery__ProductCard__card');
            expect(recommendationCards.length).toBeGreaterThan(0);

            const cardsToTest = Math.min(3, recommendationCards.length);

            for (let i = 0; i < cardsToTest; i++) {
                const card = recommendationCards[i];

                const productImage = card.$('.cmp-ProductRecsGallery__ProductCard__productImage');
                expect(productImage).toBeDisplayed();

                const productLink = card.$('a[title]');
                expect(productLink).toBeDisplayed();

                const productTitle = productLink.getAttribute('title');
                expect(productTitle).not.toBe('');
                expect(productTitle).not.toBe(null);
                expect(productTitle).not.toBe('Alexia Maxi Dress');

                const addToCartBtn = card.$('.cmp-ProductRecsGallery__ProductCard__addToCart');
                expect(addToCartBtn).toBeDisplayed();

                const addToWishlistBtn = card.$('.cmp-ProductRecsGallery__ProductCard__addToWishlist');
                expect(addToWishlistBtn).toBeDisplayed();
            }
        } finally {
            // Component is kept for other tests
        }
    });

    it('test page cleanup verification', () => {
        browser.url(`${config.aem.author.base_url}/editor.html${testing_page}.html`);
        browser.AEMEditorLoaded();

        const productComponent = $('div[data-path*="/jcr:content/root/container/container/product"]');
        expect(productComponent).toBeDisplayed();

        if (addedNodeName) {
            const recommendationComponent = $(
                `div[data-path="${testing_page}/jcr:content/root/container/container/${addedNodeName}"]`
            );
            expect(recommendationComponent).toBeDisplayed();
        }
    });
});
