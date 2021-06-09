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
const { OnboardingDialogHandler } = require('../../lib/commons');

describe('XF Page Component Dialog', function () {
    const xf_properties = `${config.aem.author.base_url}/mnt/overlay/wcm/core/content/sites/properties.html`;
    const sample_xf = '/content/experience-fragments/venia/us/en/site/products/promotions/master';

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

    it('XF dialog extensions are displayed when CIF Add-On is present', () => {
        // Open XF properties
        browser.url(`${xf_properties}?item=${encodeURIComponent(`${sample_xf}`)}`);

        // Open Commerce tab
        let commerceTab = $('coral-tab-label=Commerce');
        expect(commerceTab).toBeDisplayed();
        commerceTab.click();

        expect($('h3=Commerce Pages')).toBeDisplayed();
        expect($('h3=Product selection')).toBeDisplayed();
        expect($('h3=Category selection')).toBeDisplayed();
        expect($('h3=Catalog placeholder location')).toBeDisplayed();
    });
});
