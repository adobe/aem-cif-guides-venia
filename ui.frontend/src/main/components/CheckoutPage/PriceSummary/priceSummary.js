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
import { FormattedMessage, useIntl } from 'react-intl';
import { gql } from '@apollo/client';
import Price from '@magento/venia-ui/lib/components/Price';
import { usePriceSummary } from '@magento/peregrine/lib/talons/CartPage/PriceSummary/usePriceSummary';
import { useStyle } from '@magento/venia-ui/lib/classify';
import defaultClasses from '@magento/venia-ui/lib/components/CartPage/PriceSummary/priceSummary.css';
import DiscountSummary from '@magento/venia-ui/lib/components/CartPage/PriceSummary/discountSummary';
import GiftCardSummary from '@magento/venia-ui/lib/components/CartPage/PriceSummary/giftCardSummary';
import ShippingSummary from '@magento/venia-ui/lib/components/CartPage/PriceSummary/shippingSummary';
import TaxSummary from '@magento/venia-ui/lib/components/CartPage/PriceSummary/taxSummary';
import { PriceSummaryFragment } from '@magento/peregrine/lib/talons/CartPage/PriceSummary/priceSummaryFragments.gql';
const GET_PRICE_SUMMARY = gql`
    query getPriceSummary($cartId: String!) {
        cart(cart_id: $cartId) {
            id
            ...PriceSummaryFragment
        }
    }
    ${PriceSummaryFragment}
`;

/**
 * A child component of the CartPage component.
 * This component fetches and renders cart data, such as subtotal, discounts applied,
 * gift cards applied, tax, shipping, and cart total.
 *
 * @param {Object} props
 * @param {Object} props.classes CSS className overrides.
 * See [priceSummary.css]{@link https://github.com/magento/pwa-studio/blob/develop/packages/venia-ui/lib/components/CartPage/PriceSummary/priceSummary.css}
 * for a list of classes you can override.
 *
 * @returns {React.Element}
 *
 * @example <caption>Importing into your project</caption>
 * import PriceSummary from "@magento/venia-ui/lib/components/CartPage/PriceSummary";
 */
const PriceSummary = props => {
    const { isUpdating } = props;
    const classes = useStyle(defaultClasses, props.classes);
    const talonProps = usePriceSummary({
        queries: {
            getPriceSummary: GET_PRICE_SUMMARY
        }
    });

    const { hasError, hasItems, isCheckout, isLoading, flatData } = talonProps;
    const { formatMessage } = useIntl();

    if (hasError) {
        return (
            <div className={classes.root}>
                <span className={classes.errorText}>
                    <FormattedMessage
                        id={'priceSummary.errorText'}
                        defaultMessage={'Something went wrong. Please refresh and try again.'}
                    />
                </span>
            </div>
        );
    } else if (!hasItems) {
        return null;
    }

    const { subtotal, total, discounts, giftCards, taxes, shipping } = flatData;

    const isPriceUpdating = isUpdating || isLoading;
    const priceClass = isPriceUpdating ? classes.priceUpdating : classes.price;
    const totalPriceClass = isPriceUpdating ? classes.priceUpdating : classes.totalPrice;

    const totalPriceLabel = isCheckout
        ? formatMessage({
              id: 'priceSummary.total',
              defaultMessage: 'Total'
          })
        : formatMessage({
              id: 'priceSummary.estimatedTotal',
              defaultMessage: 'Estimated Total'
          });

    return (
        <div className={classes.root}>
            <div className={classes.lineItems}>
                <span className={classes.lineItemLabel}>
                    <FormattedMessage id={'priceSummary.lineItemLabel'} defaultMessage={'Subtotal'} />
                </span>
                <span className={priceClass}>
                    <Price value={subtotal.value} currencyCode={subtotal.currency} />
                </span>
                <DiscountSummary
                    classes={{
                        lineItemLabel: classes.lineItemLabel,
                        price: priceClass
                    }}
                    data={discounts}
                />
                <GiftCardSummary
                    classes={{
                        lineItemLabel: classes.lineItemLabel,
                        price: priceClass
                    }}
                    data={giftCards}
                />
                <TaxSummary
                    classes={{
                        lineItemLabel: classes.lineItemLabel,
                        price: priceClass
                    }}
                    data={taxes}
                    isCheckout={isCheckout}
                />
                <ShippingSummary
                    classes={{
                        lineItemLabel: classes.lineItemLabel,
                        price: priceClass
                    }}
                    data={shipping}
                    isCheckout={isCheckout}
                />
                <span className={classes.totalLabel}>{totalPriceLabel}</span>
                <span className={totalPriceClass}>
                    <Price value={total.value} currencyCode={total.currency} />
                </span>
            </div>
        </div>
    );
};

export default PriceSummary;

PriceSummary.propTypes = {
    classes: shape({
        root: string,
        errorText: string,
        priceUpdating: string,
        price: string,
        totalPrice: string,
        lineItems: string,
        lineItemLabel: string,
        totalLabel: string
    }),
    isUpdating: bool
};
