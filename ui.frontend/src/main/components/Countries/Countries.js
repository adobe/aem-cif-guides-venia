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
import gql from 'graphql-tag';

import { useQuery } from '@apollo/react-hooks';
const countries = gql`
    query {
        country {
            id
            full_name_locale
        }
    }
`;
const Countries = props => {
    const { data, loading } = useQuery(countries);

    if (loading) {
        return <p>Loading...</p>;
    }

    return (
        <div>
            This is a list with countries:{' '}
            {data &&
                data.countries.map(c => {
                    return <div key={c.id}>{c.full_name_locale}</div>;
                })}
        </div>
    );
};

export default Countries;
