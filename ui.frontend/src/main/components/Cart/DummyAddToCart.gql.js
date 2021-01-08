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
import { gql } from '@apollo/client';

export const ADD_CONFIGURABLE_MUTATION = gql`
    mutation addConfigurableProductToCart($cartId: String!, $quantity: Float!, $sku: String!, $parentSku: String!) {
        addConfigurableProductsToCart(
            input: {
                cart_id: $cartId
                cart_items: [{ data: { quantity: $quantity, sku: $sku }, parent_sku: $parentSku }]
            }
        ) @connection(key: "addConfigurableProductsToCart") {
            cart {
                id
            }
        }
    }
`;

export const ADD_SIMPLE_MUTATION = gql`
    mutation addSimpleProductToCart($cartId: String!, $quantity: Float!, $sku: String!) {
        addSimpleProductsToCart(
            input: { cart_id: $cartId, cart_items: [{ data: { quantity: $quantity, sku: $sku } }] }
        ) @connection(key: "addSimpleProductsToCart") {
            cart {
                id
            }
        }
    }
`;
