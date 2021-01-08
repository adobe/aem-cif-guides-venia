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
/* eslint-disable react/prop-types */
import React from 'react';

import { useProduct } from '@magento/peregrine/lib/talons/RootComponents/Product/useProduct';
import { useProductFullDetail } from '@magento/peregrine/lib/talons/ProductFullDetail/useProductFullDetail';

import { GET_PRODUCT_DETAIL_QUERY } from './product.gql';

import { ADD_CONFIGURABLE_MUTATION, ADD_SIMPLE_MUTATION } from './DummyAddToCart.gql';

const DummyAddToCart = props => {
    const { error, loading, product } = useProduct({
        mapProduct: i => i,
        queries: {
            getProductQuery: GET_PRODUCT_DETAIL_QUERY
        },
        urlKey: props.urlKey
    });
    /* const { handleAddToCart } = useProductFullDetail({
        addConfigurableProductToCartMutation: ADD_CONFIGURABLE_MUTATION,
        addSimpleProductToCartMutation: ADD_SIMPLE_MUTATION,
        product
    }); */

    if (loading) {
        return <div>Loading</div>;
    }
    if (error) {
        return <div>Error</div>;
    }

    return (
        <div>
            <h1>{product.name}</h1>
            <button>Add to cart</button>
        </div>
    );
};

export default DummyAddToCart;
