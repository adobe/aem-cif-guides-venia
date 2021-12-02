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

const storeConfigEl = document.querySelector('meta[name="store-config"]');
let storeConfig;
let headers;

if (storeConfigEl) {
    storeConfig = JSON.parse(storeConfigEl.content);
    headers = storeConfig.headers;
} else {
    // TODO: deprecated - the store configuration on the <body> has been deprecated and will be removed
    storeConfig = document.body.dataset;
    headers = JSON.parse(storeConfig.httpHeaders);
}

const baseUrl = storeConfig.storeRootUrl;
const basePath = baseUrl.substr(0, baseUrl.indexOf('.'));

// necessary to be set for venia-ui components
window.STORE_VIEW_CODE = storeConfig.storeView || '';
window.AVAILABLE_STORE_VIEWS = [
    {
        code: storeConfig.storeView,
        base_currency_code: 'USD',
        default_display_currency_code: 'USD',
        id: 1,
        locale: 'en',
        secure_base_media_url: '',
        store_name: 'Venia'
    }
];

export default {
    storeView: storeConfig.storeView,
    graphqlEndpoint: storeConfig.graphqlEndpoint,
    // Can be GET or POST. When selecting GET, this applies to cache-able GraphQL query requests only. Mutations
    // will always be executed as POST requests.
    graphqlMethod: storeConfig.graphqlMethod,
    headers,

    mountingPoints: {
        accountContainer: '.miniaccount__body',
        addressBookContainer: '.addressbook__body',
        authBarContainer: 'aside.navigation__root #miniaccount',
        cartTrigger: '.minicart__trigger',
        minicart: '.minicart__body',
        navPanel: 'aside.navigation__root',
        bundleProductOptionsContainer: '#bundle-product-options',
        giftCardProductOptionsContainer: '#gift-card-product-options',
        accountDetails: '.accountdetails__body',
        resetPasswordPage: '.resetpassword__body',
        productRecs: '[data-is-product-recs]',
        cartDetailsContainer: '.cartcontainer__body',
        checkoutPageContainer: '.checkoutpage__body',
        orderHistoryPageContainer: '.orderhistory__body',
        wishlistPageContainer: '.wishlist__body'
    },
    pagePaths: {
        baseUrl,
        addressBook: `${basePath}/my-account/address-book.html`,
        accountDetails: `${basePath}/my-account/account-details.html`,
        cartDetails: `${basePath}/cart-details.html`,
        checkoutPage: `${basePath}/checkout.html`,
        orderHistory: `${basePath}/my-account/order-history.html`,
        wishlist: `${basePath}/my-account/wishlist.html`
    }
};
