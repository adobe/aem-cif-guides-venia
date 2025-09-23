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
        console.log('📋 Purpose: This test validates that the preconfigured product recommendations component');
        console.log('    - Makes correct API calls to Adobe Commerce recommendation service');
        console.log('    - Displays the same data returned by the API');
        console.log('    - Shows proper component structure and content');

        // Set up network intercept for preconfigured API
        const intercept = browser.mock('**/recs/v1/precs/preconfigured*', {
            method: 'POST'
        });
        console.log('🕸️ Set up network intercept for preconfigured API endpoint');
        console.log('    Intercepting: **/recs/v1/precs/preconfigured* (POST method)');

        // Load test product page to trigger API calls
        const productPageUrl = `${config.aem.author.base_url}${testing_page}.html?wcmmode=disabled`;
        console.log('📄 Loading test product page to trigger recommendation API calls...');
        console.log('    URL:', productPageUrl);
        console.log('    wcmmode=disabled ensures we see published content without editor UI');
        browser.url(productPageUrl);
        console.log('✅ Page loaded successfully');

        // Scroll down to ensure component is fully visible
        console.log('📜 Scrolling down to ensure recommendations component is in viewport...');
        browser.execute(() => {
            /* eslint-disable-next-line no-undef */
            window.scrollTo(0, document.body.scrollHeight * 0.6);
        });
        console.log('✅ Scrolled to 60% of page height to make component visible');

        // Wait for component to load using timeout
        console.log('⏳ Waiting for product recommendations component to fully load...');
        console.log('    Looking for title element: .cmp-ProductRecsGallery__ProductRecsGallery__title');
        const titleElement = $('.cmp-ProductRecsGallery__ProductRecsGallery__title');
        titleElement.waitForDisplayed({ timeout: 20000 });
        console.log('✅ Product recommendations component title is now visible');

        // Extract component data from the UI
        console.log('🔍 Extracting data from the displayed component...');
        const titleText = titleElement.getText();
        console.log('📝 Component title text:', titleText);

        const productCards = $$('.cmp-ProductRecsGallery__ProductCard__card');
        console.log('🃏 Found product recommendation cards:', productCards.length);

        const componentProductNames = [];
        console.log('📊 Extracting product names from each card:');
        productCards.forEach((card, index) => {
            const productLink = card.$('a');
            const productTitleDiv = productLink.$('div:nth-child(2)');
            const productTitle = productTitleDiv.getText();
            componentProductNames.push(productTitle);
            console.log(`    Card ${index + 1}: "${productTitle}"`);
        });
        console.log('✅ Extracted', componentProductNames.length, 'product names from UI');

        // Validate API data
        console.log('🔍 Analyzing intercepted API calls...');
        const calls = intercept.calls;
        console.log('📞 Number of API calls intercepted:', calls.length);
        expect(calls.length).toBeGreaterThan(0, 'Should intercept API calls');

        const call = calls[0];
        const requestData = call.body;
        console.log('📋 First API call request data:');
        console.log('    Method:', call.method);
        console.log('    URL:', call.url);
        console.log('    Request body keys:', Object.keys(requestData));

        const firstResult = requestData.results[0];
        console.log('🎯 First recommendation result from API:');
        console.log('    Unit ID:', firstResult.unitId);
        console.log('    Unit Name:', firstResult.unitName);
        console.log('    Storefront Label:', firstResult.storefrontLabel);
        console.log('    Number of products:', firstResult.products.length);

        const apiTitle = firstResult.storefrontLabel || firstResult.unitName;
        const apiProductNames = firstResult.products.map(product => product.name);
        console.log('📝 API title for comparison:', apiTitle);
        console.log('📊 API product names:');
        apiProductNames.forEach((name, index) => {
            console.log(`    API Product ${index + 1}: "${name}"`);
        });

        // Validate data matches between API and UI
        console.log('🔍 Comparing API data with UI component data...');
        const titleMatch = apiTitle === titleText;
        console.log('📝 Title comparison:');
        console.log('    API Title:', `"${apiTitle}"`);
        console.log('    UI Title:', `"${titleText}"`);
        console.log('    Titles match:', titleMatch ? '✅ YES' : '❌ NO');

        const allApiProductsPresent = apiProductNames.every(apiName => componentProductNames.includes(apiName));
        console.log('📊 Product comparison:');
        console.log('    All API products present in UI:', allApiProductsPresent ? '✅ YES' : '❌ NO');

        // Detailed product matching analysis
        console.log('🔍 Detailed product matching analysis:');
        apiProductNames.forEach((apiName, index) => {
            const isPresent = componentProductNames.includes(apiName);
            console.log(
                `    API Product ${index + 1}: "${apiName}" - ${isPresent ? '✅ Found in UI' : '❌ Missing from UI'}`
            );
        });

        // Final validation assertions
        console.log('🧪 Running final test assertions...');
        expect(titleMatch).toBe(true, 'API title should match component title');
        console.log('✅ Title assertion passed');

        expect(allApiProductsPresent).toBe(true, 'All API products should be present in component');
        console.log('✅ Product presence assertion passed');

        console.log('🎉 TEST 2 PASSED: API data matches UI component display perfectly!');
        console.log('📋 Summary: The preconfigured recommendations are working correctly');
    });

    it('product recommendation custom configuration test', () => {
        console.log('🧪 TEST 3: Testing custom product recommendation configuration...');
        console.log('📋 Purpose: This test validates custom recommendation configuration');
        console.log('    - Configures component with custom title and recommendation type');
        console.log('    - Verifies the test page shows Alexia Maxi Dress product correctly');
        console.log('    - Validates "more-like-this" recommendations appear with custom title');
        console.log('    - Tests all UI elements of recommendation cards');

        try {
            // Navigate to the test page editor for custom configuration
            console.log('⚙️ Step 1: Opening component dialog for custom configuration...');
            const editorUrl = `${config.aem.author.base_url}/editor.html${testing_page}.html`;
            console.log('📄 Navigating to test page editor:', editorUrl);

            browser.url(editorUrl);
            browser.AEMEditorLoaded();
            console.log('✅ Successfully navigated to test page editor');
            console.log('📝 Page loaded with AEM editor interface');

            console.log('🔧 Opening component configuration dialog...');
            console.log('    Component node name:', addedNodeName);
            openComponentDialog(addedNodeName);
            console.log('✅ Component configuration dialog is now open');

            // Check if "Use preconfigured recommendation" checkbox is checked
            console.log('⚙️ Step 2: Configuring component settings...');
            console.log('🔍 Looking for preconfigured recommendation checkbox...');
            const preconfiguredCheckbox = $('coral-checkbox[name="./preconfigured"]');
            expect(preconfiguredCheckbox).toBeDisplayed();
            console.log('✅ Found preconfigured checkbox element');

            // Check if checkbox is checked
            const checkboxInput = preconfiguredCheckbox.$('input[type="checkbox"]');
            const isChecked = checkboxInput.isSelected();
            console.log('📋 Current preconfigured checkbox state:', isChecked ? 'CHECKED ✅' : 'UNCHECKED ❌');

            if (isChecked) {
                console.log('🔄 Unchecking preconfigured checkbox to enable custom configuration...');
                preconfiguredCheckbox.waitAndClick();
                console.log('✅ Successfully unchecked - custom configuration now enabled');
            } else {
                console.log('✅ Already unchecked - custom configuration is enabled');
            }

            // Set custom title value
            console.log('📝 Setting custom title for recommendations...');
            const titleInput = $('input[name="./jcr:title"]');
            expect(titleInput).toBeDisplayed();
            console.log('✅ Found title input field');

            const currentTitle = titleInput.getValue();
            console.log('📋 Current title value:', `"${currentTitle}"`);

            titleInput.clearValue();
            titleInput.setValue('Recommended products test');
            console.log('✅ Set custom title to: "Recommended products test"');

            // Select recommendation type from dropdown
            console.log('🎯 Setting recommendation type to "more-like-this"...');
            const recommendationTypeDropdown = $('coral-select[name="./recommendationType"]');
            expect(recommendationTypeDropdown).toBeDisplayed();
            console.log('✅ Found recommendation type dropdown');

            console.log('📂 Opening recommendation type dropdown...');
            recommendationTypeDropdown.waitAndClick();
            console.log('✅ Dropdown opened - showing available options');

            console.log('🔍 Looking for "more-like-this" option...');
            const moreLikeThisOption = $('coral-selectlist-item[value="more-like-this"]');
            moreLikeThisOption.waitForDisplayed({ timeout: 5000 });
            console.log('✅ Found "more-like-this" option');

            moreLikeThisOption.waitAndClick();
            console.log('✅ Selected "more-like-this" recommendation type');
            console.log('📋 This will show products similar to the current product');

            // Click Done button to close dialog (handles both Cloud and 6.5 versions)
            console.log('💾 Saving configuration and closing dialog...');
            clickDoneButton();
            console.log('✅ Configuration saved successfully');

            // Navigate to test page (which is configured with Alexia Maxi Dress)
            console.log('⚙️ Step 3: Testing the configured page...');
            const testProductUrl = `${config.aem.author.base_url}${testing_page}.html?wcmmode=disabled`;
            console.log('📄 Navigating to test page to verify configuration:', testProductUrl);
            console.log('📝 wcmmode=disabled shows published content without editor overlay');
            browser.url(testProductUrl);
            console.log('✅ Test page loaded successfully');

            // Verify Alexia Maxi Dress product details are displayed
            console.log('🔍 Step 4: Verifying main product details (Alexia Maxi Dress)...');
            console.log('👗 Looking for product name element...');
            const productName = $('.productFullDetail__productName span[role="name"]');
            expect(productName).toBeDisplayed();
            console.log('✅ Product name element found and displayed');

            const actualProductName = productName.getText();
            console.log('📝 Product name displayed:', `"${actualProductName}"`);
            expect(productName).toHaveText('Alexia Maxi Dress');
            console.log('✅ Product name validation passed: "Alexia Maxi Dress"');

            console.log('🏷️ Looking for product SKU element...');
            const productSku = $('.productFullDetail__sku strong[role="sku"]');
            expect(productSku).toBeDisplayed();
            console.log('✅ Product SKU element found and displayed');

            const actualSku = productSku.getText();
            console.log('📝 Product SKU displayed:', `"${actualSku}"`);
            expect(productSku).toHaveText('VD09');
            console.log('✅ Product SKU validation passed: "VD09"');

            console.log('💰 Looking for product price element...');
            const productPrice = $('.productFullDetail__price .price');
            expect(productPrice).toBeDisplayed();
            console.log('✅ Product price element found and displayed');

            const actualPrice = productPrice.getText();
            console.log('📝 Product price displayed:', `"${actualPrice}"`);
            console.log('✅ Product price validation passed (price may vary)');

            // Verify product recommendations component is present and shows data
            console.log('⚙️ Step 5: Verifying product recommendations component...');
            console.log('🔍 Looking for recommendations component...');
            const recommendationsComponent = $('[data-is-product-recs]');
            expect(recommendationsComponent).toBeDisplayed();
            console.log('✅ Product recommendations component found and displayed');

            // Scroll down to ensure recommendations component is visible
            console.log('📜 Scrolling to ensure recommendations are fully visible...');
            browser.execute(() => {
                /* eslint-disable-next-line no-undef */
                window.scrollTo(0, document.body.scrollHeight * 0.7);
            });
            console.log('✅ Scrolled to 70% of page height');

            // Verify custom title is displayed
            console.log('📝 Verifying custom title is displayed correctly...');
            const recommendationsTitle = $('.cmp-ProductRecsGallery__ProductRecsGallery__title');
            expect(recommendationsTitle).toBeDisplayed();
            console.log('✅ Recommendations title element found and displayed');

            const actualTitle = recommendationsTitle.getText();
            console.log('📝 Displayed recommendations title:', `"${actualTitle}"`);
            expect(recommendationsTitle).toHaveText('Recommended products test');
            console.log('✅ Custom title validation passed: "Recommended products test"');

            // Verify that product recommendation cards are displayed
            console.log('🃏 Verifying product recommendation cards...');
            const recommendationCards = $$('.cmp-ProductRecsGallery__ProductCard__card');
            console.log('📊 Number of recommendation cards found:', recommendationCards.length);
            expect(recommendationCards.length).toBeGreaterThan(0);
            console.log('✅ At least one recommendation card is displayed');

            // Verify first few recommendation cards contain product data
            console.log('⚙️ Step 6: Detailed validation of recommendation cards...');
            const cardsToTest = Math.min(3, recommendationCards.length);
            console.log(`🔍 Testing first ${cardsToTest} recommendation cards in detail:`);

            for (let i = 0; i < cardsToTest; i++) {
                console.log(`\n🃏 Testing Card ${i + 1}:`);
                const card = recommendationCards[i];

                // Check product image
                console.log('    🖼️ Looking for product image...');
                const productImage = card.$('.cmp-ProductRecsGallery__ProductCard__productImage');
                expect(productImage).toBeDisplayed();
                console.log('    ✅ Product image found and displayed');

                // Check product title/name
                console.log('    📝 Looking for product link and title...');
                const productLink = card.$('a[title]');
                expect(productLink).toBeDisplayed();
                console.log('    ✅ Product link found and displayed');

                const productTitle = productLink.getAttribute('title');
                console.log('    📋 Product title:', `"${productTitle}"`);
                expect(productTitle).not.toBe('');
                expect(productTitle).not.toBe(null);
                console.log('    ✅ Product title is not empty');

                // Check that it's showing related dress products (should be different from Alexia Maxi Dress)
                const isDifferentProduct = productTitle !== 'Alexia Maxi Dress';
                console.log('    🔍 Is different from main product:', isDifferentProduct ? '✅ YES' : '❌ NO');
                expect(productTitle).not.toBe('Alexia Maxi Dress');
                console.log('    ✅ Showing related product (not the main product)');

                // Verify Add to Cart button
                console.log('    🛒 Looking for Add to Cart button...');
                const addToCartBtn = card.$('.cmp-ProductRecsGallery__ProductCard__addToCart');
                expect(addToCartBtn).toBeDisplayed();
                console.log('    ✅ Add to Cart button found and displayed');

                // Verify Add to Wishlist button
                console.log('    💖 Looking for Add to Wishlist button...');
                const addToWishlistBtn = card.$('.cmp-ProductRecsGallery__ProductCard__addToWishlist');
                expect(addToWishlistBtn).toBeDisplayed();
                console.log('    ✅ Add to Wishlist button found and displayed');

                console.log(`    🎉 Card ${i + 1} validation completed successfully!`);
            }

            console.log('\n🎉 TEST 3 PASSED: Custom product recommendation configuration working perfectly!');
            console.log('📋 Summary of what was validated:');
            console.log('    ✅ Component configured with custom title: "Recommended products test"');
            console.log('    ✅ Recommendation type set to: "more-like-this"');
            console.log('    ✅ Main product displayed correctly: "Alexia Maxi Dress" (VD09)');
            console.log('    ✅ Custom recommendations title displayed correctly');
            console.log('    ✅ Multiple recommendation cards displayed');
            console.log('    ✅ Each card has: image, title, Add to Cart, Add to Wishlist');
            console.log('    ✅ Related products shown (different from main product)');
        } finally {
            console.log('🔧 Keeping component for other tests - no cleanup here');
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
