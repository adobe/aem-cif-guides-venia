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
import render from '../../utils/test-utils';
import { useToasts } from '@magento/peregrine/lib/Toasts';
import { useOrderHistoryPage } from '@magento/peregrine/lib/talons/OrderHistoryPage/useOrderHistoryPage';

import { MockedProvider } from '@apollo/client/testing';
import OrderHistoryPage from '../orderHistoryPage';

jest.mock('@magento/peregrine/lib/talons/OrderHistoryPage/useOrderHistoryPage', () => ({
    useOrderHistoryPage: jest
        .fn()
        .mockName('useOrderHistoryPage')
        .mockReturnValue({
            errorMessage: null,
            handleReset: jest.fn().mockName('handleReset'),
            handleSubmit: jest.fn().mockName('handleSubmit'),
            isBackgroundLoading: false,
            isLoadingWithoutData: false,
            loadMoreOrders: null,
            orders: [],
            pageInfo: null,
            searchText: ''
        })
}));

jest.mock('@magento/peregrine/lib/talons/OrderHistoryPage/orderHistoryContext', () => ({
    __esModule: true,
    default: props => <mock-OrderHistoryContextProvider>{props.children}</mock-OrderHistoryContextProvider>
}));

jest.mock('@magento/peregrine/lib/Toasts', () => ({
    useToasts: jest
        .fn()
        .mockName('useToasts')
        .mockReturnValue([
            {},
            {
                addToast: jest.fn().mockName('addToast')
            }
        ])
}));

jest.mock('react-router-dom', () => ({
    useHistory: () => ({
        replace: jest.fn(),
        go: jest.fn()
    })
}));
jest.mock('@magento/peregrine/lib/context/user', () => ({
    useUserContext: () => [
        {
            isSignedIn: true
        }
    ]
}));
jest.mock('@magento/venia-ui/lib/classify');
jest.mock('../orderRow', () => 'OrderRow');
jest.mock('@adobe/aem-core-cif-react-components', () => ({
    useConfigContext: () => ({
        pagePaths: {
            baseUrl: '/content/venia/us/en.html'
        }
    })
}));

const talonProps = {
    errorMessage: null,
    handleReset: jest.fn().mockName('handleReset'),
    handleSubmit: jest.fn().mockName('handleSubmit'),
    isBackgroundLoading: false,
    isLoadingWithoutData: false,
    loadMoreOrders: null,
    orders: [],
    pageInfo: null,
    searchText: ''
};

test('renders loading indicator', () => {
    useOrderHistoryPage.mockReturnValueOnce({
        ...talonProps,
        isLoadingWithoutData: true,
        orders: []
    });

    const tree = render(
        <MockedProvider>
            <OrderHistoryPage />
        </MockedProvider>
    );

    expect(tree.toJSON()).toMatchSnapshot();
});

test('renders correctly without data', () => {
    useOrderHistoryPage.mockReturnValueOnce({
        ...talonProps,
        isLoadingWithoutData: false,
        orders: []
    });

    const tree = render(
        <MockedProvider>
            <OrderHistoryPage />
        </MockedProvider>
    );

    expect(tree.toJSON()).toMatchSnapshot();
});

test('renders correctly with data', () => {
    useOrderHistoryPage.mockReturnValueOnce({
        ...talonProps,
        isLoadingWithoutData: false,
        loadMoreOrders: jest.fn().mockName('loadMoreOrders'),
        orders: [{ id: 1 }, { id: 2 }, { id: 3 }],
        pageInfo: { current: 3, total: 6 }
    });

    const tree = render(
        <MockedProvider>
            <OrderHistoryPage />
        </MockedProvider>
    );

    expect(tree.toJSON()).toMatchSnapshot();
});

test('renders error messages if any', () => {
    useOrderHistoryPage.mockReturnValueOnce({
        ...talonProps,
        errorMessage: 'Some Error Message'
    });
    const addToast = jest.fn();
    useToasts.mockReturnValueOnce([{}, { addToast }]);

    render(
        <MockedProvider>
            <OrderHistoryPage />
        </MockedProvider>
    );

    expect(addToast).toHaveBeenCalled();
    expect(addToast.mock.calls).toMatchSnapshot();
});

test('renders invalid order id message if order id is wrong', () => {
    useOrderHistoryPage.mockReturnValueOnce({
        ...talonProps,
        searchText: '********',
        isBackgroundLoading: false,
        orders: []
    });

    const tree = render(
        <MockedProvider>
            <OrderHistoryPage />
        </MockedProvider>
    );

    expect(tree.toJSON()).toMatchSnapshot();
});

test('renders no orders message is orders is empty', () => {
    useOrderHistoryPage.mockReturnValueOnce({
        ...talonProps,
        searchText: null,
        isBackgroundLoading: false,
        orders: []
    });

    const tree = render(
        <MockedProvider>
            <OrderHistoryPage />
        </MockedProvider>
    );

    expect(tree.toJSON()).toMatchSnapshot();
});
