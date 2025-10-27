/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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
import { shape, string, bool } from 'prop-types';
import { FormattedMessage } from 'react-intl';
import PriceSummary from '../PriceSummary';
import { useStyle } from '@magento/venia-ui/lib/classify';

import defaultClasses from '@magento/venia-ui/lib/components/CheckoutPage/OrderSummary/orderSummary.module.css';

const OrderSummary = props => {
    const classes = useStyle(defaultClasses, props.classes);
    return (
        <div className={classes.root}>
            <h1 className={classes.title}>
                <FormattedMessage id={'checkoutPage.orderSummary'} defaultMessage={'Order Summary'} />
            </h1>
            <PriceSummary isUpdating={props.isUpdating} />
        </div>
    );
};

export default OrderSummary;

OrderSummary.propTypes = {
    classes: shape({
        root: string,
        title: string
    }),
    isUpdating: bool
};
