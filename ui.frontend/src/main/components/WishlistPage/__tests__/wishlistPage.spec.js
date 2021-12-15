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

import render from '../../utils/test-utils';
import { useWishlistPage } from '@magento/peregrine/lib/talons/WishlistPage/useWishlistPage';

import WishlistPage from '../wishlistPage';

jest.mock('@magento/peregrine/lib/talons/WishlistPage/useWishlistPage');
jest.mock('@magento/venia-ui/lib/classify');
jest.mock('../wishlist', () => 'Wishlist');
jest.mock('@magento/venia-ui/lib/components/WishlistPage/createWishlist', () => 'CreateWishlist');
jest.mock('@adobe/aem-core-cif-react-components', () => ({
    useConfigContext: jest.fn(() => ({
        pagePaths: {
            checkoutPage: '/content/venia/us/en/checkout.html'
        }
    }))
}));

jest.mock('react-router-dom', () => ({
    useHistory: () => ({
        replace: jest.fn(),
        go: jest.fn()
    })
}));

jest.mock('@magento/peregrine/lib/context/user', () => ({
    useUserContext: () => [
        {
            isSignedIn: true
        }
    ]
}));

test('renders loading indicator', () => {
    useWishlistPage.mockReturnValue({
        errors: new Map(),
        wishlists: [],
        loading: true
    });

    const tree = render(<WishlistPage />);

    expect(tree.toJSON()).toMatchSnapshot();
});

test('renders general fetch error', () => {
    useWishlistPage.mockReturnValue({
        errors: new Map([['getCustomerWishlistQuery', { graphQLErrors: [{ message: 'Ruh roh!' }] }]]),
        wishlists: []
    });

    const tree = render(<WishlistPage />);

    expect(tree.toJSON()).toMatchSnapshot();
});

test('renders disabled feature error', () => {
    useWishlistPage.mockReturnValue({
        errors: new Map([
            [
                'getCustomerWishlistQuery',
                {
                    graphQLErrors: [{ message: 'The wishlist is not currently available.' }]
                }
            ]
        ]),
        wishlists: []
    });

    const tree = render(<WishlistPage />);

    expect(tree.toJSON()).toMatchSnapshot();
});

test('renders wishlist data', () => {
    useWishlistPage.mockReturnValue({
        errors: new Map(),
        wishlists: [
            { id: 1, name: 'Favorites' },
            { id: 2, name: 'Registry' }
        ]
    });

    const tree = render(<WishlistPage />);

    expect(tree.toJSON()).toMatchSnapshot();
});

test('renders a single wishlist without visibility toggle', () => {
    useWishlistPage.mockReturnValue({
        errors: new Map(),
        wishlists: [{ id: 1, name: 'Favorites' }]
    });

    const tree = render(<WishlistPage />);

    expect(tree.toJSON()).toMatchSnapshot();
});

test('empty single wishlist', () => {
    useWishlistPage.mockReturnValue({
        errors: new Map(),
        wishlists: []
    });

    const tree = render(<WishlistPage />);

    expect(tree.toJSON()).toMatchSnapshot();
});
