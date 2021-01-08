/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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

import { useCartPage } from '@magento/peregrine/lib/talons/CartPage/useCartPage';

import { GET_CART_DETAILS } from './cartPage.gql';

const Cart = props => {
    const { cartItems, hasItems, isCartUpdating, setIsCartUpdating, shouldShowLoadingIndicator } = useCartPage({
        queries: {
            getCartDetails: GET_CART_DETAILS
        }
    });

    console.log('cartItems', cartItems);

    return <div>Cart</div>;
};

export default Cart;
