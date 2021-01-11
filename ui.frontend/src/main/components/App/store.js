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
import { combineReducers, createStore, compose, applyMiddleware } from 'redux';
import { enhancer, reducers } from '@magento/peregrine';

const sessionStorageReduxKey = 'venia-store-redux';

// Add custom enhancer to store every state change in sessionStorage
const sessionEnhancer = store => next => action => {
    const state = store.getState();
    sessionStorage.setItem(sessionStorageReduxKey, JSON.stringify({ ...store.getState().computedStates[state.currentStateIndex].state }));
    return next(action);
};

// Retrieve the initial state from sessionStorage
let initialState = {};
try {
    const value = sessionStorage.getItem(sessionStorageReduxKey);
    if (value) {
        initialState = JSON.parse(value);
    }
} catch(err) {
    console.error(err);
    initialState = {};
}

const rootReducer = combineReducers(reducers);
const rootEnhancer = compose(enhancer, applyMiddleware(sessionEnhancer));

export default createStore(rootReducer, initialState, rootEnhancer);
