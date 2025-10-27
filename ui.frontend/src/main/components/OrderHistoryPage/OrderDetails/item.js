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
import React, { useMemo } from 'react';
import { shape, string, number, arrayOf } from 'prop-types';
import { FormattedMessage } from 'react-intl';

import { useStyle } from '@magento/venia-ui/lib/classify';
import Button from '@magento/venia-ui/lib/components/Button';
import ProductOptions from '@magento/venia-ui/lib/components/LegacyMiniCart/productOptions';
import Image from '@magento/venia-ui/lib/components/Image';
import Price from '@magento/venia-ui/lib/components/Price';
import defaultClasses from '@magento/venia-ui/lib/components/OrderHistoryPage/OrderDetails/item.module.css';
import PlaceholderImage from '@magento/venia-ui/lib/components/Image/placeholderImage';

const Item = props => {
    const { product_name, product_sale_price, quantity_ordered, selected_options, thumbnail } = props;
    const { currency, value: unitPrice } = product_sale_price;

    const mappedOptions = useMemo(
        () =>
            selected_options.map(option => ({
                option_label: option.label,
                value_label: option.value
            })),
        [selected_options]
    );
    const classes = useStyle(defaultClasses, props.classes);

    const thumbnailProps = {
        alt: product_name,
        classes: { root: classes.thumbnail },
        width: 50
    };
    const thumbnailElement = thumbnail ? (
        <Image {...thumbnailProps} resource={thumbnail.url} />
    ) : (
        <PlaceholderImage {...thumbnailProps} />
    );

    return (
        <div className={classes.root}>
            <div className={classes.thumbnailContainer}>{thumbnailElement}</div>
            <div className={classes.nameContainer}>{product_name}</div>
            <ProductOptions
                options={mappedOptions}
                classes={{
                    options: classes.options
                }}
            />
            <span className={classes.quantity}>
                <FormattedMessage
                    id="orderDetails.quantity"
                    defaultMessage="Qty"
                    values={{
                        quantity: quantity_ordered
                    }}
                />
            </span>
            <div className={classes.price}>
                <Price currencyCode={currency} value={unitPrice} />
            </div>
            <Button
                onClick={() => {
                    // TODO will be implemented in PWA-979
                    console.log('Buying Again');
                }}
                className={classes.buyAgainButton}
            >
                <FormattedMessage id="orderDetails.buyAgain" defaultMessage="Buy Again" />
            </Button>
        </div>
    );
};

export default Item;

Item.propTypes = {
    classes: shape({
        root: string,
        thumbnailContainer: string,
        thumbnail: string,
        name: string,
        options: string,
        quantity: string,
        price: string,
        buyAgainButton: string
    }),
    product_name: string.isRequired,
    product_sale_price: shape({
        currency: string,
        value: number
    }).isRequired,
    product_url_key: string.isRequired,
    quantity_ordered: number.isRequired,
    selected_options: arrayOf(
        shape({
            label: string,
            value: string
        })
    ).isRequired,
    thumbnail: shape({
        url: string
    })
};
