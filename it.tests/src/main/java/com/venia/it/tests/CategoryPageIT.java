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
import java.util.stream.Collectors;

import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.venia.it.utils.Utils;

public class CategoryPageIT extends CommerceTestBase {

    private static final String PRODUCTLIST_SELECTOR = ".productlist ";
    private static final String PRODUCTLIST_TITLE_SELECTOR = PRODUCTLIST_SELECTOR + ".category__categoryTitle";
    private static final String PRODUCTLIST_GALLERY_SELECTOR = PRODUCTLIST_SELECTOR + ".gallery__root";

    @Test
    public void testProductListPageWithSampleData() throws ClientException, IOException {
        SlingHttpResponse response = adminAuthor.doGet(VENIA_CONTENT_US_EN_PRODUCTS_CATEGORY_PAGE + ".1.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Verify category name
        Elements elements = doc.select(PRODUCTLIST_TITLE_SELECTOR);
        Assert.assertEquals("Outdoor Collection", elements.first().html());

        // Check that search filters are displayed
        elements = doc.select(PRODUCTLIST_SELECTOR + SEARCH_FILTERS_SELECTOR);
        Assert.assertEquals(1, elements.size());

        // Check that the 6 products are displayed on the first page
        elements = doc.select(PRODUCTLIST_SELECTOR + PRODUCTCOLLECTION_GALLERY_ITEMS_SELECTOR);
        Assert.assertEquals(6, elements.size());

        // Verify breadcrumb: Home > Outdoor > Collection
        elements = doc.select(BREADCRUMB_ITEMS_SELECTOR);
        Assert.assertEquals(3, elements.size());

        // Check the number of root elements in the navigation menu
        elements = doc.select(NAVIGATION_ITEM_SELECTOR);
        Assert.assertEquals(7, elements.size());

        // Verify category gallery datalayer
        elements = doc.select(PRODUCTLIST_GALLERY_SELECTOR);
        JsonNode result = Utils.OBJECT_MAPPER.readTree(elements.first().attr("data-cmp-data-layer"));
        JsonNode expected = Utils.OBJECT_MAPPER.readTree(Utils.getResource("datalayer/sample-category-gallery.json"));
        Assert.assertEquals(expected, result);

        // Verify product items datalayer attributes
        elements = doc.select(PRODUCTLIST_SELECTOR + PRODUCTCOLLECTION_GALLERY_ITEMS_SELECTOR);
        result = Utils.OBJECT_MAPPER.readTree(elements.stream()
            .map(e -> e.attr("data-cmp-data-layer"))
            .collect(Collectors.joining(",", "[", "]")));
        expected = Utils.OBJECT_MAPPER.readTree(Utils.getResource("datalayer/sample-category-items.json"));
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testProductListPageWithPlaceholderData() throws ClientException, IOException {
        SlingHttpResponse response = adminAuthor.doGet(VENIA_CONTENT_US_EN_PRODUCTS_CATEGORY_PAGE + ".html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Verify category name
        Elements elements = doc.select(PRODUCTLIST_TITLE_SELECTOR);
        Assert.assertEquals("Category name", elements.first().html());

        // Check that search filters are NOT displayed
        elements = doc.select(PRODUCTLIST_SELECTOR + SEARCH_FILTERS_SELECTOR);
        Assert.assertTrue(elements.isEmpty());

        // Check that the 6 products are displayed on the first page
        elements = doc.select(PRODUCTLIST_SELECTOR + PRODUCTCOLLECTION_GALLERY_ITEMS_SELECTOR);
        Assert.assertEquals(6, elements.size());

        // Verify breadcrumb: Home
        elements = doc.select(BREADCRUMB_ITEMS_SELECTOR);
        Assert.assertEquals(1, elements.size());

        // Verify category gallery datalayer
        elements = doc.select(PRODUCTLIST_GALLERY_SELECTOR);
        JsonNode result = Utils.OBJECT_MAPPER.readTree(elements.first().attr("data-cmp-data-layer"));
        JsonNode expected = Utils.OBJECT_MAPPER.readTree(Utils.getResource("datalayer/sample-category-gallery.json"));
        Assert.assertEquals(expected, result);

        // Verify product items datalayer attributes
        elements = doc.select(PRODUCTLIST_SELECTOR + PRODUCTCOLLECTION_GALLERY_ITEMS_SELECTOR);
        result = Utils.OBJECT_MAPPER.readTree(elements.stream()
            .map(e -> e.attr("data-cmp-data-layer"))
            .collect(Collectors.joining(",", "[", "]")));
        expected = Utils.OBJECT_MAPPER.readTree(Utils.getResource("datalayer/placeholder-category-items.json"));
        Assert.assertEquals(expected, result);
    }
}
