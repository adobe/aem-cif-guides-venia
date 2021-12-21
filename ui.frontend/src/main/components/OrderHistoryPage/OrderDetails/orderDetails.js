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
import { shape, string, arrayOf, number } from 'prop-types';
import { FormattedMessage } from 'react-intl';

import { Printer } from 'react-feather';
import { useStyle } from '@magento/venia-ui/lib/classify';

import BillingInformation from '@magento/venia-ui/lib/components/OrderHistoryPage/OrderDetails/billingInformation';
import Items from './items';
import PaymentMethod from '@magento/venia-ui/lib/components/OrderHistoryPage/OrderDetails/paymentMethod';
import ShippingInformation from '@magento/venia-ui/lib/components/OrderHistoryPage/OrderDetails/shippingInformation';
import ShippingMethod from '@magento/venia-ui/lib/components/OrderHistoryPage/OrderDetails/shippingMethod';
import OrderTotal from '@magento/venia-ui/lib/components/OrderHistoryPage/OrderDetails/orderTotal';
import Icon from '@magento/venia-ui/lib/components/Icon';
import Button from '@magento/venia-ui/lib/components/Button';

import defaultClasses from '@magento/venia-ui/lib/components/OrderHistoryPage/OrderDetails/orderDetails.css';

const OrderDetails = props => {
    const { classes: propClasses, imagesData, orderData } = props;
    const { billing_address, items, payment_methods, shipping_address, shipping_method, shipments, total } = orderData;
    const classes = useStyle(defaultClasses, propClasses);

    const shippingMethodData = {
        shippingMethod: shipping_method,
        shipments
    };

    return (
        <div className={classes.root}>
            <div className={classes.shippingInformationContainer}>
                <ShippingInformation data={shipping_address} />
            </div>
            <div className={classes.shippingMethodContainer}>
                <ShippingMethod data={shippingMethodData} />
            </div>
            <div className={classes.billingInformationContainer}>
                <BillingInformation data={billing_address} />
            </div>
            <div className={classes.paymentMethodContainer}>
                <PaymentMethod data={payment_methods} />
            </div>
            <div className={classes.itemsContainer}>
                <Items data={{ imagesData, items }} />
            </div>
            <div className={classes.orderTotalContainer}>
                <OrderTotal data={total} />
            </div>
            <Button
                className={classes.printButton}
                onClick={() => {
                    // TODO will be implemented in PWA-978
                    console.log('Printing Receipt');
                }}
            >
                <Icon src={Printer} />
                <span className={classes.printLabel}>
                    <FormattedMessage id="orderDetails.printLabel" defaultMessage="Print Receipt" />
                </span>
            </Button>
        </div>
    );
};

export default OrderDetails;

OrderDetails.propTypes = {
    classes: shape({
        root: string,
        shippingInformationContainer: string,
        shippingMethodContainer: string,
        billingInformationContainer: string,
        paymentMethodContainer: string,
        itemsContainer: string,
        orderTotalContainer: string,
        printButton: string,
        printLabel: string
    }),
    imagesData: arrayOf(
        shape({
            id: number,
            sku: string,
            thumbnail: shape({
                url: string
            }),
            url_key: string,
            url_suffix: string
        })
    ),
    orderData: shape({
        billing_address: shape({
            city: string,
            country_code: string,
            firstname: string,
            lastname: string,
            postcode: string,
            region_id: string,
            street: arrayOf(string)
        }),
        items: arrayOf(
            shape({
                id: string,
                product_name: string,
                product_sale_price: shape({
                    currency: string,
                    value: number
                }),
                product_sku: string,
                selected_options: arrayOf(
                    shape({
                        label: string,
                        value: string
                    })
                ),
                quantity_ordered: number
            })
        ),
        payment_methods: arrayOf(
            shape({
                type: string,
                additional_data: arrayOf(
                    shape({
                        name: string,
                        value: string
                    })
                )
            })
        ),
        shipping_address: shape({
            city: string,
            country_code: string,
            firstname: string,
            lastname: string,
            postcode: string,
            region_id: string,
            street: arrayOf(string),
            telephone: string
        }),
        shipping_method: string,
        shipments: arrayOf(
            shape({
                id: string,
                tracking: arrayOf(
                    shape({
                        carrier: string,
                        number: string
                    })
                )
            })
        ),
        total: shape({
            discounts: arrayOf(
                shape({
                    amount: shape({
                        currency: string,
                        value: number
                    })
                })
            ),
            grand_total: shape({
                currency: string,
                value: number
            }),
            subtotal: shape({
                currency: string,
                value: number
            }),
            total_tax: shape({
                currency: string,
                value: number
            }),
            total_shipping: shape({
                currency: string,
                value: number
            })
        })
    })
};
