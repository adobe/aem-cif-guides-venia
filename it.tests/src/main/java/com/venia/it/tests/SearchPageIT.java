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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.venia.it.utils.Utils;
import static org.junit.Assert.assertEquals;

public class SearchPageIT extends CommerceTestBase {

    private static final String SEARCHRESULTS_SELECTOR = ".searchresults ";
    private static final String SEARCHRESULTS_SEARCH_ROOT_SELECTOR = SEARCHRESULTS_SELECTOR + ".productcollection__root";

    @Test
    public void testSearchResultsWithSampleData() throws ClientException, IOException {
        List<NameValuePair> parameters = Arrays.asList(
            new BasicNameValuePair("search_query", "pants"),
            new BasicNameValuePair("sort_key", "name"),
            new BasicNameValuePair("sort_order", "asc"));
        SlingHttpResponse response = adminAuthor.doGet(VENIA_CONTENT_US_EN_SEARCH_PAGE + ".html", parameters, 200);
        Document doc = Jsoup.parse(response.getContent());

        // Check that search filters are displayed
        Elements elements = doc.select(SEARCHRESULTS_SELECTOR + SEARCH_FILTERS_SELECTOR);
        assertEquals("Expected 1 search filter element, but found: " + elements.size(), 1, elements.size());

        // Check that the 6 products are displayed on the first page
        elements = doc.select(SEARCHRESULTS_SELECTOR + PRODUCTCOLLECTION_GALLERY_ITEMS_SELECTOR);
        assertEquals("Expected 6 products, but found: " + elements.size(), 6, elements.size());

        // Verify breadcrumb: Home
        elements = doc.select(BREADCRUMB_ITEMS_SELECTOR);
        assertEquals("Expected 1 breadcrumb item, but found: " + elements.size(), 1, elements.size());

        // Check the number of root elements in the navigation menu
        elements = doc.select(NAVIGATION_ITEM_SELECTOR);
        assertEquals("Expected 6 navigation items, but found: " + elements.size(), 6, elements.size());

        // Verify search result gallery datalayer
        elements = doc.select(SEARCHRESULTS_SEARCH_ROOT_SELECTOR);
        JsonNode result = Utils.OBJECT_MAPPER.readTree(elements.first().attr("data-cmp-data-layer"));
        JsonNode expected = Utils.OBJECT_MAPPER.readTree(Utils.getResource("datalayer/sample-searchresult-gallery.json"));
        assertEquals("Expected search result gallery datalayer to match sample data, but found differences", expected, result);

        // Verify product items datalayer attributes
        elements = doc.select(SEARCHRESULTS_SELECTOR + PRODUCTCOLLECTION_GALLERY_ITEMS_SELECTOR);
        result = Utils.OBJECT_MAPPER.readTree(elements.stream()
            .map(e -> e.attr("data-cmp-data-layer"))
            .map(e -> e.replaceAll(",\\s*\"repo:modifyDate\":\\s*\"[\\d\\w:-]+\"", ""))
            .collect(Collectors.joining(",", "[", "]")));
        expected = Utils.OBJECT_MAPPER.readTree(Utils.getResource("datalayer/sample-searchresult-items.json"));
        assertEquals("Expected search result items datalayer to match sample data, but found differences", expected, result);
    }

    @Test
    public void testSearchResultsWithoutSearchQuery() throws ClientException, IOException {
        SlingHttpResponse response = adminAuthor.doGet(VENIA_CONTENT_US_EN_SEARCH_PAGE + ".html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Check that the search doesn't display any product
        Elements elements = doc.select(SEARCHRESULTS_SELECTOR + ".searchresults_root p");
        assertEquals("Expected 'No products to display.' message, but found: " + elements.first().html(), "No products to display.", elements.first().html());
    }
}
