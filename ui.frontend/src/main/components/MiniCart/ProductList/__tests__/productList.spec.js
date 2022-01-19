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

import ProductList from '../productList';
import render from '../../../utils/test-utils';

jest.mock('@magento/venia-ui/lib/classify');
jest.mock('../item', () => 'Item');

const props = {
    closeMiniCart: jest.fn().mockName('closeMiniCart'),
    handleRemoveItem: jest.fn().mockName('handleRemoveItem'),
    items: [
        {
            id: '1',
            product: {
                name: 'Simple Product'
            }
        }
    ]
};

test('Should render properly', () => {
    const tree = render(<ProductList {...props} />);

    expect(tree.toJSON()).toMatchSnapshot();
});
