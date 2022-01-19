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

import Items from '../items';
import render from '../../../utils/test-utils';

// eslint-disable-next-line react/display-name
jest.mock('../item', () => props => <div componentName="Item Component" {...props} />);

const defaultProps = {
    data: {
        imagesData: [
            {
                id: 1094,
                sku: 'VA03',
                thumbnail: {
                    url: 'https://master-7rqtwti-mfwmkrjfqvbjk.us-4.magentosite.cloud/media/catalog/product/cache/d3ba9f7bcd3b0724e976dc5144b29c7d/v/s/vsw01-rn_main_2.jpg'
                },
                url_key: 'valeria-two-layer-tank',
                url_suffix: '.html'
            },
            {
                id: 1103,
                sku: 'VP08',
                thumbnail: {
                    url: 'https://master-7rqtwti-mfwmkrjfqvbjk.us-4.magentosite.cloud/media/catalog/product/cache/d3ba9f7bcd3b0724e976dc5144b29c7d/v/s/vsw01-rn_main_2.jpg'
                },
                url_key: 'chloe-silk-shell',
                url_suffix: '.html'
            },
            {
                id: 1108,
                sku: 'VSW09',
                thumbnail: {
                    url: 'https://master-7rqtwti-mfwmkrjfqvbjk.us-4.magentosite.cloud/media/catalog/product/cache/d3ba9f7bcd3b0724e976dc5144b29c7d/v/s/vsw01-rn_main_2.jpg'
                },
                url_key: 'helena-cardigan',
                url_suffix: '.html'
            }
        ],
        items: [
            {
                id: '3',
                product_name: 'Product 3',
                product_sale_price: '$100.00',
                product_sku: 'VA03',
                product_url_key: 'valeria-two-layer-tank',
                selected_options: [
                    {
                        label: 'Color',
                        value: 'Blue'
                    }
                ],
                quantity_ordered: 1
            },
            {
                id: '4',
                product_name: 'Product 4',
                product_sale_price: '$100.00',
                product_sku: 'VP08',
                product_url_key: 'chloe-silk-shell',
                selected_options: [
                    {
                        label: 'Color',
                        value: 'Black'
                    }
                ],
                quantity_ordered: 1
            },
            {
                id: '5',
                product_name: 'Product 5',
                product_sale_price: '$100.00',
                product_sku: 'VSW09',
                product_url_key: 'helena-cardigan',
                selected_options: [
                    {
                        label: 'Color',
                        value: 'Orange'
                    }
                ],
                quantity_ordered: 1
            }
        ]
    }
};

test('should render properly', () => {
    const tree = render(<Items {...defaultProps} />);

    expect(tree.toJSON()).toMatchSnapshot();
});
