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
import { FormattedMessage, useIntl } from 'react-intl';
import { gql } from '@apollo/client';
import { useProduct } from '@magento/peregrine/lib/talons/CartPage/ProductListing/useProduct';
import Price from '@magento/venia-ui/lib/components/Price';

import { useStyle } from '@magento/venia-ui/lib/classify';
import Image from '@magento/venia-ui/lib/components/Image';
import Kebab from '@magento/venia-ui/lib/components/LegacyMiniCart/kebab';
import ProductOptions from '@magento/venia-ui/lib/components/LegacyMiniCart/productOptions';
import Section from '@magento/venia-ui/lib/components/LegacyMiniCart/section';
import Quantity from '@magento/venia-ui/lib/components/CartPage/ProductListing/quantity';

import defaultClasses from '@magento/venia-ui/lib/components/CartPage/ProductListing/product.css';

import { CartPageFragment } from '@magento/venia-ui/lib/components/CartPage/cartPageFragments.gql';
import { AvailableShippingMethodsCartFragment } from '@magento/venia-ui/lib/components/CartPage/PriceAdjustments/ShippingMethods/shippingMethodsFragments.gql';

const IMAGE_SIZE = 100;

const Product = props => {
    const { item } = props;

    const { formatMessage } = useIntl();
    const talonProps = useProduct({
        operations: {
            removeItemMutation: REMOVE_ITEM_MUTATION,
            updateItemQuantityMutation: UPDATE_QUANTITY_MUTATION
        },
        ...props,
        item: {
            ...item,
            id: parseInt(item.id)
        }
    });

    const {
        errorMessage,
        handleEditItem,
        handleRemoveFromCart,
        handleUpdateItemQuantity,
        isEditable,
        product,
        isProductUpdating
    } = talonProps;

    const { currency, image, name, options, quantity, stockStatus, unitPrice } = product;

    const classes = useStyle(defaultClasses, props.classes);

    const itemClassName = isProductUpdating ? classes.item_disabled : classes.item;

    const editItemSection = isEditable ? (
        <Section
            text={formatMessage({
                id: 'product.editItem',
                defaultMessage: 'Edit item'
            })}
            onClick={handleEditItem}
            icon="Edit2"
            classes={{
                text: classes.sectionText
            }}
        />
    ) : null;

    const stockStatusMessage =
        stockStatus === 'OUT_OF_STOCK'
            ? formatMessage({
                  id: 'product.outOfStock',
                  defaultMessage: 'Out-of-stock'
              })
            : '';

    return (
        <li className={classes.root}>
            <span className={classes.errorText}>{errorMessage}</span>
            <div className={itemClassName}>
                <div className={classes.imageContainer}>
                    <Image
                        alt={name}
                        classes={{
                            root: classes.imageRoot,
                            image: classes.image
                        }}
                        width={IMAGE_SIZE}
                        resource={image}
                    />
                </div>
                <div className={classes.details}>
                    <div className={classes.name}>{name}</div>
                    <ProductOptions
                        options={options}
                        classes={{
                            options: classes.options,
                            optionLabel: classes.optionLabel
                        }}
                    />
                    <span className={classes.price}>
                        <Price currencyCode={currency} value={unitPrice} />
                        <FormattedMessage id={'product.price'} defaultMessage={' ea.'} />
                    </span>
                    <span className={classes.stockStatusMessage}>{stockStatusMessage}</span>
                    <div className={classes.quantity}>
                        <Quantity itemId={item.id} initialValue={quantity} onChange={handleUpdateItemQuantity} />
                    </div>
                </div>
                <Kebab
                    classes={{
                        root: classes.kebab
                    }}
                    disabled={true}
                >
                    {editItemSection}
                    <Section
                        text={formatMessage({
                            id: 'product.removeFromCart',
                            defaultMessage: 'Remove from cart'
                        })}
                        onClick={handleRemoveFromCart}
                        icon="Trash"
                        classes={{
                            text: classes.sectionText
                        }}
                    />
                </Kebab>
            </div>
        </li>
    );
};

export default Product;

export const REMOVE_ITEM_MUTATION = gql`
    mutation removeItem($cartId: String!, $itemId: Int!) {
        removeItemFromCart(input: { cart_id: $cartId, cart_item_id: $itemId }) @connection(key: "removeItemFromCart") {
            cart {
                id
                ...CartPageFragment
                ...AvailableShippingMethodsCartFragment
            }
        }
    }
    ${CartPageFragment}
    ${AvailableShippingMethodsCartFragment}
`;

export const UPDATE_QUANTITY_MUTATION = gql`
    mutation updateItemQuantity($cartId: String!, $itemId: Int!, $quantity: Float!) {
        updateCartItems(input: { cart_id: $cartId, cart_items: [{ cart_item_id: $itemId, quantity: $quantity }] })
            @connection(key: "updateCartItems") {
            cart {
                id
                ...CartPageFragment
                ...AvailableShippingMethodsCartFragment
            }
        }
    }
    ${CartPageFragment}
    ${AvailableShippingMethodsCartFragment}
`;
