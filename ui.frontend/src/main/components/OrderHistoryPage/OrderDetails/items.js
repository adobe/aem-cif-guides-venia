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
import { shape, arrayOf, string, number } from 'prop-types';

import { useStyle } from '@magento/venia-ui/lib/classify';

import Item from './item';

import defaultClasses from '@magento/venia-ui/lib/components/OrderHistoryPage/OrderDetails/items.module.css';
import { FormattedMessage } from 'react-intl';

const Items = props => {
    const { items, imagesData } = props.data;
    const classes = useStyle(defaultClasses, props.classes);

    const itemsComponent = items.map(item => <Item key={item.id} {...item} {...imagesData[item.product_sku]} />);

    return (
        <div className={classes.root}>
            <h3 className={classes.heading}>
                <FormattedMessage id="orderItems.itemsHeading" defaultMessage="Items" />
            </h3>
            <div className={classes.itemsContainer}>{itemsComponent}</div>
        </div>
    );
};

export default Items;

Items.propTypes = {
    classes: shape({
        root: string
    }),
    data: shape({
        items: arrayOf(
            shape({
                id: string,
                product_name: string,
                product_sale_price: shape({
                    currency: string,
                    value: number
                }),
                product_sku: string,
                product_url_key: string,
                selected_options: arrayOf(
                    shape({
                        label: string,
                        value: string
                    })
                ),
                quantity_ordered: number
            })
        ),
        imagesData: arrayOf(
            shape({
                id: number,
                sku: string,
                thumbnail: shape({
                    url: string
                }),
                url_key: string,
                url_suffix: string
            })
        )
    })
};
