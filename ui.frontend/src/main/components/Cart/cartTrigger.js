
import React, { useEffect } from 'react';
import { AppContextProvider } from '../Peregrine';
import VeniaCartTrigger from '@magento/venia-ui/lib/components/Header/cartTrigger';
import { useEventListener, useMinicart } from '@adobe/aem-core-cif-react-components'

const ADD_TO_CART_EVENT = 'aem.cif.add-to-cart';

const CartTrigger = () => {
    return (
        <AppContextProvider>
            <VeniaCartTrigger></VeniaCartTrigger>
        </AppContextProvider>
    )
}

export default CartTrigger;