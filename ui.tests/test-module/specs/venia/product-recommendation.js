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
const { OnboardingDialogHandler } = require('../../lib/commons');

let testing_page;
describe('Product recommendation', function () {
    let onboardingHandler;
    let addedNodeName = null;
    testing_page = '/content/venia/us/en/products/product-page';
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
    });

    after(function () {
        // Disable helper to handle onboarding dialog popup
        onboardingHandler.disable();
    });

    const addComponentToPage = (group = 'Venia - Commerce') => {
        browser.url(`${config.aem.author.base_url}/editor.html/content/venia/us/en/products/product-page.html`);
        browser.AEMEditorLoaded();
        browser.EditorOpenSidePanel();

        // Open the Components tab
        $('coral-tab[title="Components"]').waitAndClick({ x: 1, y: 1 });

        // Filter for Commerce components
        $('#components-filter coral-select button').waitAndClick();
        $(`coral-selectlist-item[value="${group}"]`).waitForDisplayed({ timeout: 5000 });
        $(`coral-selectlist-item[value="${group}"]`).waitAndClick();
        expect($('#components-filter coral-select [handle=label]')).toHaveText(group);

        const name = 'Product Recommendations';
        const componentPath = '/apps/venia/components/commerce/productrecommendations';

        // Build a more precise selector
        const cmpSelector = `div.editor-ComponentBrowser-component[data-title="${name}"][data-group="${group}"][data-path="${componentPath}"]`;
        const cmp = $(cmpSelector);
        expect(cmp).toBeDisplayed();

        cmp.scrollIntoView();

        const dropTarget = $(`div[data-path="${testing_page}/jcr:content/root/container/container/*"]`);
        cmp.dragAndDrop(dropTarget, 1000);

        // Wait for component to be added
        browser.waitUntil(
            () => {
                const addedComponents = $$(
                    `div[data-path*="${testing_page}/jcr:content/root/container/container/"][data-path*="productrecommendatio"]`
                );
                return addedComponents.length > 0;
            },
            { timeout: 10000, timeoutMsg: 'Component was not added to the page' }
        );

        // Find the newly added component and get its actual node name
        const addedComponents = $$(
            `div[data-path*="${testing_page}/jcr:content/root/container/container/"][data-path*="productrecommendatio"]`
        );

        // Get the last added component (most recent)
        let addedNodeName = 'productrecommendatio'; // default fallback
        if (addedComponents.length > 0) {
            const lastComponent = addedComponents[addedComponents.length - 1];
            const fullPath = lastComponent.getAttribute('data-path');

            // Extract node name from path like "/content/.../container/productrecommendatio_123456"
            const pathParts = fullPath.split('/');
            addedNodeName = pathParts[pathParts.length - 1];
        }

        return addedNodeName;
    };

    const openComponentDialog = (node = 'productrecommendatio') => {
        // Open edit dialog
        const cmpPlaceholder = $(`div[data-path="${testing_page}/jcr:content/root/container/container/${node}"]`);
        expect(cmpPlaceholder).toBeDisplayed();
        cmpPlaceholder.waitAndClick();
        const configureButton = $('button[title="Configure"]');
        expect(configureButton).toBeDisplayed();
        configureButton.waitAndClick();
    };

    const clickDoneButton = () => {
        // Handle both AEM Cloud and AEM 6.5 Done button variations
        let doneButton;
        try {
            // Try Cloud version first (with title)
            doneButton = $('button.cq-dialog-submit[title="Done"]');
            if (!doneButton.isDisplayed()) {
                // Try 6.5 version with icon
                doneButton = $('button.cq-dialog-submit[icon="check"]');
            }
        } catch (e) {
            // Fallback to 6.5 version
            doneButton = $('button.cq-dialog-submit[icon="check"]');
        }

        expect(doneButton).toBeDisplayed();
        doneButton.waitAndClick();

        // Wait for dialog to close
        browser.waitUntil(() => !$('.cq-dialog').isDisplayed(), {
            timeout: 5000,
            timeoutMsg: 'Dialog did not close'
        });
    };

    const deleteComponent = (node = 'productrecommendatio') => {
        // Navigate back to the product page editor
        browser.url(`${config.aem.author.base_url}/editor.html/content/venia/us/en/products/product-page.html`);
        browser.AEMEditorLoaded();

        // Find the component to delete
        const cmpPlaceholder = $(`div[data-path="${testing_page}/jcr:content/root/container/container/${node}"]`);
        expect(cmpPlaceholder).toBeDisplayed();

        // Click on the component to select it
        cmpPlaceholder.waitAndClick();

        // Find and click the delete button using the provided selector
        const deleteButton = $('button[data-action="DELETE"][title="Delete"]');
        expect(deleteButton).toBeDisplayed();

        deleteButton.waitAndClick();

        // Confirm deletion if confirmation dialog appears
        try {
            const confirmButton = $('button[id="DELETE"]');
            if (confirmButton.isDisplayed()) {
                confirmButton.waitAndClick();
            }
        } catch (e) {
            // No confirmation dialog appeared
        }

        // Component deletion completed
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
        // Set up network intercept for preconfigured API
        const intercept = browser.mock('**/recs/v1/precs/preconfigured*', {
            method: 'POST'
        });

        // Load product page to trigger API calls
        const productPageUrl = `${config.aem.author.base_url}/content/venia/us/en/products/product-page.html/venia-tops/venia-blouses/jillian-top.html?wcmmode=disabled`;
        browser.url(productPageUrl);

        // Wait for component to load using timeout
        const titleElement = $('.cmp-ProductRecsGallery__ProductRecsGallery__title');
        titleElement.waitForDisplayed({ timeout: 20000 });

        // Extract component data
        const titleText = titleElement.getText();
        const productCards = $$('.cmp-ProductRecsGallery__ProductCard__card');

        const componentProductNames = [];
        productCards.forEach(card => {
            const productLink = card.$('a');
            const productTitleDiv = productLink.$('div:nth-child(2)');
            const productTitle = productTitleDiv.getText();
            componentProductNames.push(productTitle);
        });

        // Validate API data
        const calls = intercept.calls;
        expect(calls.length).toBeGreaterThan(0, 'Should intercept API calls');

        const call = calls[0];
        const requestData = call.body;
        const firstResult = requestData.results[0];
        const apiTitle = firstResult.storefrontLabel;
        const apiProductNames = firstResult.products.map(product => product.name);

        // Validate data matches
        const titleMatch = apiTitle === titleText;
        const allApiProductsPresent = apiProductNames.every(apiName => componentProductNames.includes(apiName));

        expect(titleMatch).toBe(true, 'API title should match component title');
        expect(allApiProductsPresent).toBe(true, 'All API products should be present in component');
    });

    it('product recommendation custom configuration test', () => {
        try {
            // Add component to page and get the actual node name

            browser.url(`${config.aem.author.base_url}/editor.html/content/venia/us/en/products/product-page.html`);
            browser.AEMEditorLoaded();

            openComponentDialog(addedNodeName);

            // Check if "Use preconfigured recommendation" checkbox is checked
            const preconfiguredCheckbox = $('coral-checkbox[name="./preconfigured"]');
            expect(preconfiguredCheckbox).toBeDisplayed();

            // Check if checkbox is checked
            const checkboxInput = preconfiguredCheckbox.$('input[type="checkbox"]');
            const isChecked = checkboxInput.isSelected();

            if (isChecked) {
                // If checked, uncheck it to enable custom configuration
                preconfiguredCheckbox.waitAndClick();
            }

            // Set custom title value
            const titleInput = $('input[name="./jcr:title"]');
            expect(titleInput).toBeDisplayed();
            titleInput.clearValue();
            titleInput.setValue('Recommended products test');

            // Select recommendation type from dropdown
            const recommendationTypeDropdown = $('coral-select[name="./recommendationType"]');
            expect(recommendationTypeDropdown).toBeDisplayed();
            recommendationTypeDropdown.waitAndClick();

            // Select "more-like-this" option
            const moreLikeThisOption = $('coral-selectlist-item[value="more-like-this"]');
            moreLikeThisOption.waitForDisplayed({ timeout: 5000 });
            moreLikeThisOption.waitAndClick();

            // Click Done button to close dialog (handles both Cloud and 6.5 versions)
            clickDoneButton();

            // Navigate to Camilla Palazzo Pants product page
            const camillaProductUrl = `${config.aem.author.base_url}/content/venia/us/en/products/product-page.html/venia-bottoms/venia-pants/camilla-palazzo-pants.html`;
            browser.url(camillaProductUrl);

            // Verify Camilla Palazzo Pants product details are displayed
            const productName = $('.productFullDetail__productName span[role="name"]');
            expect(productName).toBeDisplayed();
            expect(productName).toHaveText('Camilla Palazzo Pants');

            const productSku = $('.productFullDetail__sku strong[role="sku"]');
            expect(productSku).toBeDisplayed();
            expect(productSku).toHaveText('VP03');

            const productPrice = $('.productFullDetail__price .price');
            expect(productPrice).toBeDisplayed();
            expect(productPrice).toHaveText('$88.00');

            // Verify product recommendations component is present and shows data
            const recommendationsComponent = $('[data-is-product-recs]');
            expect(recommendationsComponent).toBeDisplayed();

            // Verify custom title is displayed
            const recommendationsTitle = $('.cmp-ProductRecsGallery__ProductRecsGallery__title');
            expect(recommendationsTitle).toBeDisplayed();
            expect(recommendationsTitle).toHaveText('Recommended products test');

            // Verify that product recommendation cards are displayed
            const recommendationCards = $$('.cmp-ProductRecsGallery__ProductCard__card');
            expect(recommendationCards.length).toBeGreaterThan(0);

            // Verify first few recommendation cards contain product data
            for (let i = 0; i < Math.min(3, recommendationCards.length); i++) {
                const card = recommendationCards[i];

                // Check product image
                const productImage = card.$('.cmp-ProductRecsGallery__ProductCard__productImage');
                expect(productImage).toBeDisplayed();

                // Check product title/name
                const productLink = card.$('a[title]');
                expect(productLink).toBeDisplayed();
                const productTitle = productLink.getAttribute('title');
                expect(productTitle).not.toBe('');
                expect(productTitle).not.toBe(null);

                // Check that it's showing related pants products (should be different from Camilla Palazzo Pants)
                expect(productTitle).not.toBe('Camilla Palazzo Pants');

                // Verify Add to Cart button
                const addToCartBtn = card.$('.cmp-ProductRecsGallery__ProductCard__addToCart');
                expect(addToCartBtn).toBeDisplayed();

                // Verify Add to Wishlist button
                const addToWishlistBtn = card.$('.cmp-ProductRecsGallery__ProductCard__addToWishlist');
                expect(addToWishlistBtn).toBeDisplayed();
            }
        } finally {
            // Keep the component for Test 2 to use - no cleanup here
        }
    });

    it('product deletion test', () => {
        deleteComponent(addedNodeName);
    });

    // Verify component is deleted
});
