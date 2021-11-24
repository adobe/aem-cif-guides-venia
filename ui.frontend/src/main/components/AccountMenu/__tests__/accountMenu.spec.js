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

import { createTestInstance } from '@magento/peregrine';
import { useAccountMenu } from '@magento/peregrine/lib/talons/Header/useAccountMenu';

import AccountMenu from '../accountMenu';

jest.mock('@magento/venia-ui/lib/classify');
/* eslint-disable react/display-name */
jest.mock('../accountMenuItems', () => props => <mock-AccountMenuItems {...props} />);
jest.mock('@magento/venia-ui/lib/components/SignIn/signIn', () => props => <mock-SignIn {...props} />);
jest.mock('@magento/venia-ui/lib/components/ForgotPassword', () => props => <mock-ForgotPassword {...props} />);
jest.mock('@magento/venia-ui/lib/components/CreateAccount', () => props => <mock-CreateAccount {...props} />);
/* eslint-enable react/display-name */

jest.mock('@magento/peregrine/lib/talons/Header/useAccountMenu', () => ({
    useAccountMenu: jest.fn().mockReturnValue({
        view: 'ACCOUNT',
        username: 'gooseton',
        handleSignOut: jest.fn(),
        handleForgotPassword: jest.fn(),
        handleCreateAccount: jest.fn(),
        handleForgotPasswordCancel: jest.fn(),
        updateUsername: jest.fn()
    })
}));

const defaultTalonProps = {
    view: 'ACCOUNT',
    username: 'gooseton',
    handleSignOut: jest.fn(),
    handleForgotPassword: jest.fn(),
    handleCreateAccount: jest.fn(),
    handleForgotPasswordCancel: jest.fn(),
    updateUsername: jest.fn()
};

const defaultProps = {
    accountMenuIsOpen: true,
    classes: {
        modal_active: 'modal_active_class'
    },
    setAccountMenuIsOpen: jest.fn()
};

test('it renders empty aside element when accountMenuIsOpen is false', () => {
    const props = {
        ...defaultProps,
        accountMenuIsOpen: false
    };

    // Act.
    const instance = createTestInstance(<AccountMenu {...props} />);

    // Assert.
    expect(instance.toJSON()).toMatchSnapshot();
});

test('it renders AccountMenuItems when the user is signed in', () => {
    // Act.
    const instance = createTestInstance(<AccountMenu {...defaultProps} />);

    // Assert.
    expect(instance.toJSON()).toMatchSnapshot();
});

test('it renders SignIn component when the view is SIGNIN', () => {
    useAccountMenu.mockReturnValueOnce({
        ...defaultTalonProps,
        view: 'SIGNIN'
    });

    // Act.
    const instance = createTestInstance(<AccountMenu {...defaultProps} />);

    // Assert.
    expect(instance.toJSON()).toMatchSnapshot();
});

test('it renders forgot password component when the view is FORGOT_PASSWORD', () => {
    useAccountMenu.mockReturnValueOnce({
        ...defaultTalonProps,
        view: 'FORGOT_PASSWORD'
    });

    // Act.
    const instance = createTestInstance(<AccountMenu {...defaultProps} />);

    // Assert.
    expect(instance.toJSON()).toMatchSnapshot();
});

test('it renders create account component when the view is CREATE_ACCOUNT', () => {
    useAccountMenu.mockReturnValueOnce({
        ...defaultTalonProps,
        view: 'CREATE_ACCOUNT'
    });

    // Act.
    const instance = createTestInstance(<AccountMenu {...defaultProps} />);

    // Assert.
    expect(instance.toJSON()).toMatchSnapshot();
});
