/*******************************************************************************
 *
 *    Copyright 2021 Adobe. All rights reserved.
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
const globals = {
    POSSIBLE_TYPES: {
        CartAddressInterface: ['BillingCartAddress', 'ShippingCartAddress'],
        CartItemInterface: [
            'SimpleCartItem',
            'VirtualCartItem',
            'DownloadableCartItem',
            'BundleCartItem',
            'ConfigurableCartItem'
        ],
        ProductInterface: [
            'VirtualProduct',
            'SimpleProduct',
            'DownloadableProduct',
            'GiftCardProduct',
            'BundleProduct',
            'GroupedProduct',
            'ConfigurableProduct'
        ],
        CategoryInterface: ['CategoryTree'],
        MediaGalleryInterface: ['ProductImage', 'ProductVideo'],
        ProductLinksInterface: ['ProductLinks'],
        AggregationOptionInterface: ['AggregationOption'],
        LayerFilterItemInterface: ['LayerFilterItem', 'SwatchLayerFilterItem'],
        PhysicalProductInterface: [
            'SimpleProduct',
            'GiftCardProduct',
            'BundleProduct',
            'GroupedProduct',
            'ConfigurableProduct'
        ],
        CustomizableOptionInterface: [
            'CustomizableAreaOption',
            'CustomizableDateOption',
            'CustomizableDropDownOption',
            'CustomizableMultipleOption',
            'CustomizableFieldOption',
            'CustomizableFileOption',
            'CustomizableRadioOption',
            'CustomizableCheckboxOption'
        ],
        CustomizableProductInterface: [
            'VirtualProduct',
            'SimpleProduct',
            'DownloadableProduct',
            'GiftCardProduct',
            'BundleProduct',
            'ConfigurableProduct'
        ],
        SwatchDataInterface: [
            'ImageSwatchData',
            'TextSwatchData',
            'ColorSwatchData'
        ],
        SwatchLayerFilterItemInterface: ['SwatchLayerFilterItem']
    },
    STORE_NAME: 'Venia',
    STORE_VIEW_CODE: 'default',
    AVAILABLE_STORE_VIEWS: [
        {
            base_currency_code: 'USD',
            code: 'default',
            default_display_currency_code: 'USD',
            id: 1,
            locale: 'en_US',
            store_name: 'Default Store View'
        },
        {
            base_currency_code: 'EUR',
            code: 'fr',
            default_display_currency_code: 'EUR',
            id: 2,
            locale: 'fr_FR',
            store_name: 'French Store View'
        }
    ],
    DEFAULT_LOCALE: 'en-US',
    DEFAULT_COUNTRY_CODE: 'US'
};
module.exports = {
    globals: globals,
    collectCoverage: true,
    moduleDirectories: ['<rootDir>/node_modules', 'node_modules'],
    moduleFileExtensions: ['ee.js', 'ce.js', 'js', 'json', 'jsx', 'node'],
    coverageDirectory: '<rootDir>/coverage',
    coverageReporters: ['json', 'lcov'],
    coveragePathIgnorePatterns: ['<rootDir>/src/queries', '\\.(gql|graphql)$'],
    testPathIgnorePatterns: ['<rootDir>/node_modules/'],
    reporters: ['default', ['jest-junit', { outputDirectory: './test-results' }]],
    transform: {
        '\\.(gql|graphql)$': 'jest-transform-graphql',
        '.+\\.(js|jsx|ts|tsx)$': 'babel-jest'
    },
    moduleNameMapper: {
        '\\.css$': 'identity-obj-proxy',
        '\\.svg$': 'identity-obj-proxy',
        '.+\\.(jpg|jpeg|png|gif|eot|otf|webp|svg|ttf|woff|woff2|mp4|webm|wav|mp3|m4a|aac|oga)$':
            '<rootDir>/__mocks__/fileMock.js'
    },
    transformIgnorePatterns: ['node_modules/(?!@magento/)']
};
