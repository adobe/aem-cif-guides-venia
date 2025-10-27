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
import React, { Fragment, Suspense } from 'react';
import { FormattedMessage } from 'react-intl';
import { gql } from '@apollo/client';
import { useProductListing } from '@magento/peregrine/lib/talons/CartPage/ProductListing/useProductListing';

import { useStyle } from '@magento/venia-ui/lib/classify';
import LoadingIndicator from '@magento/venia-ui/lib/components/LoadingIndicator';
import defaultClasses from '@magento/venia-ui/lib/components/CartPage/ProductListing/productListing.module.css';
import Product from './product';
import { ProductListingFragment } from '@magento/peregrine/lib/talons/CartPage/ProductListing/productListingFragments.gql.js';

const EditModal = React.lazy(
    async () => await import('@magento/venia-ui/lib/components/CartPage/ProductListing/EditModal').default
);
/**
 * A child component of the CartPage component.
 * This component renders the product listing on the cart page.
 *
 * @param {Object} props
 * @param {Function} props.setIsCartUpdating Function for setting the updating state of the cart.
 * @param {Object} props.classes CSS className overrides.
 * See [productListing.css]{@link https://github.com/magento/pwa-studio/blob/develop/packages/venia-ui/lib/components/CartPage/ProductListing/productListing.css}
 * for a list of classes you can override.
 *
 * @returns {React.Element}
 *
 * @example <caption>Importing into your project</caption>
 * import ProductListing from "@magento/venia-ui/lib/components/CartPage/ProductListing";
 */
const ProductListing = props => {
    const { onAddToWishlistSuccess, setIsCartUpdating, fetchCartDetails } = props;
    const talonProps = useProductListing({
        queries: {
            getProductListing: GET_PRODUCT_LISTING
        }
    });
    const { activeEditItem, isLoading, items, setActiveEditItem, wishlistConfig } = talonProps;

    const classes = useStyle(defaultClasses, props.classes);

    if (isLoading) {
        return (
            <LoadingIndicator>
                <FormattedMessage id={'productListing.loading'} defaultMessage={'Fetching Cart...'} />
            </LoadingIndicator>
        );
    }

    if (items.length) {
        const productComponents = items.map(product => (
            <Product
                item={product}
                key={product.id}
                setActiveEditItem={setActiveEditItem}
                setIsCartUpdating={setIsCartUpdating}
                onAddToWishlistSuccess={onAddToWishlistSuccess}
                fetchCartDetails={fetchCartDetails}
                wishlistConfig={wishlistConfig}
            />
        ));

        return (
            <Fragment>
                <ul className={classes.root}>{productComponents}</ul>
                <Suspense fallback={null}>
                    <EditModal
                        item={activeEditItem}
                        setIsCartUpdating={setIsCartUpdating}
                        setActiveEditItem={setActiveEditItem}
                    />
                </Suspense>
            </Fragment>
        );
    }

    return null;
};

export const GET_PRODUCT_LISTING = gql`
    query getProductListing($cartId: String!) {
        cart(cart_id: $cartId) {
            id
            ...ProductListingFragment
        }
    }
    ${ProductListingFragment}
`;

export default ProductListing;
