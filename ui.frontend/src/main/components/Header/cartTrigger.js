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
import React, { Fragment, Suspense } from 'react';
import { shape, string } from 'prop-types';
import { ShoppingBag as ShoppingCartIcon } from 'react-feather';
import { useIntl } from 'react-intl';

import { useCartTrigger } from '@magento/peregrine/lib/talons/Header/useCartTrigger';

import useStyle from '@magento/peregrine/lib/util/shallowMerge';
import Icon from '@magento/venia-ui/lib/components/Icon';
import defaultClasses from '@magento/venia-ui/lib/components/Header/cartTrigger.css';
import { GET_ITEM_COUNT_QUERY } from '@magento/venia-ui/lib/components/Header/cartTrigger.gql';

const MiniCart = React.lazy(() => import('../MiniCart'));

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

    const cartTrigger = hideCartTrigger ? null : (
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

    return cartTrigger;
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
