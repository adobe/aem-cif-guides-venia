import { Provider as ReduxProvider } from 'react-redux';
import VeniaMiniCart from '@magento/venia-ui/lib/components/MiniCart';

import store from '../../store';

const MiniCart = props => {

    return (
        <ReduxProvider store={store}>
            <VeniaMiniCart></VeniaMiniCart>
        </ReduxProvider>
    )
}

export default MiniCart;