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
import React, { Fragment, Suspense, useEffect } from 'react';
import { FormattedMessage } from 'react-intl';
import { useHistory } from 'react-router-dom';
import { useAccountInformationPage } from '@magento/peregrine/lib/talons/AccountInformationPage/useAccountInformationPage';
import { useConfigContext } from '@adobe/aem-core-cif-react-components';

import { useStyle } from '@magento/venia-ui/lib/classify';
import Button from '@magento/venia-ui/lib/components/Button';
import { Message } from '@magento/venia-ui/lib/components/Field';
import { fullPageLoadingIndicator } from '@magento/venia-ui/lib/components/LoadingIndicator';

import defaultClasses from '@magento/venia-ui/lib/components/AccountInformationPage/accountInformationPage.module.css';
import AccountInformationPageOperations from '@magento/venia-ui/lib/components/AccountInformationPage/accountInformationPage.gql.js';

const EditModal = React.lazy(() => import('@magento/venia-ui/lib/components/AccountInformationPage/editModal'));

const AccountInformationPage = props => {
    const { pagePaths } = useConfigContext();
    const classes = useStyle(defaultClasses, props.classes);

    const history = useHistory();
    const talonProps = useAccountInformationPage({
        ...AccountInformationPageOperations
    });

    const {
        handleCancel,
        formErrors,
        handleChangePassword,
        handleSubmit,
        initialValues,
        isDisabled,
        isSignedIn,
        isUpdateMode,
        loadDataError,
        shouldShowNewPassword,
        showUpdateMode
    } = talonProps;

    // If the user is no longer signed in, redirect to the home page.
    useEffect(() => {
        if (!isSignedIn) {
            history.replace(pagePaths.baseUrl);
            history.go(0);
        }
    }, [history, isSignedIn]);

    const errorMessage = loadDataError ? (
        <Message>
            <FormattedMessage
                id={'accountInformationPage.errorTryAgain'}
                defaultMessage={'Something went wrong. Please refresh and try again.'}
            />
        </Message>
    ) : null;

    let pageContent = null;
    if (!initialValues) {
        return fullPageLoadingIndicator;
    } else {
        const { customer } = initialValues;
        const customerName = `${customer.firstname} ${customer.lastname}`;
        const passwordValue = '***********';

        pageContent = (
            <Fragment>
                <div className={classes.accountDetails}>
                    <div className={classes.lineItemsContainer}>
                        <span className={classes.nameLabel}>
                            <FormattedMessage id={'global.name'} defaultMessage={'Name'} />
                        </span>
                        <span className={classes.nameValue}>{customerName}</span>
                        <span className={classes.emailLabel}>
                            <FormattedMessage id={'global.email'} defaultMessage={'Email'} />
                        </span>
                        <span className={classes.emailValue}>{customer.email}</span>
                        <span className={classes.passwordLabel}>
                            <FormattedMessage id={'global.password'} defaultMessage={'Password'} />
                        </span>
                        <span className={classes.passwordValue}>{passwordValue}</span>
                    </div>
                    <div className={classes.editButtonContainer}>
                        <Button
                            className={classes.editInformationButton}
                            disabled={false}
                            onClick={showUpdateMode}
                            priority="normal"
                        >
                            <FormattedMessage id={'global.editButton'} defaultMessage={'Edit'} />
                        </Button>
                    </div>
                </div>
                <Suspense fallback={null}>
                    <EditModal
                        formErrors={formErrors}
                        initialValues={customer}
                        isDisabled={isDisabled}
                        isOpen={isUpdateMode}
                        onCancel={handleCancel}
                        onChangePassword={handleChangePassword}
                        onSubmit={handleSubmit}
                        shouldShowNewPassword={shouldShowNewPassword}
                    />
                </Suspense>
            </Fragment>
        );
    }

    return (
        <div className={classes.root}>
            <h1 className={classes.title}>
                <FormattedMessage
                    id={'accountInformationPage.accountInformation'}
                    defaultMessage={'Account Information'}
                />
            </h1>
            {errorMessage ? errorMessage : pageContent}
        </div>
    );
};

export default AccountInformationPage;
