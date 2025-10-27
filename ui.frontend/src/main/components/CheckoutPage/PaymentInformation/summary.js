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
import { FormattedMessage } from 'react-intl';
import { shape, string, func } from 'prop-types';

import { useSummary } from '@magento/peregrine/lib/talons/CheckoutPage/PaymentInformation/useSummary';
import { useStyle } from '@magento/venia-ui/lib/classify';

import defaultClasses from '@magento/venia-ui/lib/components/CheckoutPage/PaymentInformation/summary.module.css';
import LoadingIndicator from '@magento/venia-ui/lib/components/LoadingIndicator';
import summaryPayments from './summaryPaymentCollection';

const Summary = props => {
    const { classes: propClasses, onEdit } = props;
    const classes = useStyle(defaultClasses, propClasses);

    const talonProps = useSummary();

    const { isLoading, selectedPaymentMethod } = talonProps;

    if (isLoading && !selectedPaymentMethod) {
        return (
            <LoadingIndicator classes={{ root: classes.loading }}>
                <FormattedMessage
                    id={'checkoutPage.loadingPaymentInformation'}
                    defaultMessage={'Fetching Payment Information'}
                />
            </LoadingIndicator>
        );
    }

    const hasCustomSummaryComp = Object.keys(summaryPayments).includes(selectedPaymentMethod.code);

    if (hasCustomSummaryComp) {
        const SummaryPaymentMethodComponent = summaryPayments[selectedPaymentMethod.code];
        return <SummaryPaymentMethodComponent onEdit={onEdit} />;
    } else {
        return (
            <div className={classes.root}>
                <div className={classes.heading_container}>
                    <h5 className={classes.heading}>
                        <FormattedMessage
                            id={'checkoutPage.paymentInformation'}
                            defaultMessage={'Payment Information'}
                        />
                    </h5>
                </div>
                <div className={classes.card_details_container}>
                    <span className={classes.payment_details}>{selectedPaymentMethod.title}</span>
                </div>
            </div>
        );
    }
};

export default Summary;

Summary.propTypes = {
    classes: shape({
        root: string,
        heading_container: string,
        heading: string,
        card_details_container: string,
        payment_details: string
    }),
    onEdit: func.isRequired
};
