import React from 'react';
import {
    CommerceApp,
    ConfigContextProvider,
    Portal,
    CartTrigger,
    Cart,
    AccountContainer,
} from '@adobe/aem-core-cif-react-components';
import i18n from './i18n';
import {I18nextProvider} from 'react-i18next';
import './index.css';
const partialConfig = {
    mountingPoints: {
        accountContainer: '.header__accountTrigger #miniaccount',
        addressBookContainer: '#addressbook',
        authBarContainer: 'aside.navigation__root #miniaccount',
        cartTrigger: '.header__cartTrigger',
        navPanel: 'aside.navigation__root',
        bundleProductOptionsContainer: '#bundle-product-options',
        minicart: '#minicart',
    },
    pagePaths: {
        addressBook: '/content/venia/us/en/my-account/address-book.html',
    },
};

// This component is the application entry point
const App = () => {
    const {storeView = 'default', graphqlEndpoint} = document.querySelector(
        'body'
    ).dataset;
    const {mountingPoints, pagePaths} = partialConfig;
    const config = {
        ...partialConfig,
        storeView,
        graphqlEndpoint,
    };

    console.log(`Hey, this is our app you know...`);

    return (
        <I18nextProvider i18n={i18n} defaultNS="common">
            <ConfigContextProvider config={config}>
                <CommerceApp>
                    <Portal selector={mountingPoints.cartTrigger}>
                        <CartTrigger />
                    </Portal>
                    <Portal selector={mountingPoints.minicart}>
                        <Cart />
                    </Portal>
                    <Portal selector={mountingPoints.accountContainer}>
                        <AccountContainer />
                    </Portal>
                </CommerceApp>
            </ConfigContextProvider>
        </I18nextProvider>
    );
};

export default App;
