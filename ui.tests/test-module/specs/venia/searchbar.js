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

describe.skip('Venia Searchbar Component', () => {
    const venia_homepage = `${config.aem.author.base_url}/content/venia/us/en.html`;

    before(() => {
        // AEM Login
        browser.AEMForceLogout();
        browser.url(config.aem.author.base_url);
        browser.AEMLogin(config.aem.author.username, config.aem.author.password);
    });

    beforeEach(() => {
        // Set window size to desktop
        browser.setWindowSize(1280, 960);
    });

    it('should render the searchbar', () => {
        // Go to the Venia homepage
        browser.url(venia_homepage);

        // Check that searchbar trigger button is displayed
        const searchBarTrigger = $('.searchbar__trigger');
        expect(searchBarTrigger).toBeDisplayed();
        // and the searchbar is hidden
        let searchBar = $('.searchbar__body');
        expect(searchBar).not.toBeDisplayed();

        searchBarTrigger.click();
        searchBar = $('.searchbar__body');
        expect(searchBar).toBeDisplayed();
        const input = searchBar.$('.searchbar__input');
        expect(input).toHaveAttr('placeholder', 'Search');
    });

    it('should display the reset button', () => {
        // Go to the Venia homepage
        browser.url(venia_homepage);

        // Check that search trigger button is displayed
        const searchBarTrigger = $('.searchbar__trigger');
        expect(searchBarTrigger).toBeDisplayed();
        searchBarTrigger.click();
        const searchBar = $('.searchbar__body');
        expect(searchBar).toBeDisplayed();
        const input = searchBar.$('.searchbar__input');
        input.setValue('dress');
        const reset = searchBar.$('.searchbar__reset-button');
        expect(reset).toBeDisplayed();
        expect(reset).toBeClickable();
        reset.click();
        expect(input).toHaveText('');
    });

    it('honors the placeholder property', () => {
        // Open Venia header in the XF editor
        let xfPath = `${config.aem.author.base_url}/editor.html/content/experience-fragments/venia/us/en/site/header/master.html`;
        browser.url(xfPath);
        let searchBar = $(
            'div[data-path="/content/experience-fragments/venia/us/en/site/header/master/jcr:content/root/searchbar"]'
        );
        // open the searchbar edit dialog
        expect(searchBar).toBeDisplayed();
        searchBar.click();
        let configureButton = $('button[title="Configure"]');
        expect(configureButton).toBeDisplayed();
        configureButton.click();
        let dialog = $('coral-dialog[trackingfeature="aem:sites:components:dialogs:cif-core-components:searchbar:v2"]');
        expect(dialog).toBeDisplayed();

        // update the placeholder field
        let placeholderInput = dialog.$('input[name="./placeholder"]');
        expect(placeholderInput).toBeDisplayed();
        let searchProductsText = 'Search products';
        placeholderInput.setValue(searchProductsText);
        let doneButton = dialog.$('.cq-dialog-submit');
        expect(doneButton).toBeDisplayed();
        doneButton.click();

        // Go to the Venia homepage
        browser.url(venia_homepage);
        browser.refresh();

        // check the new placeholder in the trigger title and in the input field
        const searchBarTrigger = $('.searchbar__trigger');
        expect(searchBarTrigger).toBeDisplayed();
        expect(searchBarTrigger).toHaveAttr('title', searchProductsText);

        searchBarTrigger.click();
        searchBar = $('.searchbar__body');
        expect(searchBar).toBeDisplayed();
        const input = searchBar.$('.searchbar__input');
        expect(input).toHaveAttr('placeholder', searchProductsText);

        // clear the placeholder
        browser.url(xfPath);
        searchBar = $(
            'div[data-path="/content/experience-fragments/venia/us/en/site/header/master/jcr:content/root/searchbar"]'
        );
        expect(searchBar).toBeDisplayed();
        searchBar.click();
        $('button[title="Configure"]').click();
        dialog = $('coral-dialog[trackingfeature="aem:sites:components:dialogs:cif-core-components:searchbar:v2"]');
        dialog.$('input[name="./placeholder"]').setValue('');
        dialog.$('.cq-dialog-submit').click();
    });
});
