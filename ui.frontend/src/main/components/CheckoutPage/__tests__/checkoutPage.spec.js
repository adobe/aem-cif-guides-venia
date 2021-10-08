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
import { useToasts } from '@magento/peregrine';

import render from '../../utils/test-utils';

import { CHECKOUT_STEP, useCheckoutPage } from '@magento/peregrine/lib/talons/CheckoutPage/useCheckoutPage';

import OrderConfirmationPage from '../OrderConfirmationPage';
import FormError from '@magento/venia-ui/lib/components/FormError';

jest.mock('@magento/peregrine', () => {
    //const actual = jest.requireActual('@magento/peregrine');
    const useToasts = jest.fn().mockReturnValue([{}, { addToast: jest.fn() }]);
    const useWindowSize = jest.fn().mockReturnValue({
        innerWidth: 961
    });

    return {
        //createTestInstance: actual.createTestInstance,
        useToasts,
        useWindowSize,
        default: null
    };
});

jest.mock('@magento/peregrine/lib/talons/CheckoutPage/useCheckoutPage', () => {
    const originalModule = jest.requireActual('@magento/peregrine/lib/talons/CheckoutPage/useCheckoutPage');

    return {
        CHECKOUT_STEP: originalModule.CHECKOUT_STEP,
        useCheckoutPage: jest.fn()
    };
});

import CheckoutPage from '../checkoutPage';

jest.mock('@magento/venia-ui/lib/classify');

jest.mock('@magento/venia-ui/lib/components/Head', () => ({ StoreTitle: () => 'Title' }));
jest.mock('@magento/venia-ui/lib/components/FormError', () => 'FormError');
jest.mock('@magento/venia-ui/lib/components/StockStatusMessage', () => 'StockStatusMessage');
jest.mock('@magento/venia-ui/lib/components/CheckoutPage/ItemsReview', () => 'ItemsReview');
jest.mock('@magento/venia-ui/lib/components/CheckoutPage/GuestSignIn', () => 'GuestSignIn');
jest.mock('../OrderSummary', () => 'OrderSummary');
jest.mock('../OrderConfirmationPage', () => 'OrderConfirmationPage');
jest.mock('@magento/venia-ui/lib/components/CheckoutPage/ShippingInformation', () => 'ShippingInformation');
jest.mock('@magento/venia-ui/lib/components/CheckoutPage/ShippingMethod', () => 'ShippingMethod');
jest.mock('../PaymentInformation', () => 'PaymentInformation');
jest.mock('../PaymentInformation/paymentMethodCollection', () => ({
    braintree: {}
}));
jest.mock('@magento/venia-ui/lib/components/CheckoutPage/PriceAdjustments', () => 'PriceAdjustments');
jest.mock('@magento/venia-ui/lib/components/CheckoutPage/AddressBook', () => 'AddressBook');

const defaultTalonProps = {
    activeContent: 'checkout',
    availablePaymentMethods: [{ code: 'braintree' }],
    cartItems: [],
    checkoutStep: 1,
    customer: null,
    error: false,
    handleSignIn: jest.fn().mockName('handleSignIn'),
    handlePlaceOrder: jest.fn().mockName('handlePlaceOrder'),
    hasError: false,
    isCartEmpty: false,
    isGuestCheckout: true,
    isLoading: false,
    isUpdating: false,
    orderDetailsData: null,
    orderDetailsLoading: false,
    orderNumber: null,
    placeOrderLoading: false,
    setIsUpdating: jest.fn().mockName('setIsUpdating'),
    setShippingInformationDone: jest.fn().mockName('setShippingInformationDone'),
    setShippingMethodDone: jest.fn().mockName('setShippingMethodDone'),
    setPaymentInformationDone: jest.fn().mockName('setPaymentInformationDone'),
    toggleAddressBookContent: jest.fn().mockName('toggleAddressBookContent'),
    toggleSignInContent: jest.fn().mockName('toggleSignInContent')
};

describe('CheckoutPage', () => {
    test('throws a toast if there is an error', () => {
        useCheckoutPage.mockReturnValueOnce({
            ...defaultTalonProps,
            hasError: true
        });
        const [, { addToast }] = useToasts();

        render(<CheckoutPage />);

        expect(addToast).toHaveBeenCalled();
    });

    test('renders order confirmation page', () => {
        useCheckoutPage.mockReturnValueOnce({
            ...defaultTalonProps,
            placeOrderLoading: false,
            hasError: false,
            orderDetailsData: {},
            orderNumber: 1
        });

        const instance = render(<CheckoutPage />);
        const component = instance.root.findByType(OrderConfirmationPage);
        expect(component).toBeTruthy();
    });

    test('disables place order button while loading', () => {
        useCheckoutPage.mockReturnValueOnce({
            ...defaultTalonProps,
            checkoutStep: CHECKOUT_STEP.REVIEW,
            isUpdating: true,
            placeOrderLoading: true,
            orderDetailsLoading: true,
            orderNumber: null
        });

        const instance = render(<CheckoutPage />);
        const button = instance.root.findByProps({
            className: 'place_order_button'
        });

        expect(button).toBeTruthy();
        expect(button.props.disabled).toBe(true);
    });

    test('renders loading indicator', () => {
        useCheckoutPage.mockReturnValueOnce({
            isLoading: true
        });

        const tree = render(<CheckoutPage />);
        expect(tree.toJSON()).toMatchSnapshot();
    });

    test('renders checkout content for guest', () => {
        useCheckoutPage.mockReturnValueOnce(defaultTalonProps);

        const tree = render(<CheckoutPage />);
        expect(tree.toJSON()).toMatchSnapshot();
    });

    test('renders checkout content for customer - no default address', () => {
        useCheckoutPage.mockReturnValueOnce({
            ...defaultTalonProps,
            customer: { default_shipping: null, firstname: 'Eloise' },
            isGuestCheckout: false
        });

        const tree = render(<CheckoutPage />);
        expect(tree.toJSON()).toMatchSnapshot();
    });

    test('renders checkout content for customer - default address', () => {
        useCheckoutPage.mockReturnValueOnce({
            ...defaultTalonProps,
            customer: { default_shipping: '1' },
            isGuestCheckout: false
        });

        const tree = render(<CheckoutPage />);
        expect(tree.toJSON()).toMatchSnapshot();
    });

    test('renders address book for customer', () => {
        useCheckoutPage.mockReturnValueOnce({
            ...defaultTalonProps,
            activeContent: 'addressBook',
            customer: { default_shipping: '1' },
            isGuestCheckout: false
        });

        const tree = render(<CheckoutPage />);
        expect(tree.toJSON()).toMatchSnapshot();
    });

    test('renders sign in for guest', () => {
        useCheckoutPage.mockReturnValueOnce({
            ...defaultTalonProps,
            activeContent: 'signIn'
        });

        const tree = render(<CheckoutPage />);
        const { root } = tree;
        const signInComponent = root.findByType('GuestSignIn');

        expect(signInComponent.props.isActive).toBe(true);
    });

    test('renders empty cart', () => {
        useCheckoutPage.mockReturnValueOnce({
            ...defaultTalonProps,
            isCartEmpty: true
        });

        const tree = render(<CheckoutPage />);
        expect(tree.toJSON()).toMatchSnapshot();
    });

    test('renders price adjustments and review order button', () => {
        useCheckoutPage.mockReturnValueOnce({
            ...defaultTalonProps,
            checkoutStep: CHECKOUT_STEP.PAYMENT,
            handleReviewOrder: jest.fn().mockName('handleReviewOrder'),
            isUpdating: true
        });

        const tree = render(<CheckoutPage />);
        const priceAdjustmentsComponent = tree.root.findByProps({
            className: 'price_adjustments_container'
        });
        const reviewOrderButtonComponent = tree.root.findByProps({
            className: 'review_order_button'
        });

        expect(priceAdjustmentsComponent.props).toMatchSnapshot();
        expect(reviewOrderButtonComponent.props).toMatchSnapshot();
    });

    test('renders an error and disables review order button if there is no payment method', () => {
        useCheckoutPage.mockReturnValueOnce({
            ...defaultTalonProps,
            checkoutStep: CHECKOUT_STEP.PAYMENT,
            isUpdating: true,
            availablePaymentMethods: []
        });

        const tree = render(<CheckoutPage />);
        const formErrorComponent = tree.root.findByType(FormError);
        const reviewOrderButtonComponent = tree.root.findByProps({
            className: 'review_order_button'
        });

        expect(tree).toMatchSnapshot();
        expect(formErrorComponent.props.errors[0]).toEqual(new Error('Payment is currently unavailable.'));
        expect(reviewOrderButtonComponent.props.disabled).toBe(true);
    });
});
