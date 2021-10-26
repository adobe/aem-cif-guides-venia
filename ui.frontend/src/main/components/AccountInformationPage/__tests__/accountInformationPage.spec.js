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
import { useAccountInformationPage } from '@magento/peregrine/lib/talons/AccountInformationPage/useAccountInformationPage';
import AccountInformationPage from '../accountInformationPage';
import LoadingIndicator from '@magento/venia-ui/lib/components/LoadingIndicator';
import { ConfigContextProvider } from '@adobe/aem-core-cif-react-components';
import { useHistory } from 'react-router-dom';

jest.mock('@magento/peregrine/lib/talons/AccountInformationPage/useAccountInformationPage');
jest.mock('@magento/venia-ui/lib/classify');

const handleSubmit = jest.fn().mockName('handleSubmit');
const handleCancel = jest.fn().mockName('handleCancel');
const showUpdateMode = jest.fn().mockName('showUpdateMode');

const emptyFormProps = {
    handleCancel,
    formErrors: [],
    handleSubmit,
    initialValues: {
        customer: {
            firtname: 'Foo',
            lastname: 'Bar',
            email: 'foobar@express.net'
        }
    },
    isDisabled: false,
    isSignedIn: true,
    isUpdateMode: false,
    loadDataError: null,
    showUpdateMode
};

const config = {
    pagePaths: {
        baseUrl: '/content/venia/us/en.html'
    }
};

jest.mock('react-router-dom', () => {
    const replace = jest.fn();
    const go = jest.fn();
    return {
        useHistory: () => ({
            replace,
            go
        })
    };
});

describe('AccountInformationPage', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    test('redirects when not authenticated', () => {
        useAccountInformationPage.mockReturnValue({
            isSignedIn: false
        });

        const { replace, go } = useHistory();

        render(
            <ConfigContextProvider config={config}>
                <AccountInformationPage />
            </ConfigContextProvider>
        );
        expect(replace).toHaveBeenCalledTimes(1);
        expect(replace).toHaveBeenCalledWith(config.pagePaths.baseUrl);
        expect(go).toHaveBeenCalledTimes(1);
        expect(go).toHaveBeenCalledWith(0);
    });

    test('renders a loading indicator', () => {
        useAccountInformationPage.mockReturnValueOnce({
            initialValues: null,
            isSignedIn: true
        });

        const { root } = render(
            <ConfigContextProvider config={config}>
                <AccountInformationPage />
            </ConfigContextProvider>
        );

        expect(root.findByType(LoadingIndicator)).toBeTruthy();
    });

    test('renders form error', () => {
        useAccountInformationPage.mockReturnValueOnce({
            ...emptyFormProps,
            loadDataError: { loadDataError: 'Form Error' }
        });

        const tree = render(
            <ConfigContextProvider config={config}>
                <AccountInformationPage />
            </ConfigContextProvider>
        );
        expect(tree.toJSON()).toMatchSnapshot();
    });
});
