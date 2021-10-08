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
import { usePriceSummary } from '@magento/peregrine/lib/talons/CartPage/PriceSummary/usePriceSummary';
import PriceSummary from '../priceSummary';

import render from '../../../utils/test-utils';

jest.mock('@adobe/aem-core-cif-react-components', () => ({
    useConfigContext: jest.fn(() => ({
        pagePaths: {
            checkoutPage: '/content/venia/us/en/checkout.html'
        }
    }))
}));
jest.mock('react-router-dom', () => ({
    Link: jest.fn(() => 'Proceed to Checkout')
}));
jest.mock('@apollo/client', () => {
    const runQuery = jest.fn();
    const queryResult = {
        data: {
            cart: {
                items: [
                    {
                        quantity: 1
                    }
                ],
                applied_gift_cards: [],
                shipping_addresses: [
                    {
                        selected_shipping_method: {
                            amount: {
                                value: 0,
                                currency: 'USD'
                            }
                        }
                    }
                ],
                prices: {
                    subtotal_excluding_tax: {
                        currency: 'USD',
                        value: 11
                    },
                    grand_total: {
                        currency: 'USD',
                        value: 10
                    },
                    discounts: [
                        {
                            amount: {
                                value: 1,
                                currency: 'USD'
                            }
                        }
                    ],
                    applied_taxes: [
                        {
                            amount: {
                                value: 0,
                                currency: 'USD'
                            }
                        }
                    ]
                }
            }
        },
        error: null,
        loading: false
    };
    const useLazyQuery = jest.fn(() => [runQuery, queryResult]);

    return {
        gql: jest.fn(),
        useLazyQuery
    };
});

const defaultTalonProps = {
    hasError: false,
    hasItems: true,
    isCheckout: false,
    isLoading: false,
    flatData: {
        subtotal: { currency: 'USD', value: 3.5 },
        total: { currency: 'USD', value: 8.5 },
        discounts: null,
        giftCards: [],
        taxes: [],
        shipping: { currency: 'USD', value: 5 }
    }
};

jest.mock('@magento/peregrine/lib/talons/CartPage/PriceSummary/usePriceSummary', () => ({
    usePriceSummary: jest.fn().mockReturnValue({
        hasError: false,
        hasItems: true,
        isCheckout: false,
        isLoading: false,
        flatData: {
            subtotal: { currency: 'USD', value: 3.5 },
            total: { currency: 'USD', value: 8.5 },
            discounts: null,
            giftCards: [],
            taxes: [],
            shipping: { currency: 'USD', value: 5 }
        }
    })
}));

jest.mock('@magento/peregrine/lib/context/cart', () => {
    const state = { cartId: 'cart123' };
    const api = {};
    const useCartContext = jest.fn(() => [state, api]);

    return { useCartContext };
});

test('renders PriceSummary correctly on cart page', () => {
    const tree = render(<PriceSummary />);

    expect(tree.toJSON()).toMatchSnapshot();
});

test('renders PriceSummary correctly on checkout page', () => {
    usePriceSummary.mockReturnValueOnce({
        ...defaultTalonProps,
        isCheckout: true
    });

    const tree = render(<PriceSummary />);

    expect(tree.toJSON()).toMatchSnapshot();
});

test('renders an error state if query fails', () => {
    usePriceSummary.mockReturnValueOnce({
        ...defaultTalonProps,
        hasError: true
    });

    const tree = render(<PriceSummary />);

    expect(tree.toJSON()).toMatchSnapshot();
});

test('renders summary with loading state if query is loading', () => {
    usePriceSummary.mockReturnValueOnce({
        ...defaultTalonProps,
        isLoading: true
    });

    const tree = render(<PriceSummary />);

    expect(tree.toJSON()).toMatchSnapshot();
});

test('renders nothing if query returns no items', () => {
    usePriceSummary.mockReturnValueOnce({
        ...defaultTalonProps,
        hasItems: false
    });

    const tree = render(<PriceSummary />);

    expect(tree.toJSON()).toMatchSnapshot();
});
