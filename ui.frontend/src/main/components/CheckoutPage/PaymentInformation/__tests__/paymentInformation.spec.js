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
import { usePaymentInformation } from '@magento/peregrine/lib/talons/CheckoutPage/PaymentInformation/usePaymentInformation';

import PaymentMethods from '../paymentMethods';
import Summary from '../summary';
import EditModal from '../editModal';
import PaymentInformation from '../paymentInformation';

jest.mock('@magento/venia-ui/lib/classify');

jest.mock('@magento/venia-ui/lib/components/CheckoutPage/PriceAdjustments', () => props => (
    <mock-PriceAdjustments {...props} />
));

jest.mock('@magento/peregrine/lib/talons/CheckoutPage/PaymentInformation/usePaymentInformation', () => ({
    usePaymentInformation: jest.fn().mockReturnValue({
        doneEditing: false,
        handlePaymentError: jest.fn(),
        handlePaymentSuccess: jest.fn(),
        hideEditModal: jest.fn(),
        isEditModalActive: false,
        isLoading: false,
        showEditModal: jest.fn()
    })
}));

jest.mock('../summary', () => props => <mock-Summary {...props} />);
jest.mock('../editModal', () => props => <mock-EditModal {...props} />);
jest.mock('../paymentMethods', () => props => <mock-PaymentMethods {...props} />);

const defaultTalonResponse = {
    doneEditing: false,
    handlePaymentError: jest.fn(),
    handlePaymentSuccess: jest.fn(),
    hideEditModal: jest.fn(),
    isEditModalActive: false,
    isLoading: false,
    showEditModal: jest.fn()
};

const defaultProps = {
    onSave: jest.fn(),
    resetShouldSubmit: jest.fn(),
    setCheckoutStep: jest.fn(),
    shouldSubmit: false
};

test('Should render summary component only if doneEditing is true', () => {
    usePaymentInformation.mockReturnValueOnce({
        ...defaultTalonResponse,
        doneEditing: true
    });

    const tree = render(<PaymentInformation {...defaultProps} />);
    expect(tree.toJSON()).toMatchSnapshot();

    usePaymentInformation.mockReturnValueOnce({
        ...defaultTalonResponse,
        doneEditing: false
    });

    tree.update(<PaymentInformation {...defaultProps} />);

    expect(() => {
        tree.root.findByType(Summary);
    }).toThrow();
});

test('Should render PaymentMethods component only if doneEditing is false', () => {
    usePaymentInformation.mockReturnValueOnce({
        ...defaultTalonResponse,
        doneEditing: false
    });

    const tree = render(<PaymentInformation />);
    expect(tree.toJSON()).toMatchSnapshot();

    usePaymentInformation.mockReturnValueOnce({
        ...defaultTalonResponse,
        doneEditing: true
    });

    tree.update(<PaymentInformation />);

    expect(() => {
        tree.root.findByType(PaymentMethods);
    }).toThrow();
});

test('Should render EditModal component only if doneEditing is true', () => {
    usePaymentInformation.mockReturnValueOnce({
        ...defaultTalonResponse,
        doneEditing: true
    });

    const tree = render(<PaymentInformation />);

    expect(tree.toJSON()).toMatchSnapshot();

    usePaymentInformation.mockReturnValueOnce({
        ...defaultTalonResponse,
        doneEditing: false
    });

    tree.update(<PaymentInformation />);

    expect(() => {
        tree.root.findByType(EditModal);
    }).toThrow();
});
