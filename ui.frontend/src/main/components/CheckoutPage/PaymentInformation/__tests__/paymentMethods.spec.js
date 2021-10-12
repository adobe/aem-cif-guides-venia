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
import { Form } from 'informed';
import render from '../../../utils/test-utils';

import PaymentMethods from '../paymentMethods';

import { usePaymentMethods } from '@magento/peregrine/lib/talons/CheckoutPage/PaymentInformation/usePaymentMethods';

jest.mock('@magento/venia-ui/lib/classify');

jest.mock('@magento/peregrine/lib/talons/CheckoutPage/PaymentInformation/usePaymentMethods');

jest.mock('../paymentMethodCollection', () => ({
    braintree: props => <mock-Braintree id={'BraintreeMockId'} {...props} />
}));

const defaultTalonProps = {
    availablePaymentMethods: [{ code: 'braintree' }],
    currentSelectedPaymentMethod: 'braintree',
    initialSelectedMethod: 'braintree',
    isLoading: false
};

const defaultProps = {
    onPaymentError: jest.fn(),
    onPaymentSuccess: jest.fn(),
    resetShouldSubmit: jest.fn(),
    shouldSubmit: false
};

test('renders null when loading', () => {
    usePaymentMethods.mockReturnValueOnce({
        ...defaultTalonProps,
        isLoading: true
    });

    const tree = render(
        <Form>
            <PaymentMethods {...defaultProps} />
        </Form>
    );

    expect(tree.toJSON()).toMatchSnapshot();
});

test('should render no method if not selected', () => {
    usePaymentMethods.mockReturnValueOnce({
        ...defaultTalonProps,
        currentSelectedPaymentMethod: null
    });

    const tree = render(
        <Form>
            <PaymentMethods {...defaultProps} />
        </Form>
    );

    expect(() => {
        tree.root.findByProps({ id: 'BraintreeMockId' });
    }).toThrow('No instances found with props: {"id":"BraintreeMockId"}');
});

test('should render CreditCard component if "braintree" is selected', () => {
    usePaymentMethods.mockReturnValueOnce({
        ...defaultTalonProps,
        currentSelectedPaymentMethod: 'braintree'
    });

    const tree = render(
        <Form>
            <PaymentMethods {...defaultProps} />
        </Form>
    );

    expect(() => {
        tree.root.findByProps({ id: 'BraintreeMockId' });
    }).not.toThrow();
});

test('should render error message if availablePaymentMethods is empty', () => {
    usePaymentMethods.mockReturnValueOnce({
        ...defaultTalonProps,
        availablePaymentMethods: []
    });

    const tree = render(
        <Form>
            <PaymentMethods {...defaultProps} />
        </Form>
    );

    expect(tree).toMatchSnapshot();
});
