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

package com.venia.it.tests;

import java.util.Collections;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.Test;

public class SearchPageIT extends CommerceTestBase {

    private static final String SEARCHRESULTS_SELECTOR = ".searchresults ";

    @Test
    public void testSearchResultsWithSampleData() throws ClientException {
        List<NameValuePair> parameters = Collections.singletonList(new BasicNameValuePair("search_query", "test"));
        SlingHttpResponse response = adminAuthor.doGet(VENIA_CONTENT_US_EN_SEARCH_PAGE + ".html", parameters, 200);
        Document doc = Jsoup.parse(response.getContent());

        // Check that search filters are displayed
        Elements elements = doc.select(SEARCHRESULTS_SELECTOR + SEARCH_FILTERS_SELECTOR);
        Assert.assertEquals(1, elements.size());

        // Check that the 6 products are displayed on the first page
        elements = doc.select(SEARCHRESULTS_SELECTOR + PRODUCTCOLLECTION_GALLERY_ITEMS_SELECTOR);
        Assert.assertEquals(6, elements.size());

        // Verify breadcrumb: Home
        // elements = doc.select(BREADCRUMB_ITEMS_SELECTOR);
        // Assert.assertEquals(1, elements.size());

        // Check the number of root elements in the navigation menu
        elements = doc.select(NAVIGATION_ITEM_SELECTOR);
        Assert.assertEquals(7, elements.size());
    }

    @Test
    public void testSearchResultsWithoutSearchQuery() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(VENIA_CONTENT_US_EN_SEARCH_PAGE + ".html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Check that the search doesn't display any product
        Elements elements = doc.select(SEARCHRESULTS_SELECTOR + ".category__root p");
        Assert.assertEquals("No products to display.", elements.first().html());
    }
}
