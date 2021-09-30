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
/* eslint-disable react/display-name */
import React from 'react';
import render from '../../../utils/test-utils';
import { useEditModal } from '@magento/peregrine/lib/talons/CheckoutPage/PaymentInformation/useEditModal';
import EditModal from '../editModal';

jest.mock('@magento/venia-ui/lib/classify');
jest.mock('@magento/venia-ui/lib/components/Dialog', () => props => (
    <mock-Dialog {...props}>{props.children}</mock-Dialog>
));
jest.mock('@magento/peregrine/lib/talons/CheckoutPage/PaymentInformation/useEditModal', () => ({
    useEditModal: jest.fn().mockReturnValue({
        selectedPaymentMethod: 'braintree',
        isLoading: false,
        updateButtonClicked: false,
        handleClose: jest.fn(),
        handlePaymentError: jest.fn(),
        handlePaymentReady: jest.fn(),
        handlePaymentSuccess: jest.fn(),
        handleUpdate: jest.fn(),
        resetUpdateButtonClicked: jest.fn()
    })
}));

jest.mock('../editablePaymentCollection', () => ({
    braintree: props => <mock-Braintree id={'BraintreeMockId'} {...props} />
}));

jest.mock('@magento/venia-ui/lib/components/Button', () => {
    return props => <mock-Button {...props} />;
});

jest.mock('@magento/venia-ui/lib/components/Icon', () => {
    return props => <mock-Icon {...props} />;
});

test('Should return correct shape', () => {
    const tree = render(<EditModal />);

    expect(tree.toJSON()).toMatchSnapshot();
});

test('Should render creditCard component if selectedPaymentMethod is braintree', () => {
    useEditModal.mockReturnValueOnce({
        selectedPaymentMethod: 'braintree',
        isLoading: true,
        updateButtonClicked: false,
        handleClose: jest.fn(),
        handleUpdate: jest.fn(),
        handlePaymentSuccess: jest.fn(),
        handlePaymentReady: jest.fn()
    });

    const tree = render(<EditModal />);

    expect(() => {
        tree.root.findByProps({ id: 'BraintreeMockId' });
    }).not.toThrow();
});

test('Should not render creditCard component if selectedPaymentMethod is not braintree', () => {
    useEditModal.mockReturnValueOnce({
        selectedPaymentMethod: 'paypal',
        isLoading: true,
        updateButtonClicked: false,
        handleClose: jest.fn(),
        handleUpdate: jest.fn(),
        handlePaymentSuccess: jest.fn(),
        handlePaymentReady: jest.fn()
    });

    const tree = render(<EditModal />);

    expect(() => {
        tree.root.findByProps({ id: 'BraintreeMockId' });
    }).toThrow('No instances found with props: {"id":"BraintreeMockId"}');
});
