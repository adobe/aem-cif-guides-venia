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

import React, { useEffect } from 'react';
import { useIntl } from 'react-intl';

import { useToasts } from '@magento/peregrine/lib/Toasts/useToasts';
import Icon from '@magento/venia-ui/lib/components/Icon';
import { Check, AlertCircle } from 'react-feather';

const CheckIcon = <Icon size={20} src={Check} />;
const ErrorIcon = <Icon src={AlertCircle} size={20} />;

const useToastEvents = () => {
    const { formatMessage } = useIntl();
    const [, { addToast }] = useToasts();

    const eventHandler = event => {
        const { type, message } = event.detail;

        let toastMessage = message;
        switch (message) {
            case 'wishlist.success':
                toastMessage = formatMessage({
                    id: 'toast.wishlist.success',
                    defaultMessage: 'Product was added to wishlist.'
                });
                break;
            case 'wishlist.error':
                toastMessage = formatMessage({
                    id: 'toast.wishlist.error',
                    defaultMessage: 'Product could not be added to wishlist.'
                });
        }

        let icon;
        if (type === 'info') {
            icon = CheckIcon;
        } else if (type === 'error') {
            icon = ErrorIcon;
        }

        addToast({ type, icon, message: toastMessage });
    };

    useEffect(() => {
        document.addEventListener('aem.cif.toast', eventHandler);
        return () => {
            document.removeEventListener('aem.cif.toast', eventHandler);
        };
    }, []);
};

export default useToastEvents;
