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
import render from '../../../utils/test-utils';

import { useOrderConfirmationPage } from '@magento/peregrine/lib/talons/CheckoutPage/OrderConfirmationPage/useOrderConfirmationPage';
import OrderConfirmationPage from '../orderConfirmationPage';

jest.mock('@magento/peregrine/lib/talons/CheckoutPage/OrderConfirmationPage/useOrderConfirmationPage', () => {
    return {
        useOrderConfirmationPage: jest.fn()
    };
});
jest.mock('@magento/venia-ui/lib/components/Head', () => ({ StoreTitle: () => 'Title' }));
jest.mock('@magento/venia-ui/lib/components/CheckoutPage/ItemsReview', () => 'ItemsReview');

const defaultTalonProps = {
    flatData: {
        city: 'Austin',
        country: 'US',
        email: 'badvirus@covid.com',
        firstname: 'Stuck',
        lastname: 'Indoors',
        postcode: '91111',
        region: 'TX',
        shippingMethod: 'Flat Rate - Fixed',
        street: ['123 Stir Crazy Dr.'],
        totalItemQuantity: 1
    },
    isSignedIn: false
};
describe('OrderConfirmationPage', () => {
    beforeEach(() => {
        globalThis.scrollTo = jest.fn();
    });
    test('renders OrderConfirmationPage component', () => {
        useOrderConfirmationPage.mockReturnValue({
            ...defaultTalonProps
        });
        const instance = render(<OrderConfirmationPage orderNumber={'123'} data={{}} />);
        expect(instance.toJSON()).toMatchSnapshot();
    });
});
