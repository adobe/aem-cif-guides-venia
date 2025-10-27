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

import React, { useEffect } from 'react';
import { FormattedMessage } from 'react-intl';
import { object, shape, string } from 'prop-types';
import { useOrderConfirmationPage } from '@magento/peregrine/lib/talons/CheckoutPage/OrderConfirmationPage/useOrderConfirmationPage';

import { useStyle } from '@magento/venia-ui/lib/classify';
import ItemsReview from '@magento/venia-ui/lib/components/CheckoutPage/ItemsReview';
import defaultClasses from '@magento/venia-ui/lib/components/CheckoutPage/OrderConfirmationPage/orderConfirmationPage.module.css';

const OrderConfirmationPage = props => {
    const classes = useStyle(defaultClasses, props.classes);
    const { data, orderNumber } = props;

    const talonProps = useOrderConfirmationPage({
        data
    });

    const { flatData } = talonProps;

    const { city, country, email, firstname, lastname, postcode, region, shippingMethod, street } = flatData;

    const streetRows = street.map((row, index) => {
        return (
            <span key={index} className={classes.addressStreet}>
                {row}
            </span>
        );
    });

    useEffect(() => {
        const { scrollTo } = globalThis;

        if (typeof scrollTo === 'function') {
            scrollTo({
                left: 0,
                top: 0,
                behavior: 'smooth'
            });
        }
    }, []);

    const nameString = `${firstname} ${lastname}`;
    const additionalAddressString = `${city}, ${region} ${postcode} ${country}`;

    return (
        <div className={classes.root}>
            <div className={classes.mainContainer}>
                <h2 className={classes.heading}>
                    <FormattedMessage id={'checkoutPage.thankYou'} defaultMessage={'Thank you for your order!'} />
                </h2>
                <div className={classes.orderNumber}>
                    <FormattedMessage
                        id={'checkoutPage.orderNumber'}
                        defaultMessage={'Order Number: {orderNumber}'}
                        values={{ orderNumber }}
                    />
                </div>
                <div className={classes.shippingInfoHeading}>
                    <FormattedMessage id={'global.shippingInformation'} defaultMessage={'Shipping Information'} />
                </div>
                <div className={classes.shippingInfo}>
                    <span className={classes.email}>{email}</span>
                    <span className={classes.name}>{nameString}</span>
                    {streetRows}
                    <span className={classes.addressAdditional}>{additionalAddressString}</span>
                </div>
                <div className={classes.shippingMethodHeading}>
                    <FormattedMessage id={'global.shippingMethod'} defaultMessage={'Shipping Method'} />
                </div>
                <div className={classes.shippingMethod}>{shippingMethod}</div>
                <div className={classes.itemsReview}>
                    <ItemsReview data={data} />
                </div>
                <div className={classes.additionalText}>
                    <FormattedMessage
                        id={'checkoutPage.additionalText'}
                        defaultMessage={
                            'You will also receive an email with the details and we will let you know when your order has shipped.'
                        }
                    />
                </div>
            </div>
        </div>
    );
};

export default OrderConfirmationPage;

OrderConfirmationPage.propTypes = {
    classes: shape({
        addressStreet: string,
        mainContainer: string,
        heading: string,
        orderNumber: string,
        shippingInfoHeading: string,
        shippingInfo: string,
        email: string,
        name: string,
        addressAdditional: string,
        shippingMethodHeading: string,
        shippingMethod: string,
        itemsReview: string,
        additionalText: string,
        sidebarContainer: string
    }),
    data: object.isRequired,
    orderNumber: string
};
