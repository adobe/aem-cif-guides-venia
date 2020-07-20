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
import ReactDOM from 'react-dom';
import { I18nextProvider } from 'react-i18next';
import { BrowserRouter as Router, Route } from 'react-router-dom';
import { CommerceApp, Cart, AuthBar, AccountContainer, AddressBook } from '@adobe/aem-core-cif-react-components';

import i18n from './i18n';
import config, { addressBookPath } from './config';
import '../../site/main.scss';

const App = () => {
    const { storeView, graphqlEndpoint } = document.querySelector('body').dataset;
    const baseURL = '/content/venia/us/en'; // This is suppose to be exposed by server side

    return (
        <I18nextProvider i18n={i18n} defaultNS="common">
            <CommerceApp uri={graphqlEndpoint} storeView={storeView} config={config}>
                <Cart />
                <AuthBar />
                <AccountContainer />
                <Route path={baseURL + addressBookPath} component={AddressBook} />
            </CommerceApp>
        </I18nextProvider>
    );
};

window.onload = () => {
    const mountPoint = document.getElementById('minicart');
    ReactDOM.render(
        <Router>
            <App />
        </Router>,
        mountPoint
    );
};

export default App;
