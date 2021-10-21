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
import { useAccountMenuItems } from '@magento/peregrine/lib/talons/AccountMenu/useAccountMenuItems';

import AccountMenuItems from '../accountMenuItems';

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

test('it renders correctly', () => {
    // Arrange.
    const myTalonProps = {
        ...talonProps,
        menuItems: [
            {
                name: 'Order History',
                id: 'accountMenu.orderHistoryLink',
                url: '/order-history'
            },
            {
                name: 'Store Credit & Gift Cards',
                id: 'accountMenu.storeCreditLink',
                url: ''
            },
            {
                name: 'Favorites Lists',
                id: 'accountMenu.favoritesListsLink',
                url: '/wishlist'
            },
            {
                name: 'Address Book',
                id: 'accountMenu.addressBookLink',
                url: ''
            },
            {
                name: 'Saved Payments',
                id: 'accountMenu.savedPaymentsLink',
                url: ''
            },
            {
                name: 'Communications',
                id: 'accountMenu.communicationsLink',
                url: '/communications'
            },
            {
                name: 'Account Information',
                id: 'accountMenu.accountInfoLink',
                url: ''
            }
        ]
    };
    useAccountMenuItems.mockReturnValueOnce(myTalonProps);

    // Act.
    const instance = createTestInstance(<AccountMenuItems {...props} />);

    // Assert.
    expect(instance.toJSON()).toMatchSnapshot();
});
