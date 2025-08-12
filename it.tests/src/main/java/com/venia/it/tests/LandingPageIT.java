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

import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class LandingPageIT extends CommerceTestBase {

    @Test
    public void testLandingPage() throws ClientException {
        SlingHttpResponse response = adminAuthor.doGet(VENIA_CONTENT_US_EN + ".html", 200);
        Document doc = Jsoup.parse(response.getContent());

        // Test hero image
        Elements elements = doc.select(".heroimage");
        assertEquals("Expected 1 hero image element, but found: " + elements.size(), 1, elements.size());

        // Verify breadcrumb: Home
        elements = doc.select(BREADCRUMB_ITEMS_SELECTOR);
        assertEquals("Expected 1 breadcrumb item, but found: " + elements.size(), 1, elements.size());

        // Check the number of root elements in the navigation menu
        elements = doc.select(NAVIGATION_ITEM_SELECTOR);
        assertEquals("Expected 6 navigation items, but found: " + elements.size(), 6, elements.size());
    }
}
