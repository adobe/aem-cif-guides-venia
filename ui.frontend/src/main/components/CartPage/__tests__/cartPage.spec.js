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
import React from 'react';
import ReactDOM from 'react-dom';
import { useCartPage } from '@magento/peregrine/lib/talons/CartPage/useCartPage';

import render from '../../utils/test-utils';
import CartPage from '../cartPage';

jest.mock('@magento/venia-ui/lib/components/StockStatusMessage', () => 'StockStatusMessage');
jest.mock('@magento/venia-ui/lib/components/CartPage/PriceAdjustments', () => 'PriceAdjustments');
jest.mock('../PriceSummary', () => 'PriceSummary');
jest.mock('../ProductListing', () => 'ProductListing');

jest.mock('@magento/peregrine/lib/talons/CartPage/useCartPage', () => {
    const useCartPageTalon = jest.requireActual('@magento/peregrine/lib/talons/CartPage/useCartPage');
    const spy = jest.spyOn(useCartPageTalon, 'useCartPage');

    return Object.assign(useCartPageTalon, { useCartPage: spy });
});

jest.mock('@magento/peregrine/lib/Toasts/useToasts', () => ({
    useToasts: () => [{}, { addToast: jest.fn() }]
}));

const talonProps = {
    hasItems: false,
    handleSignIn: jest.fn().mockName('handleSignIn'),
    isSignedIn: false,
    isCartUpdating: false,
    setIsCartUpdating: jest.fn().mockName('setIsCartUpdating'),
    shouldShowLoadingIndicator: false,
    onAddToWishlistSuccess: jest.fn().mockName('onAddToWishlistSuccess'),
    wishlistSuccessProps: null
};

describe('cartPage', () => {
    beforeAll(() => {
        /**
         * Mocking ReactDOM.createPortal because of incompatabilities
         * between ReactDOM and react-test-renderer.
         *
         * More info: https://github.com/facebook/react/issues/11565
         */
        ReactDOM.createPortal = jest.fn(element => {
            return element;
        });
    });

    afterAll(() => {
        ReactDOM.createPortal.mockClear();
    });

    test('renders a loading indicator when talon indicates', () => {
        // Arrange.
        const myTalonProps = {
            ...talonProps,
            shouldShowLoadingIndicator: true
        };
        useCartPage.mockReturnValueOnce(myTalonProps);

        // Act.
        const instance = render(<CartPage />);

        // Assert.
        expect(instance.toJSON()).toMatchSnapshot();
    });

    test('renders empty cart text (no adjustments, list or summary) if cart is empty', () => {
        // Arrange.
        useCartPage.mockReturnValueOnce(talonProps);

        // Act.
        const instance = render(<CartPage />);

        // Assert.
        expect(instance.toJSON()).toMatchSnapshot();
    });

    test('renders components if cart has items', () => {
        // Arrange.
        const myTalonProps = {
            ...talonProps,
            hasItems: true
        };
        useCartPage.mockReturnValueOnce(myTalonProps);

        // Act.
        const instance = render(<CartPage />);

        // Assert.
        expect(instance.toJSON()).toMatchSnapshot();
    });
});
