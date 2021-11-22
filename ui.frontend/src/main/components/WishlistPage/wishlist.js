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
import React, { Fragment } from 'react';
import { FormattedMessage, useIntl } from 'react-intl';
import { ChevronDown, ChevronUp } from 'react-feather';
import { useWishlist } from '@magento/peregrine/lib/talons/WishlistPage/useWishlist';
import { bool, shape, string, int } from 'prop-types';

import { useStyle } from '@magento/venia-ui/lib/classify';
import LoadingIndicator from '@magento/venia-ui/lib/components/LoadingIndicator';
import Icon from '@magento/venia-ui/lib/components/Icon';
import WishlistItems from '@magento/venia-ui/lib/components/WishlistPage/wishlistItems';
import Button from '@magento/venia-ui/lib/components/Button';
import defaultClasses from '@magento/venia-ui/lib/components/WishlistPage/wishlist.css';
import ActionMenu from '@magento/venia-ui/lib/components/WishlistPage/actionMenu';
import operations from './wishlist.gql';

/**
 * A single wishlist container.
 *
 * @param {Object} props.data the data for this wishlist
 * @param {boolean} props.shouldRenderVisibilityToggle whether or not to render the visiblity toggle
 * @param {boolean} props.isCollapsed whether or not is the whislist unfolded
 */
const Wishlist = props => {
    const { data, shouldRenderVisibilityToggle, isCollapsed } = props;
    const { formatMessage } = useIntl();
    const { id, items_count: itemsCount, name, visibility } = data;

    const talonProps = useWishlist({ id, itemsCount, isCollapsed, operations });
    const { handleContentToggle, isOpen, items, isLoading, isFetchingMore, handleLoadMore } = talonProps;

    console.log('items', items);

    const classes = useStyle(defaultClasses, props.classes);
    const contentClass = isOpen ? classes.content : classes.content_hidden;
    const contentToggleIconSrc = isOpen ? ChevronUp : ChevronDown;
    const contentToggleIcon = <Icon src={contentToggleIconSrc} size={24} />;

    const itemsCountMessage =
        itemsCount && isOpen
            ? formatMessage(
                  {
                      id: 'wishlist.itemCountOpen',
                      defaultMessage: 'Showing {currentCount} of {count} items in this list'
                  },
                  { currentCount: items.length, count: itemsCount }
              )
            : formatMessage(
                  {
                      id: 'wishlist.itemCountClosed',
                      defaultMessage: `You have {count} {count, plural,
                        one {item}
                        other {items}
                      } in this list`
                  },
                  { count: itemsCount }
              );
    const loadMoreButton =
        items && items.length < itemsCount ? (
            <div>
                <Button className={classes.loadMore} disabled={isFetchingMore} onClick={handleLoadMore}>
                    <FormattedMessage id={'wishlist.loadMore'} defaultMessage={'Load more'} />
                </Button>
            </div>
        ) : null;

    const contentMessageElement = itemsCount ? (
        <Fragment>
            <WishlistItems items={items} wishlistId={id} />
            {loadMoreButton}
        </Fragment>
    ) : (
        <p className={classes.emptyListText}>
            <FormattedMessage
                id={'wishlist.emptyListText'}
                defaultMessage={'There are currently no items in this list'}
            />
        </p>
    );

    const wishlistName = name ? (
        <div className={classes.nameContainer}>
            <h2 className={classes.name}>{name}</h2>
        </div>
    ) : (
        <div className={classes.nameContainer}>
            <h2 className={classes.name}>
                <FormattedMessage id={'wishlist.name'} defaultMessage={'Wish List'} />
            </h2>
        </div>
    );

    if (isLoading) {
        return (
            <div className={classes.root}>
                <div className={classes.header}>
                    {wishlistName} {itemsCountMessage}
                    <div className={classes.buttonsContainer}>
                        <ActionMenu id={id} name={name} visibility={visibility} />
                    </div>
                </div>
                <LoadingIndicator />
            </div>
        );
    }

    const visibilityToggleClass = shouldRenderVisibilityToggle
        ? classes.visibilityToggle
        : classes.visibilityToggle_hidden;

    const buttonsContainer = id ? (
        <div className={classes.buttonsContainer}>
            <ActionMenu id={id} name={name} visibility={visibility} />
            <button className={visibilityToggleClass} onClick={handleContentToggle} type="button">
                {contentToggleIcon}
            </button>
        </div>
    ) : null;

    return (
        <div className={classes.root}>
            <div className={classes.header}>
                {wishlistName}
                <div className={classes.itemsCountContainer}>{itemsCountMessage}</div>
                {buttonsContainer}
            </div>
            <div className={contentClass}>{contentMessageElement}</div>
        </div>
    );
};

Wishlist.propTypes = {
    classes: shape({
        root: string,
        header: string,
        content: string,
        content_hidden: string,
        emptyListText: string,
        name: string,
        nameContainer: string,
        visibilityToggle: string,
        visibilityToggle_hidden: string,
        visibility: string,
        buttonsContainer: string,
        loadMore: string
    }),
    shouldRenderVisibilityToggle: bool,
    isCollapsed: bool,
    data: shape({
        id: int,
        items_count: int,
        name: string,
        visibility: string
    })
};

Wishlist.defaultProps = {
    data: {
        items_count: 0,
        items_v2: []
    }
};

export default Wishlist;
