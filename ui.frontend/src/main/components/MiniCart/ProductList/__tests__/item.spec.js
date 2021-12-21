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
/* eslint-disable react/display-name, @typescript-eslint/no-empty-function */
import React from 'react';
import { useItem } from '@magento/peregrine/lib/talons/MiniCart/useItem';

import Item from '../item';
import render from '../../../utils/test-utils';

jest.mock('@magento/venia-ui/lib/classify');
jest.mock('@magento/peregrine/lib/talons/MiniCart/useItem', () => ({
    useItem: jest.fn().mockReturnValue({
        isDeleting: false,
        removeItem: () => {}
    })
}));
jest.mock('react-router-dom', () => ({
    Link: ({ children, ...rest }) => <div {...rest}>{children}</div>
}));
// jest.mock('@magento/peregrine/lib/util/makeUrl', () => jest.fn(src => src));

const props = {
    product: {
        name: 'P1',
        url_key: 'product',
        url_suffix: '.html',
        thumbnail: {
            url: 'www.venia.com/p1'
        },
        variants: [
            {
                attributes: [
                    {
                        uid: 'Y29uZmlndXJhYmxlLzIyLzI='
                    }
                ],
                product: {
                    thumbnail: {
                        url: 'www.venia.com/p1-variant1'
                    }
                }
            },
            {
                attributes: [
                    {
                        uid: 'Y29uZmlndXJhYmxlLzIyLzM='
                    }
                ],
                product: {
                    thumbnail: {
                        url: 'www.venia.com/p1-variant2'
                    }
                }
            }
        ]
    },
    id: 'p1',
    quantity: 10,
    configurable_options: [
        {
            option_label: 'Color',
            value_label: 'red',
            id: 22,
            value_id: 2
        }
    ],
    handleRemoveItem: () => {},
    prices: {
        price: {
            value: 420,
            currency: 'USD'
        }
    }
};

test('Should render correctly', () => {
    const tree = render(<Item {...props} />);

    expect(tree.toJSON()).toMatchSnapshot();
});

test('Should render correctly with out of stock product', () => {
    const outOfStockProps = {
        ...props,
        product: {
            ...props.product,
            stock_status: 'OUT_OF_STOCK'
        }
    };
    const tree = render(<Item {...outOfStockProps} />);

    expect(tree.toJSON()).toMatchSnapshot();
});

test('Should render correctly when configured to use variant thumbnail', () => {
    const variantThumbnailProps = {
        ...props,
        configurableThumbnailSource: 'itself'
    };
    const tree = render(<Item {...variantThumbnailProps} />);

    expect(tree.toJSON()).toMatchSnapshot();
});

test('Should disable delete icon while loading', () => {
    useItem.mockReturnValueOnce({
        isDeleting: true,
        removeItem: () => {}
    });
    const tree = render(<Item {...props} />);

    expect(tree.toJSON()).toMatchSnapshot();
});
