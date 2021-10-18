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
jest.mock('@magento/venia-ui/lib/components/Image', () => 'Image');
jest.mock('@magento/peregrine/lib/talons/CartPage/ProductListing/useProduct');
jest.mock('@apollo/client', () => {
    const executeMutation = jest.fn(() => ({ error: null }));
    const useMutation = jest.fn(() => [executeMutation]);

    return {
        gql: jest.fn(),
        useMutation
    };
});

jest.mock('@magento/peregrine/lib/context/cart', () => {
    const state = { cartId: 'cart123' };
    const api = {};
    const useCartContext = jest.fn(() => [state, api]);

    return { useCartContext };
});

jest.mock('react-router-dom', () => ({
    Link: ({ children, ...rest }) => <div {...rest}>{children}</div> // eslint-disable-line react/display-name
}));
jest.mock('@magento/peregrine/lib/util/makeUrl');

import React from 'react';
import { useProduct } from '@magento/peregrine/lib/talons/CartPage/ProductListing/useProduct';

import Product from '../product';
import render from '../../../utils/test-utils';

const props = {
    item: {
        id: '123',
        product: {
            name: 'Unit Test Product',
            small_image: {
                url: 'unittest.jpg'
            },
            urlKey: 'unittest',
            urlSuffix: '.html',
            sku: '12345'
        },
        prices: {
            price: {
                currency: 'USD',
                value: 100
            }
        },
        quantity: 1,
        configurable_options: [
            {
                configurable_product_option_value_uid: '12345asd'
            },
            {
                configurable_product_option_value_uid: 'asf2134'
            }
        ]
    }
};

describe('product', () => {
    test('renders simple product correctly', () => {
        useProduct.mockReturnValueOnce({
            addToWishlistProps: {
                atwProp1: 'value1'
            },
            errorMessage: undefined,
            handleEditItem: jest.fn(),
            handleRemoveFromCart: jest.fn(),
            handleUpdateItemQuantity: jest.fn(),
            isEditable: false,
            product: {
                currency: 'USD',
                image: {},
                name: '',
                options: [],
                quantity: 1,
                unitPrice: 1,
                urlKey: 'unittest',
                urlSuffix: '.html'
            },
            isProductUpdating: false
        });
        const tree = render(<Product {...props} />);

        expect(tree.toJSON()).toMatchSnapshot();
    });

    test('renders out of stock product', () => {
        useProduct.mockReturnValueOnce({
            addToWishlistProps: {
                atwProp1: 'value1'
            },
            errorMessage: undefined,
            handleEditItem: jest.fn(),
            handleRemoveFromCart: jest.fn(),
            handleUpdateItemQuantity: jest.fn(),
            isEditable: false,
            product: {
                currency: 'USD',
                image: {},
                name: '',
                options: [],
                quantity: 2,
                stockStatus: 'OUT_OF_STOCK',
                unitPrice: 55,
                urlKey: 'popular-product',
                urlSuffix: ''
            },
            loginToastProps: null,
            isProductUpdating: false
        });
        const tree = render(<Product {...props} />);

        expect(tree.toJSON()).toMatchSnapshot();
    });

    test('renders configurable product with options', () => {
        useProduct.mockReturnValueOnce({
            addToWishlistProps: {
                atwProp1: 'value1'
            },
            errorMessage: undefined,
            handleEditItem: jest.fn(),
            handleRemoveFromCart: jest.fn(),
            handleUpdateItemQuantity: jest.fn(),
            isEditable: true,
            product: {
                currency: 'USD',
                image: {},
                name: '',
                urlKey: 'unittest',
                urlSuffix: '.html',
                options: [
                    {
                        option_label: 'Option 1',
                        value_label: 'Value 1'
                    },
                    {
                        option_label: 'Option 2',
                        value_label: 'Value 2'
                    }
                ],
                quantity: 1,
                unitPrice: 1
            },
            loginToastProps: null,
            isProductUpdating: false
        });

        const tree = render(<Product {...props} />);

        expect(tree.toJSON()).toMatchSnapshot();
    });
});
