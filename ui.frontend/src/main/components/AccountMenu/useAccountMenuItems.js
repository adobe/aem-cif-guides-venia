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
import { useCallback } from 'react';
import { useConfigContext } from '@adobe/aem-core-cif-react-components';

/**
 * @param {Object}      props
 * @param {Function}    props.onSignOut - A function to call when sign out occurs.
 *
 * @returns {Object}    result
 * @returns {Function}  result.handleSignOut - The function to handle sign out actions.
 */
export const useAccountMenuItems = props => {
    const { onSignOut, showWishList } = props;

    const handleSignOut = useCallback(() => {
        onSignOut();
    }, [onSignOut]);

    const { pagePaths } = useConfigContext();
    const MENU_ITEMS = [];

    MENU_ITEMS.push({
        name: 'Order History',
        id: 'accountMenu.orderHistoryLink',
        url: pagePaths.orderHistory
    });

    if (showWishList) {
        MENU_ITEMS.push({
            name: 'Favorites Lists',
            id: 'accountMenu.favoritesListsLink',
            url: pagePaths.wishlist
        });
    }

    MENU_ITEMS.push({
        name: 'Favorites Lists',
            id: 'accountMenu.favoritesListsLink',
            url: pagePaths.wishlist
        },
        {
            name: 'Address Book',
        id: 'accountMenu.addressBookLink',
        url: pagePaths.addressBook
    });
    MENU_ITEMS.push({
        name: 'Account Information',
        id: 'accountMenu.accountInfoLink',
        url: pagePaths.accountDetails
    });

    // Hide links until features are completed
    // {
    //     name: 'Saved Payments',
    //     id: 'accountMenu.savedPaymentsLink',
    //     url: '/saved-payments'
    // }

    return {
        handleSignOut,
        menuItems: MENU_ITEMS
    };
};
