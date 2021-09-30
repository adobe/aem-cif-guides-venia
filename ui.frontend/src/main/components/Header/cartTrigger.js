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
import React, { Fragment, Suspense, useCallback } from 'react';
import { shape, string } from 'prop-types';
import { ShoppingBag as ShoppingCartIcon } from 'react-feather';
import { useIntl } from 'react-intl';
import { useMutation } from '@apollo/client';
import { useCartTrigger } from '@magento/peregrine/lib/talons/Header/useCartTrigger';
import { useCartContext } from '@magento/peregrine/lib/context/cart';
import useStyle from '@magento/peregrine/lib/util/shallowMerge';
import Icon from '@magento/venia-ui/lib/components/Icon';
import defaultClasses from '@magento/venia-ui/lib/components/Header/cartTrigger.css';
import { GET_ITEM_COUNT_QUERY } from '@magento/venia-ui/lib/components/Header/cartTrigger.gql';

import MUTATION_ADD_TO_CART from '../../queries/mutation_add_to_cart.graphql';
import MUTATION_ADD_BUNDLE_TO_CART from '../../queries/mutation_add_bundle_to_cart.graphql';
import MUTATION_ADD_VIRTUAL_TO_CART from '../../queries/mutation_add_virtual_to_cart.graphql';
import MUTATION_ADD_SIMPLE_AND_VIRTUAL_TO_CART from '../../queries/mutation_add_simple_and_virtual_to_cart.graphql';

import { useEventListener } from '../../utils/hooks';

const MiniCart = React.lazy(() => import('../MiniCart'));

const productMapper = item => ({
    data: {
        sku: item.sku,
        quantity: parseFloat(item.quantity)
    }
});

const bundledProductMapper = item => ({
    ...productMapper(item),
    bundle_options: item.options
});

const CartTrigger = props => {
    const {
        handleLinkClick,
        handleTriggerClick,
        itemCount,
        miniCartRef,
        miniCartIsOpen,
        hideCartTrigger,
        setMiniCartIsOpen,
        miniCartTriggerRef
    } = useCartTrigger({
        queries: {
            getItemCountQuery: GET_ITEM_COUNT_QUERY
        }
    });

    const [{ cartId }] = useCartContext();

    const [addToCartMutation] = useMutation(MUTATION_ADD_TO_CART);
    const [addBundleItemMutation] = useMutation(MUTATION_ADD_BUNDLE_TO_CART);
    const [addVirtualItemMutation] = useMutation(MUTATION_ADD_VIRTUAL_TO_CART);
    const [addSimpleAndVirtualItemMutation] = useMutation(MUTATION_ADD_SIMPLE_AND_VIRTUAL_TO_CART);

    const handleAddToCart = useCallback(async (event) => {
        const items = typeof event.detail === 'string' ? JSON.parse(event.detail) : event.detail;
        const physicalCartItems = items.filter(item => !item.virtual).map(productMapper);
        const virtualCartItems = items.filter(item => item.virtual).map(productMapper);
        const bundleCartItems = items.filter(item => item.bundle).map(bundledProductMapper);

        if (bundleCartItems.length > 0) {
            await addBundleItemMutation({
                variables: {
                    cartId,
                    cartItems: bundleCartItems
                }
            });
        } else if (virtualCartItems.length > 0 && physicalCartItems.length > 0) {
            await addSimpleAndVirtualItemMutation({
                variables: {
                    cartId,
                    virtualCartItems: virtualCartItems,
                    simpleCartItems: physicalCartItems
                }
            });
        } else if (virtualCartItems.length > 0) {
            await addVirtualItemMutation({
                variables: {
                    cartId,
                    cartItems: virtualCartItems
                }
            });
        } else if (physicalCartItems.length > 0) {
            await addToCartMutation({
                variables: {
                    cartId,
                    cartItems: physicalCartItems
                }
            });
        }
    }, [cartId]);

    useEventListener(document, 'aem.cif.add-to-cart', handleAddToCart);

    const classes = useStyle(defaultClasses, props.classes);
    const { formatMessage } = useIntl();
    const buttonAriaLabel = formatMessage(
        {
            id: 'cartTrigger.ariaLabel',
            defaultMessage: 'Toggle mini cart. You have {count} items in your cart.'
        },
        { count: itemCount }
    );
    const itemCountDisplay = itemCount > 99 ? '99+' : itemCount;
    const triggerClassName = miniCartIsOpen ? classes.triggerContainer_open : classes.triggerContainer;

    const maybeItemCounter = itemCount ? <span className={classes.counter}>{itemCountDisplay}</span> : null;

    return hideCartTrigger ? null : (
        // Because this button behaves differently on desktop and mobile
        // we render two buttons that differ only in their click handler
        // and control which one displays via CSS.
        <Fragment>
            <div className={triggerClassName} ref={miniCartTriggerRef}>
                <button aria-label={buttonAriaLabel} className={classes.trigger} onClick={handleTriggerClick}>
                    <Icon src={ShoppingCartIcon} />
                    {maybeItemCounter}
                </button>
            </div>
            <button aria-label={buttonAriaLabel} className={classes.link} onClick={handleLinkClick}>
                <Icon src={ShoppingCartIcon} />
                {maybeItemCounter}
            </button>
            <Suspense fallback={null}>
                <MiniCart isOpen={miniCartIsOpen} setIsOpen={setMiniCartIsOpen} ref={miniCartRef} />
            </Suspense>
        </Fragment>
    );
};

export default CartTrigger;

CartTrigger.propTypes = {
    classes: shape({
        counter: string,
        link: string,
        openIndicator: string,
        root: string,
        trigger: string,
        triggerContainer: string
    })
};
