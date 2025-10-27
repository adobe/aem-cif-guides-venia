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

import React, { useMemo } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { bool, func } from 'prop-types';

import { useEditModal } from '@magento/peregrine/lib/talons/CheckoutPage/PaymentInformation/useEditModal';
import Dialog from '@magento/venia-ui/lib/components/Dialog';
import editModalOperations from '@magento/peregrine/lib/talons/CheckoutPage/PaymentInformation/editModal.gql.js';
import editablePayments from './editablePaymentCollection';

const EditModal = props => {
    const { onClose, isOpen } = props;
    const { formatMessage } = useIntl();

    const talonProps = useEditModal({ onClose, ...editModalOperations });

    const {
        selectedPaymentMethod,
        handleUpdate,
        handleClose,
        handlePaymentSuccess,
        handlePaymentReady,
        updateButtonClicked,
        resetUpdateButtonClicked,
        handlePaymentError
    } = talonProps;

    const paymentMethodComponent = useMemo(() => {
        const isEditable = Object.keys(editablePayments).includes(selectedPaymentMethod);
        if (isEditable) {
            const PaymentMethodComponent = editablePayments[selectedPaymentMethod];
            return (
                <PaymentMethodComponent
                    onPaymentReady={handlePaymentReady}
                    onPaymentSuccess={handlePaymentSuccess}
                    onPaymentError={handlePaymentError}
                    resetShouldSubmit={resetUpdateButtonClicked}
                    shouldSubmit={updateButtonClicked}
                />
            );
        } else {
            return (
                <div>
                    <FormattedMessage
                        id={'checkoutPage.paymentMethodStatus'}
                        defaultMessage={'The selected method is not supported for editing.'}
                        values={{ selectedPaymentMethod }}
                    />
                </div>
            );
        }
    }, [
        handlePaymentError,
        handlePaymentReady,
        handlePaymentSuccess,
        resetUpdateButtonClicked,
        selectedPaymentMethod,
        updateButtonClicked
    ]);

    return (
        <Dialog
            confirmText={'Update'}
            confirmTranslationId={'global.updateButton'}
            isOpen={isOpen}
            onCancel={handleClose}
            onConfirm={handleUpdate}
            shouldDisableAllButtons={updateButtonClicked}
            shouldDisableConfirmButton={updateButtonClicked}
            title={formatMessage({
                id: 'checkoutPage.editPaymentInformation',
                defaultMessage: 'Edit Payment Information'
            })}
        >
            {paymentMethodComponent}
        </Dialog>
    );
};

export default EditModal;

EditModal.propTypes = {
    onClose: func.isRequired,
    isOpen: bool
};
