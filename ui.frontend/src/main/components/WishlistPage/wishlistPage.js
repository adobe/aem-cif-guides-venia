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
import React, { Fragment, useMemo, useEffect } from 'react';
import { useHistory } from 'react-router-dom';
import { FormattedMessage, useIntl } from 'react-intl';
import { useUserContext } from '@magento/peregrine/lib/context/user';
import { useWishlistPage } from '@magento/peregrine/lib/talons/WishlistPage/useWishlistPage';
import { deriveErrorMessage } from '@magento/peregrine/lib/util/deriveErrorMessage';
import { useConfigContext } from '@adobe/aem-core-cif-react-components';

import { useStyle } from '@magento/venia-ui/lib/classify';
import { fullPageLoadingIndicator } from '@magento/venia-ui/lib/components/LoadingIndicator';
import Wishlist from './wishlist';
import defaultClasses from '@magento/venia-ui/lib/components/WishlistPage/wishlistPage.css';

const WishlistPage = props => {
    const {
        pagePaths: { baseUrl }
    } = useConfigContext();
    const history = useHistory();
    const [{ isSignedIn }] = useUserContext();

    useEffect(() => {
        if (!isSignedIn) {
            history.replace(baseUrl);
            history.go(0);
        }
    }, [history, isSignedIn]);

    const talonProps = useWishlistPage();
    const { errors, loading, shouldRenderVisibilityToggle, wishlists } = talonProps;

    const { formatMessage } = useIntl();
    const error = errors.get('getCustomerWishlistQuery');

    const classes = useStyle(defaultClasses, props.classes);
    const WISHLIST_DISABLED_MESSAGE = formatMessage({
        id: 'wishlistPage.wishlistDisabledMessage',
        defaultMessage: 'The wishlist is not currently available.'
    });
    const wishlistElements = useMemo(() => {
        if (wishlists.length === 0) {
            return <Wishlist />;
        }

        return wishlists.map((wishlist, index) => (
            <Wishlist
                key={wishlist.id}
                isCollapsed={index !== 0}
                data={wishlist}
                shouldRenderVisibilityToggle={shouldRenderVisibilityToggle}
            />
        ));
    }, [shouldRenderVisibilityToggle, wishlists]);

    if (loading && !error) {
        return fullPageLoadingIndicator;
    }

    let content;
    if (error) {
        const derivedErrorMessage = deriveErrorMessage([error]);
        const errorElement =
            derivedErrorMessage === WISHLIST_DISABLED_MESSAGE ? (
                <p>
                    <FormattedMessage
                        id={'wishlistPage.disabledMessage'}
                        defaultMessage={'Sorry, this feature has been disabled.'}
                    />
                </p>
            ) : (
                <p className={classes.fetchError}>
                    <FormattedMessage
                        id={'wishlistPage.fetchErrorMessage'}
                        defaultMessage={'Something went wrong. Please refresh and try again.'}
                    />
                </p>
            );

        content = <div className={classes.errorContainer}>{errorElement}</div>;
    } else {
        content = <Fragment>{wishlistElements}</Fragment>;
    }

    return (
        <div className={classes.root}>
            <h1 className={classes.heading}>
                <FormattedMessage
                    values={{ count: wishlists.length }}
                    id={'wishlistPage.headingText'}
                    defaultMessage={'Favorites Lists'}
                />
            </h1>
            {content}
        </div>
    );
};

export default WishlistPage;
