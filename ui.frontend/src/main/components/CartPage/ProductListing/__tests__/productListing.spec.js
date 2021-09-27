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
import { createTestInstance } from '@magento/peregrine';

import LoadingIndicator from '@magento/venia-ui/lib/components/LoadingIndicator';
import ProductListing from '../productListing';
import { useProductListing } from '@magento/peregrine/lib/talons/CartPage/ProductListing/useProductListing';

jest.mock('@magento/peregrine/lib/talons/CartPage/ProductListing/useProductListing');
jest.mock('@magento/peregrine/lib/util/shallowMerge');
jest.mock('@apollo/client', () => {
    return {
        gql: jest.fn(),
        useLazyQuery: jest.fn()
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
jest.mock('../product', () => 'Product');
jest.mock('@magento/venia-ui/lib/components/CartPage/ProductListing/EditModal', () => 'EditModal');

test('renders null with no items in cart', () => {
    useProductListing.mockReturnValueOnce({
        isLoading: false,
        isUpdating: false,
        items: []
    });

    const tree = createTestInstance(<ProductListing />);

    expect(tree.toJSON()).toMatchSnapshot();
});

test('renders list of products with items in cart', () => {
    useProductListing.mockReturnValueOnce({
        isLoading: false,
        isUpdating: false,
        items: ['1', '2', '3']
    });

    const tree = createTestInstance(<ProductListing />);

    expect(tree.toJSON()).toMatchSnapshot();
});

test('renders loading indicator if isLoading', () => {
    useProductListing.mockReturnValueOnce({
        isLoading: true
    });

    const propsWithClass = {
        classes: {
            root: 'root'
        }
    };

    const tree = createTestInstance(<ProductListing {...propsWithClass} />);

    expect(tree.root.findByType(LoadingIndicator)).toBeTruthy();
});
