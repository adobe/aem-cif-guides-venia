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
import { useAddressBookPage } from '../../../talons/AddressBookPage/useAddressBookPage';
import { ConfigContextProvider } from '@adobe/aem-core-cif-react-components';
import AddressBookPage from '../addressBookPage';

jest.mock('@magento/venia-ui/lib/classify');

jest.mock('@magento/venia-ui/lib/components/Icon', () => 'Icon');
jest.mock('../../../talons/AddressBookPage/useAddressBookPage', () => {
    return {
        useAddressBookPage: jest.fn()
    };
});
jest.mock('@magento/venia-ui/lib/components/AddressBookPage/addEditDialog', () => 'AddEditDialog');
jest.mock('@magento/venia-ui/lib/components/AddressBookPage/addressCard', () => 'AddressCard');

const props = {};
const talonProps = {
    confirmDeleteAddressId: null,
    countryDisplayNameMap: new Map([['US', 'United States']]),
    customerAddresses: [],
    formErrors: new Map([]),
    formProps: null,
    handleAddAddress: jest.fn().mockName('handleAddAddress'),
    handleCancelDeleteAddress: jest.fn().mockName('handleCancelDeleteAddress'),
    handleCancelDialog: jest.fn().mockName('handleCancelDialog'),
    handleConfirmDeleteAddress: jest.fn().mockName('handleConfirmDeleteAddress'),
    handleConfirmDialog: jest.fn().mockName('handleConfirmDialog'),
    handleDeleteAddress: jest.fn().mockName('handleDeleteAddress'),
    handleEditAddress: jest.fn().mockName('handleEditAddress'),
    isDeletingCustomerAddress: false,
    isDialogBusy: false,
    isDialogEditMode: false,
    isDialogOpen: false,
    isLoading: false
};

const config = {
    pagePaths: {
        baseUrl: '/content/venia/us/en.html'
    }
};

it('renders correctly when there are no existing addresses', () => {
    // Arrange.
    useAddressBookPage.mockReturnValueOnce(talonProps);

    // Act.
    const instance = render(
        <ConfigContextProvider config={config}>
            <AddressBookPage {...props} />
        </ConfigContextProvider>
    );

    // Assert.
    expect(instance.toJSON()).toMatchSnapshot();
});

it('renders loading indicator', () => {
    useAddressBookPage.mockReturnValueOnce({ ...talonProps, isLoading: true });

    const instance = render(
        <ConfigContextProvider config={config}>
            <AddressBookPage {...props} />
        </ConfigContextProvider>
    );

    expect(instance.toJSON()).toMatchSnapshot();
});

it('renders correctly when there are existing addresses', () => {
    // Arrange.
    const myTalonProps = {
        ...talonProps,
        customerAddresses: [
            { id: 'a', country_code: 'US' },
            { id: 'b', country_code: 'US', default_shipping: true },
            { id: 'c', country_code: 'FR' }
        ]
    };
    useAddressBookPage.mockReturnValueOnce(myTalonProps);

    // Act.
    const instance = render(
        <ConfigContextProvider config={config}>
            <AddressBookPage {...props} />
        </ConfigContextProvider>
    );

    // Assert.
    expect(instance.toJSON()).toMatchSnapshot();
});

it('renders delete confirmation on address that is being deleted', () => {
    // Arrange.
    const myTalonProps = {
        ...talonProps,
        customerAddresses: [
            { id: 'a', country_code: 'US' },
            { id: 'b', country_code: 'US', default_shipping: true },
            { id: 'c', country_code: 'FR' }
        ],
        isDeletingCustomerAddress: true,
        confirmDeleteAddressId: 'a'
    };
    useAddressBookPage.mockReturnValueOnce(myTalonProps);

    // Act.
    const instance = render(
        <ConfigContextProvider config={config}>
            <AddressBookPage {...props} />
        </ConfigContextProvider>
    );

    // Assert.
    expect(instance.toJSON()).toMatchSnapshot();
});
