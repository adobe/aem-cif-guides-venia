/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
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

package com.venia.it.tests;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;

import com.adobe.cq.testing.client.CQClient;
import com.adobe.cq.testing.client.CommerceClient;
import com.adobe.cq.testing.junit.rules.CQAuthorClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;

import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

public class CommerceTestBase {

    protected static final String VENIA_CONTENT_US_EN = "/content/venia/us/en";
    protected static final String VENIA_CONTENT_US_EN_PRODUCTS = VENIA_CONTENT_US_EN + "/products";
    protected static final String VENIA_CONTENT_US_EN_PRODUCTS_PRODUCT_PAGE = VENIA_CONTENT_US_EN_PRODUCTS + "/product-page";
    protected static final String VENIA_CONTENT_US_EN_PRODUCTS_CATEGORY_PAGE = VENIA_CONTENT_US_EN_PRODUCTS + "/category-page";
    protected static final String VENIA_CONTENT_US_EN_SEARCH_PAGE = VENIA_CONTENT_US_EN + "/search";

    protected static final String BREADCRUMB_ITEMS_SELECTOR = ".breadcrumb .cmp-breadcrumb__item";
    protected static final String SEARCH_FILTERS_SELECTOR = ".productcollection__filters";
    protected static final String PRODUCTCOLLECTION_GALLERY_ITEMS_SELECTOR = ".productcollection__item";
    protected static final String NAVIGATION_ITEM_SELECTOR = ".cmp-navigation > .cmp-navigation__group > .cmp-navigation__item";

    @ClassRule
    public static final CQAuthorClassRule cqBaseClassRule = new CQAuthorClassRule();

    @Rule
    public CQRule cqBaseRule = new CQRule(cqBaseClassRule.authorRule);

    protected static CQClient adminAuthor;
    protected static boolean isCloudEnvironment;

    @BeforeClass
    public static void init() {
        adminAuthor = cqBaseClassRule.authorRule.getAdminClient(CommerceClient.class);
        // check again if the environment may be a cloud environment (either skd on localhost, or an adobeaemcloud host)
        isCloudEnvironment = adminAuthor.getUrl().getAuthority().contains("localhost")
            || adminAuthor.getUrl().getAuthority().contains("adobeaemcloud");
    }

    protected static void assumeCloud() {
        assumeTrue("not a cloud environment", isCloudEnvironment);
    }

    protected static void assumeOnPrem() {
        assumeFalse("not an on-prem environment", isCloudEnvironment);
    }
}
