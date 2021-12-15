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
import { gql } from '@apollo/client';

import { WishlistPageFragment } from '@magento/peregrine/lib/talons/WishlistPage/wishlistFragment.gql';

export const GET_CUSTOMER_WISHLIST = gql`
    query GetCustomerWishlist {
        customer {
            id
            wishlists {
                id
                ...WishlistPageFragment
            }
        }
    }
    ${WishlistPageFragment}
`;

export const GET_CUSTOMER_WISHLIST_ITEMS = gql`
    query getCustomerWishlist($id: ID!, $currentPage: Int) {
        customer {
            id
            wishlist_v2(id: $id) {
                id
                items_v2(currentPage: $currentPage) {
                    items {
                        id
                        product {
                            id
                            image {
                                label
                                url
                            }
                            name
                            price_range {
                                maximum_price {
                                    final_price {
                                        currency
                                        value
                                    }
                                }
                            }
                            sku
                            stock_status
                            ... on ConfigurableProduct {
                                configurable_options {
                                    id
                                    attribute_code
                                    attribute_id
                                    attribute_id_v2
                                    label
                                    values {
                                        uid
                                        default_label
                                        label
                                        store_label
                                        use_default_value
                                        value_index
                                        swatch_data {
                                            ... on ImageSwatchData {
                                                thumbnail
                                            }
                                            value
                                        }
                                    }
                                }
                            }
                        }
                        ... on ConfigurableWishlistItem {
                            configurable_options {
                                id
                                option_label
                                value_id
                                value_label
                            }
                        }
                    }
                }
            }
        }
    }
`;

export const UPDATE_WISHLIST = gql`
    mutation UpdateWishlist($name: String!, $visibility: WishlistVisibilityEnum!, $wishlistId: ID!) {
        updateWishlist(name: $name, visibility: $visibility, wishlistId: $wishlistId) {
            name
            uid
            visibility
        }
    }
`;

export default {
    getCustomerWishlistQuery: GET_CUSTOMER_WISHLIST,
    getCustomerWhislistItems: GET_CUSTOMER_WISHLIST_ITEMS,
    updateWishlistMutation: UPDATE_WISHLIST
};
