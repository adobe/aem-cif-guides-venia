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
        // Open the page editor
        browser.url(`${editor_page}${testing_page}.html`);
        browser.AEMEditorLoaded();
        browser.EditorOpenSidePanel();

        // Open the Components tab
        $('coral-tab[title="Components"]').waitAndClick({ x: 1, y: 1 });

        // Filter for Commerce components
        $('#components-filter coral-select button').waitAndClick();
        browser.pause(2000);

        $(`coral-selectlist-item[value="${group}"]`).waitAndClick();
        expect($('#components-filter coral-select [handle=label]')).toHaveText(group);

        const component = $(`div[data-title="${name}"]`);
        component.scrollIntoView();

        browser.pause(2000);

        // Check if the component is displayed after scrolling
        expect(component).toBeDisplayed();

        // Ensure the selected component is visible and drag it to the page
        component.waitForDisplayed();
        const dropTarget = $(`div[data-path="${testing_page}/jcr:content/root/container/container/*"]`);
        component.dragAndDrop(dropTarget, 1000);
    };

    const openComponentDialog = (node, trackingId) => {
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

        let fields = $$('.cq-dialog-content .coral-Form-fieldwrapper');
        expect(fields.length).toEqual(5);
        expect(fields[0].$('input[name="./jcr:title"]')).toBeDisplayed();
        expect(fields[1].$('coral-select[name="./titleType"]')).toBeDisplayed();
        expect(fields[2].$('coral-checkbox[name="./linkTarget"]')).toBeDisplayed();
        expect(fields[3].$('coral-multifield[data-granite-coral-multifield-name="./items"]')).toBeDisplayed();
        expect(fields[4].$('input[name="./id"]')).toBeDisplayed();

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

    it('opens the featured categories dialog', () => {
        addComponentToPage('Featured Categories');
        openComponentDialog(
            'featuredcategorylist',
            'aem:sites:components:dialogs:cif-core-components:featuredcategorylist:v1'
        );

        let fields = $$('.cq-dialog-content .coral-Form-fieldwrapper');
        expect(fields.length).toEqual(5);
        expect(fields[0].$('input[name="./jcr:title"]')).toBeDisplayed();
        expect(fields[1].$('coral-select[name="./titleType"]')).toBeDisplayed();
        expect(fields[2].$('coral-checkbox[name="./linkTarget"]')).toBeDisplayed();
        expect(fields[3].$('coral-multifield[data-granite-coral-multifield-name="./items"]')).toBeDisplayed();
        expect(fields[4].$('input[name="./id"]')).toBeDisplayed();

        $('coral-multifield button').click();
        expect($('coral-multifield-item').$('label=Category')).toBeDisplayed();
    });

    it('opens the product teaser dialog', () => {
        addComponentToPage('Product Teaser');
        openComponentDialog('productteaser', 'aem:sites:components:dialogs:cif-core-components:productteaser:v1');

        expect($('label=Select Product')).toBeDisplayed();

        let fields = $$('.cq-dialog-content .coral-Form-fieldwrapper');
        expect(fields.length).toEqual(8);

        // fields of the OOTB component
        expect(fields[0].$('product-field')).toBeDisplayed();
        expect(fields[0].$('input[name="./selection"]')).toBeDefined();
        expect(fields[1].$('coral-select[name="./cta"]')).toBeDisplayed();
        expect(fields[2].$('input[name="./ctaText"]')).toBeDisplayed();
        expect(fields[3].$('coral-checkbox[name="./linkTarget"]')).toBeDisplayed();
        expect(fields[4].$('input[name="./id"]')).toBeDisplayed();
    });

    it('opens the releated products dialog', () => {
        addComponentToPage('Related Products');

        openComponentDialog('relatedproducts', 'aem:sites:components:dialogs:cif-core-components:relatedproducts:v1');

        expect(
            $('label=Base product - Leave empty to use the current product of the generic product page.')
        ).toBeDisplayed();

        let fields = $$('.cq-dialog-content .coral-Form-fieldwrapper');
        expect(fields.length).toEqual(8);
        expect(fields[0].$('input[name="./jcr:title"]')).toBeDisplayed();
        expect(fields[1].$('coral-select[name="./titleType"]')).toBeDisplayed();
        expect(fields[2].$('coral-checkbox[name="./linkTarget"]')).toBeDisplayed();
        expect(fields[3].$('coral-checkbox-label')).toHaveText('Add to Cart');
        expect(fields[3].$('input[name="./enableAddToCart"]')).toExist();
        expect(fields[4].$('coral-checkbox-label')).toHaveText('Add to Wish List');
        expect(fields[4].$('input[name="./enableAddToWishList"]')).toExist();
        expect(fields[5].$('product-field')).toBeDisplayed();
        expect(fields[5].$('input[name="./product"]')).toBeDefined();
        expect(fields[6].$('coral-select[name="./relationType"]')).toBeDisplayed();
        expect(fields[7].$('input[name="./id"]')).toBeDisplayed();
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

    it('opens the searchresults dialog', () => {
        addComponentToPage('Search Results');
        openComponentDialog('searchresults', 'aem:sites:components:dialogs:cif-core-components:searchresults:v2');

        let fields = $$('.cq-dialog-content .coral-Form-fieldwrapper');

        // check fields
        expect(fields.length).toEqual(3);
        expect(fields[0].$('label')).toHaveText('Page Size');
        expect(fields[0].$('input[name="./pageSize"]')).toExist();
        expect(fields[0].nextElement()).toHaveElementClass('coral-Well');
        expect(fields[0].nextElement().$('input[name="./defaultSortField"]')).toExist();
        expect(fields[0].nextElement().$('input[name="./defaultSortOrder"]')).toExist();
        expect(fields[2].$('label')).toHaveText('ID');
        expect(fields[2].$('input[name="./id"]')).toExist();
    });

    it('opens the commerce list dialog', () => {
        addComponentToPage('List', 'Venia - Content');
        openComponentDialog('list', 'aem:sites:components:dialogs:cif-core-components:list:v1');

        let fields = $$('.cq-dialog-content .coral-Form-fieldwrapper');

        expect(fields[9].$('product-field')).toExist();
        expect(fields[9].$('product-field')).not.toBeDisplayed();
        expect(fields[10].$('category-field')).toExist();
        expect(fields[10].$('category-field')).not.toBeDisplayed();

        // check fields
        expect(fields[0].$('coral-select')).toHaveAttribute('name', './listFrom');
        let selectItems = fields[0].$$('coral-select coral-select-item');

        expect(selectItems.length).toEqual(6);
        expect(selectItems[4]).toHaveValue('productAssociation');
        expect(selectItems[5]).toHaveValue('categoryAssociation');

        fields[0].$('coral-select').click();

        let itemsSelector =
            config.aem.type === 'classic'
                ? 'coral-overlay.is-open coral-selectlist-item[value="productAssociation"]'
                : 'coral-popover-content coral-selectlist-item[value="productAssociation"]';
        $(itemsSelector).click();
        expect(fields[9].$('product-field')).toBeDisplayed();
        expect(fields[10].$('category-field')).not.toBeDisplayed();

        fields[0].$('coral-select').click();
        itemsSelector =
            config.aem.type === 'classic'
                ? 'coral-overlay.is-open coral-selectlist-item[value="categoryAssociation"]'
                : 'coral-popover-content coral-selectlist-item[value="categoryAssociation"]';
        $(itemsSelector).click();
        expect(fields[9].$('product-field')).not.toBeDisplayed();
        expect(fields[10].$('category-field')).toBeDisplayed();
    });
});
