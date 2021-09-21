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
import partialConfig from './config';

import React from 'react';
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
    AccountDetails,
    ResetPassword,
    PortalPlacer,
    AddToCart
} from '@adobe/aem-core-cif-react-components';

import {
    ProductRecsGallery,
    StorefrontInstanceContextProvider
} from '@adobe/aem-core-cif-product-recs-extension';

import { AppContextProvider as PeregrineContextProvider } from '../Peregrine';
import CartTrigger from '@magento/venia-ui/lib/components/Header/cartTrigger';
import { HeadProvider } from '@magento/venia-ui/lib/components/Head';
import CartPage from '../CartPage';
import Redirect from '../Redirect';

import loadLocaleData from './i18n';

import '../../site/main.scss';

const App = props => {
    const { storeView, graphqlEndpoint, graphqlMethod, httpHeaders } = document.querySelector('body').dataset;
    const { mountingPoints, pagePaths } = partialConfig;
    const { locale, messages } = props;

    const config = {
        ...partialConfig,
        storeView,
        graphqlEndpoint,
        // Can be GET or POST. When selecting GET, this applies to cache-able GraphQL query requests only. Mutations
        // will always be executed as POST requests.
        graphqlMethod,
        headers: JSON.parse(httpHeaders)
    };

    window.STORE_NAME = storeView;
    window.DEFAULT_COUNTRY_CODE = locale;

    return (
        <IntlProvider locale={locale} messages={messages}>
            <ConfigContextProvider config={config}>
                <CommerceApp>
                    <PeregrineContextProvider>
                        <PortalPlacer selector={mountingPoints.addToCart} component={AddToCart} />

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

                        <Route exact path="/cart">
                            <Redirect to={pagePaths.cartDetails} />
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

                        <Route path={pagePaths.accountDetails}>
                            <Portal selector={mountingPoints.accountDetails}>
                                <AccountDetails />
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

export default App;
