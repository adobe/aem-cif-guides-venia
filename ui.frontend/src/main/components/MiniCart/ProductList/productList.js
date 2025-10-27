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
import { string, func, arrayOf, shape, number, oneOf } from 'prop-types';

import Item from './item';
import { useStyle } from '@magento/venia-ui/lib/classify';

import defaultClasses from '@magento/venia-ui/lib/components/MiniCart/ProductList/productList.module.css';

const ProductList = props => {
    const { items, handleRemoveItem, classes: propClasses, closeMiniCart, configurableThumbnailSource } = props;
    const classes = useStyle(defaultClasses, propClasses);
    const cartItems = useMemo(() => {
        if (items) {
            return items.map(item => (
                <Item
                    key={item.id}
                    {...item}
                    closeMiniCart={closeMiniCart}
                    handleRemoveItem={handleRemoveItem}
                    configurableThumbnailSource={configurableThumbnailSource}
                />
            ));
        }
    }, [items, handleRemoveItem, closeMiniCart, configurableThumbnailSource]);

    return <div className={classes.root}>{cartItems}</div>;
};

export default ProductList;

ProductList.propTypes = {
    classes: shape({ root: string }),
    items: arrayOf(
        shape({
            product: shape({
                name: string,
                thumbnail: shape({
                    url: string
                })
            }),
            id: string,
            quantity: number,
            configurable_options: arrayOf(
                shape({
                    label: string,
                    value: string
                })
            ),
            prices: shape({
                price: shape({
                    value: number,
                    currency: string
                })
            }),
            configured_variant: shape({
                thumbnail: shape({
                    url: string
                })
            })
        })
    ),
    configurableThumbnailSource: oneOf(['parent', 'itself']),
    handleRemoveItem: func
};
