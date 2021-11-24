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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import com.venia.it.utils.Utils;
import junit.category.IgnoreOn65;
import junit.category.IgnoreOnCloud;

import static org.junit.Assert.assertEquals;

public class CategoryPageIT extends CommerceTestBase {

    private static final String PRODUCTLIST_SELECTOR = ".productlist ";
    private static final String PRODUCTLIST_TITLE_SELECTOR = PRODUCTLIST_SELECTOR + ".category__title";
    private static final String PRODUCTLIST_GALLERY_SELECTOR = PRODUCTLIST_SELECTOR + ".productcollection__root";

    @Test
    @Category({ IgnoreOnCloud.class })
    public void testProductListPageWithSampleData65() throws ClientException, IOException {
        String pagePath = VENIA_CONTENT_US_EN_PRODUCTS_CATEGORY_PAGE + ".html/venia-bottoms/venia-pants.html";
        testProductListPageWithSampleData(
            pagePath,
            ImmutableMap.of(
                doc -> doc.select("title").first().html(), "Pants &amp; Shorts",
                // on 6.5.8 the Sites SEO API is NOT available, and so the canonical link is created using the Externalizer and its
                // default configuration
                doc -> doc.select("link[rel=canonical]").first().attr("href"), "http://localhost:4502" + pagePath
            ));
    }

    @Test
    @Category({ IgnoreOn65.class })
    public void testProductListPageWithSampleDataCloud() throws ClientException, IOException {
        String pagePath = VENIA_CONTENT_US_EN_PRODUCTS_CATEGORY_PAGE + ".html/venia-bottoms/venia-pants.html";
        testProductListPageWithSampleData(
            pagePath,
            ImmutableMap.of(
                doc -> doc.select("title").first().html(), "Pants &amp; Shorts",
                // on Cloud the Sites SEO API is available, but without any mappings configured the pagePath is returned as is as canonical
                // link
                doc -> doc.select("link[rel=canonical]").first().attr("href"), pagePath
            ));
    }

    private void testProductListPageWithSampleData(String pagePath, Map<Function<Document, String>, String> expectations)
        throws ClientException, IOException {
        SlingHttpResponse response = adminAuthor.doGet(pagePath, 200);
        Document doc = Jsoup.parse(response.getContent());

        // Verify category name
        Elements elements = doc.select(PRODUCTLIST_TITLE_SELECTOR);
        assertEquals("Pants &amp; Shorts", elements.first().html());

        // Check that search filters are displayed
        elements = doc.select(PRODUCTLIST_SELECTOR + SEARCH_FILTERS_SELECTOR);
        assertEquals(1, elements.size());

        // Check that the 6 products are displayed on the first page
        elements = doc.select(PRODUCTLIST_SELECTOR + PRODUCTCOLLECTION_GALLERY_ITEMS_SELECTOR);
        assertEquals(6, elements.size());

        // Verify breadcrumb: Home > Outdoor > Collection
        elements = doc.select(BREADCRUMB_ITEMS_SELECTOR);
        assertEquals(3, elements.size());

        // Check the number of root elements in the navigation menu
        elements = doc.select(NAVIGATION_ITEM_SELECTOR);
        assertEquals(6, elements.size());

        // Check the meta data
        for (Map.Entry<Function<Document, String>, String> expectation : expectations.entrySet()) {
            assertEquals(expectation.getValue(), expectation.getKey().apply(doc));
        }

        // Verify category gallery datalayer
        elements = doc.select(PRODUCTLIST_GALLERY_SELECTOR);
        JsonNode result = Utils.OBJECT_MAPPER.readTree(elements.first().attr("data-cmp-data-layer"));
        JsonNode expected = Utils.OBJECT_MAPPER.readTree(Utils.getResource("datalayer/sample-category-gallery.json"));
        assertEquals(expected, result);

        // Verify product items datalayer attributes
        elements = doc.select(PRODUCTLIST_SELECTOR + PRODUCTCOLLECTION_GALLERY_ITEMS_SELECTOR);
        result = Utils.OBJECT_MAPPER.readTree(elements.stream()
            .map(e -> e.attr("data-cmp-data-layer"))
            .map(e -> e.replaceAll(",\\s*\"repo:modifyDate\":\\s*\"[\\d\\w:-]+\"", ""))
            .collect(Collectors.joining(",", "[", "]")));
        expected = Utils.OBJECT_MAPPER.readTree(Utils.getResource("datalayer/sample-category-items.json"));
        assertEquals(expected, result);
    }

    @Test
    public void testProductListPageWithPlaceholderData() throws ClientException, IOException {
        SlingHttpResponse response = adminAuthor.doGet(VENIA_CONTENT_US_EN_PRODUCTS_CATEGORY_PAGE + ".html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Verify category name
        Elements elements = doc.select(PRODUCTLIST_TITLE_SELECTOR);
        assertEquals("Category name", elements.first().html());

        // Check that search filters are NOT displayed
        elements = doc.select(PRODUCTLIST_SELECTOR + SEARCH_FILTERS_SELECTOR);
        Assert.assertTrue(elements.isEmpty());

        // Check that the 6 products are displayed on the first page
        elements = doc.select(PRODUCTLIST_SELECTOR + PRODUCTCOLLECTION_GALLERY_ITEMS_SELECTOR);
        assertEquals(6, elements.size());

        // Verify breadcrumb: Home
        elements = doc.select(BREADCRUMB_ITEMS_SELECTOR);
        assertEquals(1, elements.size());

        // Verify category gallery datalayer
        elements = doc.select(PRODUCTLIST_GALLERY_SELECTOR);
        JsonNode result = Utils.OBJECT_MAPPER.readTree(elements.first().attr("data-cmp-data-layer"));
        JsonNode expected = Utils.OBJECT_MAPPER.readTree(Utils.getResource("datalayer/sample-category-gallery.json"));
        assertEquals(expected, result);

        // Verify product items datalayer attributes
        elements = doc.select(PRODUCTLIST_SELECTOR + PRODUCTCOLLECTION_GALLERY_ITEMS_SELECTOR);
        result = Utils.OBJECT_MAPPER.readTree(elements.stream()
            .map(e -> e.attr("data-cmp-data-layer"))
            .map(e -> e.replaceAll(",\\s*\"repo:modifyDate\":\\s*\"[\\d\\w:-]+\"", ""))
            .collect(Collectors.joining(",", "[", "]")));
        expected = Utils.OBJECT_MAPPER.readTree(Utils.getResource("datalayer/placeholder-category-items.json"));
        assertEquals(expected, result);
    }

    @Test
    public void testCategoryNotFoundPage() throws ClientException {
        String pagePath = VENIA_CONTENT_US_EN_PRODUCTS_CATEGORY_PAGE + ".html/unknown-category.html";
        List<NameValuePair> params = Collections.singletonList(new BasicNameValuePair("wcmmode","disabled"));

        adminAuthor.doGet(pagePath, params, 404);
    }
}
