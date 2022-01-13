/*******************************************************************************
 *
 *    Copyright 2022 Adobe. All rights reserved.
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
import { IntlProvider } from 'react-intl';

import { renderHook } from '@testing-library/react-hooks';

import Icon from '@magento/venia-ui/lib/components/Icon';
import { Check, AlertCircle } from 'react-feather';

import useToastEvents from '../useToastEvents';

const mockAddToast = jest.fn();
jest.mock('@magento/peregrine/lib/Toasts/useToasts', () => ({
    useToasts: () => [{}, { addToast: mockAddToast }]
}));

const CheckIcon = <Icon size={20} src={Check} />;
const ErrorIcon = <Icon src={AlertCircle} size={20} />;

describe('useToastEvents', () => {
    beforeEach(() => {
        mockAddToast.mockClear();
    });

    const wrapper = ({ children }) => <IntlProvider locale="en">{children}</IntlProvider>;

    it('displays a wishlist success toast', () => {
        renderHook(() => useToastEvents(), { wrapper });

        document.dispatchEvent(
            new CustomEvent('aem.cif.toast', {
                detail: {
                    type: 'info',
                    message: 'wishlist.success'
                }
            })
        );

        expect(mockAddToast).toHaveBeenCalledTimes(1);
        expect(mockAddToast).toHaveBeenCalledWith({
            message: 'Product was added to wishlist.',
            type: 'info',
            icon: CheckIcon
        });
    });

    it('displays a wishlist error toast', () => {
        renderHook(() => useToastEvents(), { wrapper });

        document.dispatchEvent(
            new CustomEvent('aem.cif.toast', {
                detail: {
                    type: 'error',
                    message: 'wishlist.error',
                    error: 'This is some error.'
                }
            })
        );

        expect(mockAddToast).toHaveBeenCalledTimes(1);
        expect(mockAddToast).toHaveBeenCalledWith({
            message: 'Product could not be added to wishlist.',
            type: 'error',
            icon: ErrorIcon
        });
    });
});
