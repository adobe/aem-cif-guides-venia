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
import { func, shape, string } from 'prop-types';
import { FormattedMessage } from 'react-intl';

import { useStyle } from '@magento/venia-ui/lib/classify';
import { useAccountMenuItems } from './useAccountMenuItems';

import defaultClasses from '@magento/venia-ui/lib/components/AccountMenu/accountMenuItems.css';

const AccountMenuItems = props => {
    const { onSignOut, showWishList } = props;

    const talonProps = useAccountMenuItems({ onSignOut, showWishList });
    const { handleSignOut, menuItems } = talonProps;

    const classes = useStyle(defaultClasses, props.classes);

    const menu = menuItems.map(item => {
        return (
            <a className={classes.link} key={item.name} href={item.url}>
                <FormattedMessage id={item.id} defaultMessage={item.name} />
            </a>
        );
    });

    return (
        <div className={classes.root}>
            {menu}
            <button className={classes.signOut} onClick={handleSignOut} type="button">
                <FormattedMessage id={'accountMenu.signOutButtonText'} defaultMessage={'Sign Out'} />
            </button>
        </div>
    );
};

export default AccountMenuItems;

AccountMenuItems.propTypes = {
    classes: shape({
        link: string,
        signOut: string
    }),
    onSignOut: func
};
