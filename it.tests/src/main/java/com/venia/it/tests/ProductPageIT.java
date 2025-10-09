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

import com.venia.it.category.IgnoreOn65;
import com.venia.it.category.IgnoreOnCloud;
import com.venia.it.category.IgnoreOnLts;
import org.apache.commons.lang3.StringUtils;
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
import org.junit.experimental.categories.Category;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProductPageIT extends CommerceTestBase {

    private static final String PRODUCT_SELECTOR = ".product ";
    private static final String PRODUCT_DETAILS_SELECTOR = PRODUCT_SELECTOR + "> .productFullDetail__root";
    private static final String PRODUCT_NAME_SELECTOR = PRODUCT_SELECTOR + ".productFullDetail__productName > span";
    private static final String GROUPED_PRODUCTS_SELECTOR = PRODUCT_SELECTOR + ".productFullDetail__groupedProducts";

    /**
     * Compare JSON strings ignoring field order and HTML entity encoding differences
     */
    private void assertJsonEqualsIgnoreOrder(String expectedJson, String actualJson, String message) {
        try {
            // Normalize HTML entities in both JSON strings
            String normalizedExpected = normalizeHtmlEntities(expectedJson);
            String normalizedActual = normalizeHtmlEntities(actualJson);
            
            JSONAssert.assertEquals(message, normalizedExpected, normalizedActual, JSONCompareMode.LENIENT);
        } catch (Exception e) {
            // If JSONAssert fails, provide more detailed error message
            throw new AssertionError(message + " - Expected: " + expectedJson + " - Actual: " + actualJson, e);
        }
    }
    
    /**
     * Normalize HTML entities to regular characters for comparison
     */
    private String normalizeHtmlEntities(String jsonString) {
        if (jsonString == null) {
            return null;
        }
        
        // Replace common HTML entities with their regular characters
        return jsonString
            .replace("&#39;", "'")           // apostrophe (numeric)
            .replace("&quot;", "\"")         // double quote (named)
            .replace("&#34;", "\"")          // double quote (numeric)
            .replace("&amp;", "&")           // ampersand (named)
            .replace("&#38;", "&")           // ampersand (numeric)
            .replace("&lt;", "<")            // less than (named)
            .replace("&#60;", "<")           // less than (numeric)
            .replace("&gt;", ">")            // greater than (named)
            .replace("&#62;", ">")           // greater than (numeric)
            .replace("&nbsp;", " ")          // non-breaking space
            .replace("&#160;", " ");         // non-breaking space (numeric)
    }

    @Test
    @Category(IgnoreOn65.class)
    public void testProductPageWithSampleData() throws ClientException, IOException {
        testProductPageWithSampleData("datalayer/simple-product.json");
    }

    @Test
    @Category(IgnoreOnCloud.class)
    public void testProductPageWithSampleData65() throws ClientException, IOException {
        System.out.println("=== ENVIRONMENT DEBUG: Running testProductPageWithSampleData65 ===");
        System.out.println("=== ENVIRONMENT DEBUG: This test has @Category(IgnoreOnCloud.class) ===");
        System.out.println("=== ENVIRONMENT DEBUG: Expected to run on: Classic and LTS (excludes Cloud) ===");
        testProductPageWithSampleData("datalayer/simple-product-65.json");
    }

    public void testProductPageWithSampleData(String jsonFile) throws ClientException, IOException {
        String pagePath = VENIA_CONTENT_US_EN_PRODUCTS_PRODUCT_PAGE + ".html/honora-wide-leg-pants.html";
        SlingHttpResponse response = adminAuthor.doGet(pagePath, 200);
        Document doc = Jsoup.parse(response.getContent());

        // Verify product name
        Elements elements = doc.select(PRODUCT_NAME_SELECTOR);
        assertEquals("Expected product name 'Honora Wide Leg Pants', but found: " + elements.first().html(), "Honora Wide Leg Pants", elements.first().html());

        // Verify that the section for GroupedProduct is NOT displayed
        assertEquals("Expected 0 grouped product sections, but found: " + doc.select(GROUPED_PRODUCTS_SELECTOR).size(), 0, doc.select(GROUPED_PRODUCTS_SELECTOR).size());

        // Verify breadcrumb: Home > Bottoms > Pants & Shorts > Honora Wide Leg Pants
        elements = doc.select(BREADCRUMB_ITEMS_SELECTOR);
        assertEquals("Expected 4 breadcrumb items, but found: " + elements.size(), 4, elements.size());

        // Check the number of root elements in the navigation menu
        elements = doc.select(NAVIGATION_ITEM_SELECTOR);
        assertEquals("Expected 6 navigation items, but found: " + elements.size(), 6, elements.size());

        // Check the meta data
        assertEquals("Expected title 'Honora Wide Leg Pants', but found: " + doc.select("title").first().html(), "Honora Wide Leg Pants", doc.select("title").first().html());
        // when Sites SEO is available and no mapping exists this is only the path
        // when Sites SEO is not available (legacy) the externalizer is used and the canonical link contains the scheme + authority
        assertTrue(StringUtils.endsWith(doc.select("link[rel=canonical]").first().attr("href"), pagePath));

        // Verify dataLayer attributes
        elements = doc.select(PRODUCT_DETAILS_SELECTOR);
        String actualJsonString = elements.first().attr("data-cmp-data-layer");
        String expectedJsonString = Utils.getResource(jsonFile);
        assertJsonEqualsIgnoreOrder(expectedJsonString, actualJsonString, "Expected product datalayer to match sample data, but found differences");
    }

    @Test
    @Category(IgnoreOn65.class )
    public void testProductPageWithSampleDataForGroupedProduct() throws ClientException, IOException {
        testProductPageWithSampleDataForGroupedProduct("datalayer/grouped-product.json");
    }

    @Test
    @Category(IgnoreOnCloud.class)
    public void testProductPageWithSampleDataForGroupedProduct65() throws ClientException, IOException {
        System.out.println("=== ENVIRONMENT DEBUG: Running testProductPageWithSampleDataForGroupedProduct65 ===");
        System.out.println("=== ENVIRONMENT DEBUG: This test has @Category(IgnoreOnCloud.class) ===");
        System.out.println("=== ENVIRONMENT DEBUG: Expected to run on: Classic and LTS (excludes Cloud) ===");
        testProductPageWithSampleDataForGroupedProduct("datalayer/grouped-product-65.json");
    }

    public void testProductPageWithSampleDataForGroupedProduct(String jsonFile) throws ClientException, IOException {
        SlingHttpResponse response = adminAuthor.doGet(VENIA_CONTENT_US_EN_PRODUCTS_PRODUCT_PAGE + ".html/augusta-trio.html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Verify product name
        Elements elements = doc.select(PRODUCT_NAME_SELECTOR);
        assertEquals("Expected product name 'Augusta Trio', but found: " + elements.first().html(), "Augusta Trio", elements.first().html());

        // Verify that the section for GroupedProduct is displayed
        assertEquals("Expected 1 grouped product section, but found: " + doc.select(GROUPED_PRODUCTS_SELECTOR).size(), 1, doc.select(GROUPED_PRODUCTS_SELECTOR).size());

        // Verify dataLayer attributes
        elements = doc.select(PRODUCT_DETAILS_SELECTOR);
        String actualJsonString = elements.first().attr("data-cmp-data-layer");
        String expectedJsonString = Utils.getResource(jsonFile);
        assertJsonEqualsIgnoreOrder(expectedJsonString, actualJsonString, "Expected grouped product datalayer to match sample data, but found differences");
    }

    @Test
    public void testProductPageWithPlaceholderData() throws ClientException, IOException {
        SlingHttpResponse response = adminAuthor.doGet(VENIA_CONTENT_US_EN_PRODUCTS_PRODUCT_PAGE + ".html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Verify product name
        Elements elements = doc.select(PRODUCT_NAME_SELECTOR);
        assertEquals("Expected product name 'Product name', but found: " + elements.first().html(), "Product name", elements.first().html());

        // Verify breadcrumb: Home
        elements = doc.select(BREADCRUMB_ITEMS_SELECTOR);
        assertEquals("Expected 1 breadcrumb item, but found: " + elements.size(), 1, elements.size());

        // Verify dataLayer attributes
        elements = doc.select(PRODUCT_DETAILS_SELECTOR);
        JsonNode result = Utils.OBJECT_MAPPER.readTree(elements.first().attr("data-cmp-data-layer"));
        JsonNode expected = Utils.OBJECT_MAPPER.readTree(Utils.getResource("datalayer/placeholder-product.json"));
        assertEquals("Expected placeholder product datalayer to match data, but found differences", expected, result);
    }

    @Test
    public void testProductNotFoundPage() throws ClientException {
        String pagePath = VENIA_CONTENT_US_EN_PRODUCTS_PRODUCT_PAGE + ".html/unknown-product.html";
        List<NameValuePair> params = Collections.singletonList(new BasicNameValuePair("wcmmode","disabled"));

        adminAuthor.doGet(pagePath, params, 404);
    }
}
