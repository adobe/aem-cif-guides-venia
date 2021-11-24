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
import Wishlist from '../wishlist';
import { useWishlist } from '@magento/peregrine/lib/talons/WishlistPage/useWishlist';

jest.mock('@magento/peregrine/lib/talons/WishlistPage/useWishlist');
jest.mock('@magento/venia-ui/lib/classify');
jest.mock('@magento/venia-ui/lib/components/WishlistPage/wishlistItems', () => 'WishlistItems');
jest.mock('@magento/venia-ui/lib/components/WishlistPage/actionMenu.ee', () => 'ActionMenu');

const baseProps = {
    data: {
        id: 5,
        items_count: 0,
        items_v2: { items: [] },
        name: 'Favorites List',
        sharing_code: null,
        visibility: 'PUBLIC'
    },
    shouldRenderVisibilityToggle: true
};

const baseTalonProps = {
    handleContentToggle: jest.fn().mockName('handleContentToggle'),
    isOpen: true
};

test('render open with no items', () => {
    useWishlist.mockReturnValue(baseTalonProps);

    const tree = render(<Wishlist {...baseProps} />);

    expect(tree.toJSON()).toMatchSnapshot();
});

test('render closed with items', () => {
    useWishlist.mockReturnValue({ ...baseTalonProps, isOpen: false });

    const myProps = {
        data: {
            ...baseProps.data,
            items_count: 20,
            items: { items: ['item1', 'item2'] },
            sharing_code: 'abc123'
        },
        shouldRenderVisibilityToggle: true
    };
    const tree = render(<Wishlist {...myProps} />);

    expect(tree.toJSON()).toMatchSnapshot();
});

test('renders a name even if none in data', () => {
    useWishlist.mockReturnValue({ ...baseTalonProps, isOpen: false });

    const myProps = {
        data: {
            ...baseProps.data,
            items_count: 20,
            items: { items: ['item1', 'item2'] },
            sharing_code: 'abc123'
        },
        shouldRenderVisibilityToggle: true
    };

    delete myProps.data.name;

    const tree = render(<Wishlist {...myProps} />);

    expect(tree.toJSON()).toMatchSnapshot();
});

test('hides visibility toggle', () => {
    useWishlist.mockReturnValue({ ...baseTalonProps, isOpen: false });

    const myProps = {
        data: {
            ...baseProps.data,
            items_count: 20,
            items: { items: ['item1', 'item2'] },
            sharing_code: 'abc123'
        },
        shouldRenderVisibilityToggle: false
    };

    const tree = render(<Wishlist {...myProps} />);

    expect(tree.toJSON()).toMatchSnapshot();
});

test('render no button when id is not set', () => {
    const tree = render(<Wishlist />);
    expect(tree.toJSON()).toMatchSnapshot();
});

test('render loading state', () => {
    useWishlist.mockReturnValue({ ...baseTalonProps, isLoading: true });

    const myProps = {
        data: {
            ...baseProps.data,
            items_count: 0,
            sharing_code: 'abc123'
        },
        shouldRenderVisibilityToggle: false
    };

    const tree = render(<Wishlist {...myProps} />);

    expect(tree.toJSON()).toMatchSnapshot();
});
