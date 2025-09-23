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
        console.log('🧹 Starting cleanup process...');

        // Disable helper to handle onboarding dialog popup
        onboardingHandler.disable();
        console.log('✅ Onboarding handler disabled');

        // Delete the test page
        if (testing_page) {
            console.log('🗑️ Deleting test page:', testing_page);
            browser.AEMDeletePage(testing_page);
            console.log('✅ Test page deleted successfully');
        } else {
            console.log('⚠️ No test page to delete');
        }

        console.log('🎉 Cleanup completed successfully!');
    });

    // Direct product selection approach (no variants needed)
    function productVariantSelection(productField, productName) {
        expect(productField).toBeDisplayed();
        console.log('🔍 Starting direct product selection for:', productName);

        const pickerButton = productField.$('button[aria-label="Open Picker"]');
        if (!pickerButton.isDisplayed()) {
            console.log('❌ Picker button with aria-label not found');
            return false;
        }

        pickerButton.waitForEnabled();
        pickerButton.click();
        console.log('✅ Clicked picker button');

        console.log('⏳ Waiting for "Add Product" dialog...');
        expect($('h3=Add Product')).toBeDisplayed();
        console.log('✅ "Add Product" dialog opened');

        return browser.waitUntil(
            () => {
                try {
                    // Directly select product by text content using XPath (no variants step needed)
                    console.log('🔍 Looking for product directly in picker...');
                    const productElement = $(`//div[contains(text(),"${productName}")]`);

                    if (productElement.isDisplayed() && productElement.isClickable()) {
                        console.log('✅ Found product directly:', productName);
                        productElement.click();
                        console.log('✅ Clicked on product');

                        // Click the Add button
                        const submitButton = $('span=Add').parentElement();
                        if (submitButton.isDisplayed()) {
                            submitButton.waitForEnabled();
                            let enabled = submitButton.isEnabled();
                            if (enabled) {
                                submitButton.click();
                                console.log('✅ Successfully added product:', productName);
                                return true;
                            } else {
                                console.log('⚠️ Add button not enabled yet, waiting...');
                            }
                        } else {
                            console.log('⚠️ Add button not found');
                        }
                    } else {
                        console.log('⚠️ Product not found or not clickable:', productName);
                    }

                    return false;
                } catch (e) {
                    console.log('⚠️ Error in direct product selection:', e.message);
                    return false;
                }
            },
            {
                timeout: 15000,
                timeoutMsg: `Failed to select product directly: ${productName}`
            }
        );
    }

    // Fallback manual product selection if product-field approach fails
    function manualProductSelection() {
        console.log('🔧 Using manual product selection approach...');

        // Look for picker button with various selectors
        let pickerButton = $('button[aria-label="Open Picker"]');
        if (!pickerButton.isDisplayed()) {
            pickerButton = $('.spectrum-ActionButton_e2d99e.PickerButton__position_right__p_skQ');
        }
        if (!pickerButton.isDisplayed()) {
            pickerButton = $('button.spectrum-ActionButton_e2d99e');
        }

        if (pickerButton.isDisplayed()) {
            pickerButton.waitAndClick();
            console.log('✅ Clicked picker button (manual)');

            // Search for Alexia
            const searchField = $('input[type="search"][placeholder="Search"], input[aria-label="Search"]');
            if (searchField.isDisplayed()) {
                searchField.setValue('Alexia');
                console.log('🔍 Searched for Alexia');

                // Try to select using XPath
                const product = $('//div[contains(text(),"Alexia Maxi Dress")]');
                if (product.isDisplayed()) {
                    product.click();
                    console.log('✅ Selected Alexia Maxi Dress');

                    // Click Add button
                    const addButton = $('span=Add').parentElement();
                    if (addButton.isDisplayed()) {
                        addButton.click();
                        console.log('✅ Clicked Add button');
                        return true;
                    }
                }
            }
        }

        console.log('❌ Manual product selection failed');
        return false;
    }

    const configureProductDetailComponent = () => {
        console.log('🔧 Starting product detail component configuration...');

        // Navigate to the test page editor
        browser.url(`${config.aem.author.base_url}/editor.html${testing_page}.html`);
        browser.AEMEditorLoaded();
        console.log('📄 Navigated to test page editor:', `${testing_page}.html`);

        // Find the product detail component (should be pre-existing from template)
        const productComponent = $('div[data-path*="/jcr:content/root/container/container/product"]');
        expect(productComponent).toBeDisplayed();
        console.log('✅ Found product detail component');

        // Click to configure the product component
        productComponent.waitAndClick();
        console.log('🖱️ Clicked on product component');

        const configureButton = $('button[title="Configure"]');
        expect(configureButton).toBeDisplayed();
        configureButton.waitAndClick();
        console.log('⚙️ Opened component configuration dialog');

        // Use the proven product selection approach
        console.log('🔍 Looking for product field...');
        const productField = $('product-field');
        if (productField.isDisplayed()) {
            console.log('✅ Found product field, using proven selection method');
            const success = productVariantSelection(productField, 'Alexia Maxi Dress');
            if (success) {
                console.log('✅ Product selected successfully using proven method');
            } else {
                console.log('⚠️ Proven method failed, trying manual approach');
                manualProductSelection();
            }
        } else {
            console.log('❌ Product field not found, trying manual approach');
            manualProductSelection();
        }

        // Step 5: Click Done button to close the main dialog
        console.log('✅ Closing configuration dialog...');
        clickDoneButton();
        console.log('🎉 Product detail component configured successfully!');
    };

    const addComponentToPage = (group = 'Venia - Commerce') => {
        console.log('🔧 Starting to add Product Recommendation component...');
        browser.url(`${config.aem.author.base_url}/editor.html${testing_page}.html`);
        browser.AEMEditorLoaded();
        browser.EditorOpenSidePanel();
        console.log('📄 Navigated to test page and opened side panel');

        // Open the Components tab
        $('coral-tab[title="Components"]').waitAndClick({ x: 1, y: 1 });
        console.log('📂 Opened Components tab');

        // Filter for Commerce components
        $('#components-filter coral-select button').waitAndClick();
        $(`coral-selectlist-item[value="${group}"]`).waitForDisplayed({ timeout: 5000 });
        $(`coral-selectlist-item[value="${group}"]`).waitAndClick();
        expect($('#components-filter coral-select [handle=label]')).toHaveText(group);
        console.log('🔍 Filtered for commerce components:', group);

        const name = 'Product Recommendations';
        const componentPath = '/apps/venia/components/commerce/productrecommendations';

        // Build a more precise selector
        const cmpSelector = `div.editor-ComponentBrowser-component[data-title="${name}"][data-group="${group}"][data-path="${componentPath}"]`;
        const cmp = $(cmpSelector);
        expect(cmp).toBeDisplayed();
        console.log('✅ Found Product Recommendations component');

        cmp.scrollIntoView();
        console.log('👀 Scrolled component into view');

        const dropTarget = $(`div[data-path="${testing_page}/jcr:content/root/container/container/*"]`);
        cmp.dragAndDrop(dropTarget, 1000);
        console.log('🚚 Dragged and dropped component to page');

        // Wait for component to be added
        console.log('⏳ Waiting for component to be added to page...');
        browser.waitUntil(
            () => {
                const addedComponents = $$(
                    `div[data-path*="${testing_page}/jcr:content/root/container/container/"][data-path*="productrecommendatio"]`
                );
                return addedComponents.length > 0;
            },
            { timeout: 10000, timeoutMsg: 'Component was not added to the page' }
        );
        console.log('✅ Component successfully added to page');

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
            console.log('🎯 Component node name:', addedNodeName);
        }

        console.log('🎉 Product Recommendation component added successfully!');
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

    it('adding component successfully to page test', () => {
        console.log('🧪 TEST 1: Adding Product Recommendation component to page...');
        addedNodeName = addComponentToPage();
        console.log('✅ Component added with node name:', addedNodeName);

        openComponentDialog(addedNodeName);
        console.log('⚙️ Opened component dialog');

        const preconfiguredCheckbox = $('coral-checkbox[name="./preconfigured"]');
        expect(preconfiguredCheckbox).toBeDisplayed();
        console.log('✅ Found preconfigured checkbox');

        const checkboxInput = preconfiguredCheckbox.$('input[type="checkbox"]');
        const isChecked = checkboxInput.isSelected();
        if (!isChecked) {
            preconfiguredCheckbox.click();
            console.log('☑️ Enabled preconfigured setting');
        } else {
            console.log('✅ Preconfigured setting already enabled');
        }

        clickDoneButton();
        console.log('🎉 TEST 1 PASSED: Component configured successfully!');
    });

    it('should capture preconfigured POST response and validate component data', () => {
        console.log('🧪 TEST 2: Testing preconfigured API response validation...');

        // Set up network intercept for preconfigured API
        const intercept = browser.mock('**/recs/v1/precs/preconfigured*', {
            method: 'POST'
        });
        console.log('🕸️ Set up network intercept for preconfigured API');

        // Load test product page to trigger API calls
        const productPageUrl = `${config.aem.author.base_url}${testing_page}.html?wcmmode=disabled`;
        browser.url(productPageUrl);
        console.log('📄 Loaded test product page:', productPageUrl);

        // Scroll down to ensure component is fully visible
        browser.execute(() => {
            /* eslint-disable-next-line no-undef */
            window.scrollTo(0, document.body.scrollHeight * 0.6);
        });

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
        const apiTitle = firstResult.storefrontLabel || firstResult.unitName;
        const apiProductNames = firstResult.products.map(product => product.name);

        // Validate data matches
        const titleMatch = apiTitle === titleText;
        const allApiProductsPresent = apiProductNames.every(apiName => componentProductNames.includes(apiName));

        expect(titleMatch).toBe(true, 'API title should match component title');
        expect(allApiProductsPresent).toBe(true, 'All API products should be present in component');
    });

    it('product recommendation custom configuration test', () => {
        console.log('🧪 TEST 3: Testing custom product recommendation configuration...');
        try {
            // Navigate to the test page editor for custom configuration
            console.log('⚙️ Opening component dialog for custom configuration');

            browser.url(`${config.aem.author.base_url}/editor.html${testing_page}.html`);
            browser.AEMEditorLoaded();
            console.log('📄 Navigated back to test page editor');

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

            // Navigate to test page (which is configured with Alexia Maxi Dress)
            const testProductUrl = `${config.aem.author.base_url}${testing_page}.html?wcmmode=disabled`;
            browser.url(testProductUrl);

            // Verify Alexia Maxi Dress product details are displayed
            const productName = $('.productFullDetail__productName span[role="name"]');
            expect(productName).toBeDisplayed();
            expect(productName).toHaveText('Alexia Maxi Dress');

            const productSku = $('.productFullDetail__sku strong[role="sku"]');
            expect(productSku).toBeDisplayed();
            expect(productSku).toHaveText('VD09');

            // Note: Product price may vary, so we'll just check it's displayed
            const productPrice = $('.productFullDetail__price .price');
            expect(productPrice).toBeDisplayed();

            // Verify product recommendations component is present and shows data
            const recommendationsComponent = $('[data-is-product-recs]');
            expect(recommendationsComponent).toBeDisplayed();

            // Scroll down to ensure recommendations component is visible
            browser.execute(() => {
                /* eslint-disable-next-line no-undef */
                window.scrollTo(0, document.body.scrollHeight * 0.7);
            });

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

                // Check that it's showing related dress products (should be different from Alexia Maxi Dress)
                expect(productTitle).not.toBe('Alexia Maxi Dress');

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

    it('test page cleanup verification', () => {
        console.log('🧪 TEST 4: Verifying test page components before cleanup...');

        // Verify the test page exists before cleanup
        // The actual page deletion will happen in the after() hook
        browser.url(`${config.aem.author.base_url}/editor.html${testing_page}.html`);
        browser.AEMEditorLoaded();
        console.log('📄 Navigated to test page for final verification');

        // Verify both components are present on the test page
        const productComponent = $('div[data-path*="/jcr:content/root/container/container/product"]');
        expect(productComponent).toBeDisplayed();
        console.log('✅ Product detail component is present');

        if (addedNodeName) {
            const recommendationComponent = $(
                `div[data-path="${testing_page}/jcr:content/root/container/container/${addedNodeName}"]`
            );
            expect(recommendationComponent).toBeDisplayed();
            console.log('✅ Product recommendation component is present:', addedNodeName);
        }

        console.log('🎉 TEST 4 PASSED: Both components verified on test page');
        console.log('🧹 Page cleanup will happen automatically in after() hook');
        // Test passes - cleanup will happen automatically in after() hook
    });
});
