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
import { useSummary } from '@magento/peregrine/lib/talons/CheckoutPage/PaymentInformation/useSummary';

import Summary from '../summary';

jest.mock('@magento/venia-ui/lib/classify');

jest.mock('@magento/peregrine/lib/talons/CheckoutPage/PaymentInformation/useSummary', () => {
    return {
        useSummary: jest.fn().mockReturnValue({
            billingAddress: {},
            isBillingAddressSame: false,
            isLoading: false,
            paymentNonce: {},
            selectedPaymentMethod: 'braintree'
        })
    };
});

jest.mock('../summaryPaymentCollection', () => ({
    braintree: props => <mock-Braintree id={'BraintreeMockId'} {...props} />
}));

const billingAddress = {
    firstName: 'Goosey',
    lastName: 'Goose',
    country: 'Goose Land',
    street1: '12345 Goosey Blvd',
    street2: 'Apt 123',
    city: 'Austin',
    state: 'Texas',
    postalCode: '12345',
    phoneNumber: '1234657890'
};

const mockOnEdit = jest.fn();

test('should render a loading indicator', () => {
    useSummary.mockReturnValueOnce({
        isLoading: true
    });

    const tree = render(<Summary onEdit={mockOnEdit} />);

    expect(tree.toJSON()).toMatchSnapshot();
});

test('should render a non-braintree summary', () => {
    useSummary.mockReturnValueOnce({
        selectedPaymentMethod: { code: 'free', title: 'Free!' }
    });

    const tree = render(<Summary onEdit={jest.fn()} />);

    expect(tree.toJSON()).toMatchSnapshot();
});

test('should render a braintree summary', () => {
    useSummary.mockReturnValueOnce({
        isBillingAddressSame: false,
        billingAddress,
        paymentNonce: {
            details: { cardType: 'visa', lastFour: '1234' }
        },
        selectedPaymentMethod: {
            code: 'braintree'
        }
    });

    const tree = render(<Summary onEdit={jest.fn()} />);

    expect(tree.toJSON()).toMatchSnapshot();
});
