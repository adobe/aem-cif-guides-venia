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
import { ConfigContextProvider } from '@adobe/aem-core-cif-react-components';

import AccountMenuItems from '../accountMenuItems';
const config = {
    pagePaths: {
        addressBook: '/content/venia/us/en/my-account/address-book.html',
        accountDetails: '/content/venia/us/en/my-account/account-details.html',
        cartDetails: '/content/venia/us/en/cart-details.html',
        checkoutPage: '/content/venia/us/en/checkout.html',
        orderHistory: '/content/venia/us/en/my-account/order-history.html'
    }
};

jest.mock('react-router-dom', () => ({
    Link: children => `<Link>${children.children}</Link>`
}));
jest.mock('@magento/peregrine/lib/talons/AccountMenu/useAccountMenuItems', () => {
    return {
        useAccountMenuItems: jest.fn()
    };
});

const props = {
    onSignOut: jest.fn().mockName('onSignOut')
};
const talonProps = {
    handleSignOut: jest.fn().mockName('handleSignOut'),
    menuItems: []
};

test.each([
    ['without wishlist', false],
    ['with wishlist', true]
])('it renders correctly (%s)', (name, showWishList) => {
    // Act.
    const instance = render(
        <ConfigContextProvider config={config}>
            <AccountMenuItems {...props} showWishList={showWishList} />
        </ConfigContextProvider>
    );

    // Assert.
    expect(instance.toJSON()).toMatchSnapshot();
});
