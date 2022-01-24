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
import { defineMessages, useIntl } from 'react-intl';

import { useToasts } from '@magento/peregrine/lib/Toasts/useToasts';
import Icon from '@magento/venia-ui/lib/components/Icon';
import { Check, AlertCircle } from 'react-feather';

const CheckIcon = <Icon size={20} src={Check} />;
const ErrorIcon = <Icon src={AlertCircle} size={20} />;

const messages = defineMessages({
    wishlistSuccess: {
        id: 'toast.wishlist.success',
        defaultMessage: 'Product was added to wishlist.'
    },
    wishlistError: {
        id: 'toast.wishlist.error',
        defaultMessage: 'Product could not be added to wishlist.'
    }
});

const toastEvents = {
    'aem.cif.add-to-wishlist.success': {
        message: messages.wishlistSuccess,
        type: 'info',
        icon: CheckIcon
    },
    'aem.cif.add-to-wishlist.error': {
        message: messages.wishlistError,
        type: 'error',
        icon: ErrorIcon
    }
};

const useToastEvents = () => {
    const { formatMessage } = useIntl();
    const [, { addToast }] = useToasts();

    const eventHandler = event => {
        const eventType = event.type;

        if (!(eventType in toastEvents)) {
            // Event is not in toastEvents list
            return;
        }

        const { type, icon, message } = toastEvents[eventType];

        addToast({
            type: type,
            icon: icon,
            message: formatMessage(message)
        });
    };

    useEffect(() => {
        Object.keys(toastEvents).forEach(event => document.addEventListener(event, eventHandler));
        return () => {
            Object.keys(toastEvents).forEach(event => document.removeEventListener(event, eventHandler));
        };
    }, []);
};

export default useToastEvents;
