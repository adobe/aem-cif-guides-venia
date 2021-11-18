import { gql } from '@apollo/client';
import { CartPageFragment } from '@magento/venia-ui/lib/components/CartPage/cartPageFragments.gql';

const ProductListFragment = gql`
    fragment ProductListFragment on Cart {
        id
        items {
            id
            product {
                id
                name
                sku
                url_key
                url_suffix
                thumbnail {
                    url
                }
                stock_status
                ... on ConfigurableProduct {
                    variants {
                        attributes {
                            uid
                        }
                        product {
                            id
                            thumbnail {
                                url
                            }
                        }
                    }
                }
            }
            prices {
                price {
                    currency
                    value
                }
            }
            quantity
            ... on ConfigurableCartItem {
                configurable_options {
                    id
                    option_label
                    value_id
                    value_label
                }
            }
        }
    }
`;

const MiniCartFragment = gql`
    fragment MiniCartFragment on Cart {
        id
        total_quantity
        prices {
            subtotal_excluding_tax {
                currency
                value
            }
        }
        ...ProductListFragment
    }
    ${ProductListFragment}
`;

/**
 * @deprecated - Moved to @magento/peregrine/lib/talons/MiniCart/miniCartFragments.gql
 */
export { MiniCartFragment };

export const MINI_CART_QUERY = gql`
    query MiniCartQuery($cartId: String!) {
        cart(cart_id: $cartId) {
            id
            ...MiniCartFragment
        }
    }
    ${MiniCartFragment}
`;

export const REMOVE_ITEM_MUTATION = gql`
    mutation RemoveItemForMiniCart($cartId: String!, $itemId: Int!) {
        removeItemFromCart(input: { cart_id: $cartId, cart_item_id: $itemId }) @connection(key: "removeItemFromCart") {
            cart {
                id
                ...MiniCartFragment
                ...CartPageFragment
            }
        }
    }
    ${MiniCartFragment}
    ${CartPageFragment}
`;

export default {
    miniCartQuery: MINI_CART_QUERY,
    removeItemMutation: REMOVE_ITEM_MUTATION
};
