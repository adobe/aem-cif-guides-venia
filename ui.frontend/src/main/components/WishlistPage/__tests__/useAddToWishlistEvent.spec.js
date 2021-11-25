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
import React from 'react';
import { render, wait } from '@testing-library/react';

jest.mock('@magento/peregrine/lib/talons/Wishlist/AddToListButton/addToListButton.gql', () => ({
    addProductToWishlistMutation: 'default'
}));
jest.mock('@apollo/client', () => ({
    useMutation: jest.fn().mockImplementation(fn => [fn])
}));

import { useMutation } from '@apollo/client';
import useAddToWishlistEvent from '../useAddToWishlistEvent';

const dispatchEvent = items =>
    document.dispatchEvent(
        new CustomEvent('aem.cif.add-to-wishlist', {
            detail: items
        })
    );

const MockComponent = props => {
    useAddToWishlistEvent(props);
    return <></>;
};

const testEventDetails = async (mockFn, result) => {
    await wait(() => {
        expect(mockFn).toHaveBeenCalledTimes(1);
        expect(mockFn).toHaveBeenCalledWith({
            variables: { wishlistId: '0', itemOptions: result }
        });
    });
};

test('uses default operation', async () => {
    render(<MockComponent />);
    expect(useMutation).toHaveBeenCalledWith('default');
});

test('uses custom operation', async () => {
    render(<MockComponent operations={{ addProductToWishlistMutation: 'custom' }} />);
    expect(useMutation).toHaveBeenCalledWith('custom');
});

test('handles event with string details', async () => {
    const addProductToWishlistMutationMock = jest.fn();
    render(<MockComponent operations={{ addProductToWishlistMutation: addProductToWishlistMutationMock }} />);

    dispatchEvent('[{"foo": "bar"}]');

    await testEventDetails(addProductToWishlistMutationMock, {
        foo: 'bar'
    });
});

test('handles event with JSON details', async () => {
    const addProductToWishlistMutationMock = jest.fn();
    render(<MockComponent operations={{ addProductToWishlistMutation: addProductToWishlistMutationMock }} />);

    dispatchEvent([{ foo: 'bar' }]);

    await testEventDetails(addProductToWishlistMutationMock, {
        foo: 'bar'
    });
});
