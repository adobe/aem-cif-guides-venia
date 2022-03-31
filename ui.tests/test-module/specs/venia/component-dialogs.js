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

describe('Component Dialogs', function () {
    const editor_page = `${config.aem.author.base_url}/editor.html`;

    let testing_page;
    let onboardingHdler;

    this.retries(2);

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

    const openComponentDialog = (node, trackingId) => {
        // Open component dialog
        const cmpPlaceholder = $(`div[data-path="${testing_page}/jcr:content/root/container/container/${node}"]`);
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

        $('coral-multifield button').click();
        expect($('coral-multifield-item').$('label=Category')).toBeDisplayed();
    });

    it('opens the commerce experience fragment dialog', () => {
        addComponentToPage('Commerce Experience Fragment');
        openComponentDialog(
            'experiencefragment',
            'aem:sites:components:dialogs:cif-core-components:experiencefragment:v1'
        );

        expect($('label=Experience fragment location name.')).toBeDisplayed();
    });

    it('opens the commerce teaser dialog', () => {
        addComponentToPage('Commerce Teaser');
        openComponentDialog('teaser', 'aem:sites:components:dialogs:cif-core-components:teaser:v2');

        $('coral-tab-label=Link & Actions').click();
        expect($('label=Link')).toBeDisplayed();
    });

    it('opens the featured categories dialog', () => {
        addComponentToPage('Featured Categories');
        openComponentDialog(
            'featuredcategorylist',
            'aem:sites:components:dialogs:cif-core-components:featuredcategorylist:v1'
        );

        $('coral-multifield button').click();
        expect($('coral-multifield-item').$('label=Category')).toBeDisplayed();
    });

    it('opens the product teaser dialog', () => {
        addComponentToPage('Product Teaser');
        openComponentDialog('productteaser', 'aem:sites:components:dialogs:cif-core-components:productteaser:v1');

        expect($('label=Select Product')).toBeDisplayed();
    });

    it('opens the releated products dialog', () => {
        addComponentToPage('Related Products');
        openComponentDialog('relatedproducts', 'aem:sites:components:dialogs:cif-core-components:relatedproducts:v1');

        expect(
            $('label=Base product - Leave empty to use the current product of the generic product page.')
        ).toBeDisplayed();

        let fields = $$('.cq-dialog-content .coral-Form-fieldwrapper');
        expect(fields.length).toEqual(7);
        expect(fields[0].$('input[name="./jcr:title"]')).toBeDisplayed();
        expect(fields[1].$('coral-select[name="./titleType"]')).toBeDisplayed();
        expect(fields[2].$('coral-checkbox-label')).toHaveText('Add to Cart');
        expect(fields[2].$('input[name="./enableAddToCart"]')).toExist();
        expect(fields[3].$('coral-checkbox-label')).toHaveText('Add to Wish List');
        expect(fields[3].$('input[name="./enableAddToWishList"]')).toExist();
        expect(fields[4].$('product-field')).toBeDisplayed();
        expect(fields[4].$('input[name="./product"]')).toBeDefined();
        expect(fields[5].$('coral-select[name="./relationType"]')).toBeDisplayed();
        expect(fields[6].$('input[name="./id"]')).toBeDisplayed();
    });

    it('opens the product recommendations dialog', () => {
        addComponentToPage('Product Recommendations');
        openComponentDialog(
            'productrecommendatio',
            'aem:sites:components:dialogs:cif-core-components:productrecommendations:v1'
        );

        expect($('coral-checkbox-label=Use preconfigured recommendation')).toBeDisplayed();

        // Verify that corresponding React component is rendered on the page. If no Commerce endpoint is provided or the
        // endpoint does not have the product recommendations extension installed, the component will not render any
        // products. So the test just checks for the existence of the div element rendered by the React component.
        browser.url(`${config.aem.author.base_url}${testing_page}.html`);
        expect($('[data-is-product-recs] .cmp-ProductRecsGallery__ProductRecsGallery__root')).toExist();
    });
});
