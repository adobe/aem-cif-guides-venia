  
import React from 'react';
import { Provider as ReduxProvider } from 'react-redux';
import {
    PeregrineContextProvider as Peregrine,
    ToastContextProvider,
    WindowSizeContextProvider
} from '@magento/peregrine';

import store from './store';

/**
 * List of context providers that are required to run Venia
 *
 * @property {React.Component[]} contextProviders
 */
const contextProviders = [
    Peregrine,
    WindowSizeContextProvider,
    ToastContextProvider
];

const ContextProvider = ({ children }) => {
    return (
        <ReduxProvider store={store}>
            {contextProviders.reduceRight((memo, ContextProvider) => {
                return <ContextProvider>{memo}</ContextProvider>;
            }, children)}
        </ReduxProvider>
    );    
};

export default ContextProvider;
