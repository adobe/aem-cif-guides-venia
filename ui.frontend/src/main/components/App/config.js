/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
 *    This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License. You may obtain a copy
 *    of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under
 *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *    OF ANY KIND, either express or implied. See the License for the specific language
 *    governing permissions and limitations under the License.
 *
 ******************************************************************************/
export default {
    mountingPoints: {
        accountContainer: '.miniaccount__body',
        addressBookContainer: '.addressbook__body',
        authBarContainer: 'aside.navigation__root #miniaccount',
        cartTrigger: '.minicart__trigger',
        minicart: '.minicart__body',
        navPanel: 'aside.navigation__root',
        bundleProductOptionsContainer: '#bundle-product-options',
        accountDetails: '.accountdetails__body',
        resetPasswordPage: '.resetpassword__body'
    },
    pagePaths: {
        addressBook: '/content/venia/us/en/my-account/address-book.html',
        baseUrl: document.querySelector('body').dataset.storeRootUrl,
        accountDetails: '/content/venia/us/en/my-account/account-details.html',
        resetPassword: '/content/venia/us/en/reset-password.html'
    }
};
