
import React from 'react';
import { AppContextProvider } from '../Peregrine';
import VeniaCartTrigger from '@magento/venia-ui/lib/components/Header/cartTrigger';


const CartTrigger = props => {
    return (
        <AppContextProvider>
            <VeniaCartTrigger></VeniaCartTrigger>
        </AppContextProvider>
    )
}

export default CartTrigger;