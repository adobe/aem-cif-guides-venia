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
// import config first due to its side effects
import config from './config';

import React from 'react';
import { object, string } from 'prop-types';
import ReactDOM from 'react-dom';
import { IntlProvider } from 'react-intl';
import { BrowserRouter as Router, Route } from 'react-router-dom';
import {
    CommerceApp,
    Portal,
    ConfigContextProvider,
    AuthBar,
    AccountContainer,
    AddressBook,
    BundleProductOptions,
    GiftCartOptions,
    AccountDetails,
    ResetPassword,
    PortalPlacer
} from '@adobe/aem-core-cif-react-components';

import { ProductRecsGallery, StorefrontInstanceContextProvider } from '@adobe/aem-core-cif-product-recs-extension';

import { AppContextProvider as PeregrineContextProvider } from '../Peregrine';
import CartTrigger from '../Header/cartTrigger';
import { HeadProvider } from '@magento/venia-ui/lib/components/Head';
import CartPage from '../CartPage';
import CheckoutPage from '../CheckoutPage';

import loadLocaleData from './i18n';

import '../../site/main.scss';

const App = props => {
    const { mountingPoints, pagePaths, storeView } = config;
    const { locale, messages } = props;

    window.STORE_NAME = storeView;
    window.DEFAULT_COUNTRY_CODE = locale;

    return (
        <IntlProvider locale={locale} messages={messages}>
            <ConfigContextProvider config={config}>
                <CommerceApp>
                    <PeregrineContextProvider>
                        <StorefrontInstanceContextProvider>
                            <PortalPlacer selector={mountingPoints.productRecs} component={ProductRecsGallery} />
                        </StorefrontInstanceContextProvider>

                        <Portal selector={mountingPoints.cartTrigger}>
                            <CartTrigger />
                        </Portal>

                        <Portal selector={mountingPoints.authBarContainer}>
                            <AuthBar />
                        </Portal>

                        <Portal selector={mountingPoints.accountContainer}>
                            <AccountContainer />
                        </Portal>

                        <Route path={pagePaths.addressBook}>
                            <Portal selector={mountingPoints.addressBookContainer}>
                                <AddressBook />
                            </Portal>
                        </Route>

                        <Route path={pagePaths.cartDetails}>
                            <Portal selector={mountingPoints.cartDetailsContainer}>
                                <HeadProvider>
                                    <CartPage />
                                </HeadProvider>
                            </Portal>
                        </Route>

                        <Route path={pagePaths.resetPassword}>
                            <Portal selector={mountingPoints.resetPasswordPage}>
                                <ResetPassword />
                            </Portal>
                        </Route>

                        <Portal selector={mountingPoints.bundleProductOptionsContainer}>
                            <BundleProductOptions />
                        </Portal>

                        <Portal selector={mountingPoints.giftCardProductOptionsContainer}>
                            <GiftCartOptions />
                        </Portal>

                        <Route path={pagePaths.accountDetails}>
                            <Portal selector={mountingPoints.accountDetails}>
                                <AccountDetails />
                            </Portal>
                        </Route>

                        <Route path={pagePaths.checkoutPage}>
                            <Portal selector={mountingPoints.checkoutPageContainer}>
                                <HeadProvider>
                                    <CheckoutPage />
                                </HeadProvider>
                            </Portal>
                        </Route>
                    </PeregrineContextProvider>
                </CommerceApp>
            </ConfigContextProvider>
        </IntlProvider>
    );
};

window.onload = async () => {
    const { locale, messages } = await loadLocaleData();
    const root = document.createElement('div');

    document.body.appendChild(root);

    ReactDOM.render(
        <Router>
            <App locale={locale} messages={messages} />
        </Router>,
        root
    );
};

App.propTypes = {
    locale: string,
    messages: object
};

export default App;
