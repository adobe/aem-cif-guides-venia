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

import Item from '../item';
import PlaceholderImage from '@magento/venia-ui/lib/components/Image/placeholderImage';
import render from '../../../utils/test-utils';

const defaultProps = {
    product_name: 'Product 1',
    product_sale_price: {
        currency: 'USD',
        value: 100
    },
    product_url_key: 'carina-cardigan',
    quantity_ordered: 3,
    selected_options: [
        {
            label: 'Color',
            value: 'Black'
        }
    ],
    thumbnail: { url: 'https://www.venia.com/product1-thumbnail.jpg' }
};

test('should render properly', () => {
    const tree = render(<Item {...defaultProps} />);

    expect(tree.toJSON()).toMatchSnapshot();
});

test('should render placeholder without thumbnail', () => {
    const props = {
        ...defaultProps,
        thumbnail: undefined
    };
    const tree = render(<Item {...props} />);
    const { root } = tree;
    const imagePlaceholderNode = root.findByType(PlaceholderImage);

    expect(imagePlaceholderNode).toBeTruthy();
});
