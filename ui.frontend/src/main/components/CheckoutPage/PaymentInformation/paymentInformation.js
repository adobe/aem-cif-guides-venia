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

import React, { Suspense } from 'react';
import { FormattedMessage } from 'react-intl';
import { Form } from 'informed';
import { shape, func, string, bool, instanceOf } from 'prop-types';

import { usePaymentInformation } from '@magento/peregrine/lib/talons/CheckoutPage/PaymentInformation/usePaymentInformation';
import CheckoutError from '@magento/peregrine/lib/talons/CheckoutPage/CheckoutError';

import useStyle from '@magento/peregrine/lib/util/shallowMerge';
import paymentInformationOperations from '@magento/venia-ui/lib/components/CheckoutPage/PaymentInformation/paymentInformation.gql';
import defaultClasses from '@magento/venia-ui/lib/components/CheckoutPage/PaymentInformation/paymentInformation.css';
import LoadingIndicator from '@magento/venia-ui/lib/components/LoadingIndicator';

const PaymentMethods = React.lazy(() => import('./paymentMethods'));
const EditModal = React.lazy(() => import('./editModal'));
const Summary = React.lazy(() => import('./summary'));

const PaymentInformation = props => {
    const { classes: propClasses, onSave, resetShouldSubmit, setCheckoutStep, shouldSubmit, checkoutError } = props;

    const classes = useStyle(defaultClasses, propClasses);

    const talonProps = usePaymentInformation({
        onSave,
        checkoutError,
        resetShouldSubmit,
        setCheckoutStep,
        shouldSubmit,
        ...paymentInformationOperations
    });

    const {
        doneEditing,
        handlePaymentError,
        handlePaymentSuccess,
        hideEditModal,
        isLoading,
        isEditModalActive,
        showEditModal
    } = talonProps;

    if (isLoading) {
        return (
            <LoadingIndicator classes={{ root: classes.loading }}>
                <FormattedMessage
                    id={'checkoutPage.loadingPaymentInformation'}
                    defaultMessage={'Fetching Payment Information'}
                />
            </LoadingIndicator>
        );
    }

    const paymentInformation = doneEditing ? (
        <Summary onEdit={showEditModal} />
    ) : (
        <Form>
            <PaymentMethods
                onPaymentError={handlePaymentError}
                onPaymentSuccess={handlePaymentSuccess}
                resetShouldSubmit={resetShouldSubmit}
                shouldSubmit={shouldSubmit}
            />
        </Form>
    );

    const editModal = doneEditing ? (
        <Suspense fallback={null}>
            <EditModal onClose={hideEditModal} isOpen={isEditModalActive} />
        </Suspense>
    ) : null;

    return (
        <div className={classes.root}>
            <div className={classes.payment_info_container}>
                <Suspense fallback={null}>{paymentInformation}</Suspense>
            </div>
            {editModal}
        </div>
    );
};

export default PaymentInformation;

PaymentInformation.propTypes = {
    classes: shape({
        container: string,
        payment_info_container: string,
        review_order_button: string
    }),
    onSave: func.isRequired,
    checkoutError: instanceOf(CheckoutError),
    resetShouldSubmit: func.isRequired,
    shouldSubmit: bool,
    setCheckoutStep: func
};
