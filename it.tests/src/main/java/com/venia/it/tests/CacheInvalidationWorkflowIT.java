/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2024 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.venia.it.tests;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.venia.it.category.IgnoreOnCloud;
import org.junit.experimental.categories.Category;
import static org.junit.Assert.assertTrue;

/**
 * Integration test for complete Cache Invalidation workflow.
 * Tests the flow: Magento Update → AEM Cache (old data) → Cache Invalidation → AEM Shows New Data
 */
public class CacheInvalidationWorkflowIT extends CommerceTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(CacheInvalidationWorkflowIT.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Magento Configuration
    private static final String MAGENTO_BASE_URL = "https://mcprod.catalogservice-commerce.fun";
    private static final String MAGENTO_REST_URL = MAGENTO_BASE_URL + "/rest/V1";
    private static final String MAGENTO_ADMIN_TOKEN = System.getenv("COMMERCE_INTEGRATION_TOKEN");

    // Test Configuration
    private static final String TEST_PRODUCT_SKU = "VA10"; // Stretch Belt with Leather Clasp
    private static final String TEST_PRODUCT_NAME = "Stretch Belt with Leather Clasp";
    private static final String PRODUCT_PAGE_URL = "/content/venia/us/en/products/product-page.html/venia-accessories/venia-belts/stretch-belt-with-leather-clasp.html";
    private static final String CACHE_INVALIDATION_ENDPOINT = "/bin/cif/invalidate-cache";
    private static final String STORE_PATH = "/content/venia/us/en";

    private CloseableHttpClient httpClient;
    private String originalProductName; // Store for cleanup

    @Before
    public void setUp() throws Exception {
        // Validate environment variable
        if (MAGENTO_ADMIN_TOKEN == null || MAGENTO_ADMIN_TOKEN.trim().isEmpty()) {
            throw new RuntimeException(
                    "❌ MISSING ENVIRONMENT VARIABLE: COMMERCE_INTEGRATION_TOKEN\n" +
                            "Please set the COMMERCE_INTEGRATION_TOKEN environment variable with your Magento admin token.\n" +
                            "Example: set COMMERCE_INTEGRATION_TOKEN=your_token_here");
        }

        httpClient = HttpClients.createDefault();
        LOG.info("=== CACHE INVALIDATION WORKFLOW TEST SETUP ===");
        LOG.info("🎯 Target page: {}", PRODUCT_PAGE_URL);
        LOG.info("📦 Test SKU: {} ({})", TEST_PRODUCT_SKU, TEST_PRODUCT_NAME);
        LOG.info("🔑 Using Magento token: {}***{}",
                MAGENTO_ADMIN_TOKEN.substring(0, Math.min(8, MAGENTO_ADMIN_TOKEN.length())),
                MAGENTO_ADMIN_TOKEN.substring(Math.max(0, MAGENTO_ADMIN_TOKEN.length() - 4)));
    }

    @After
    public void tearDown() throws Exception {
        // Restore original product name if we changed it
        if (originalProductName != null) {
            try {
                LOG.info("🔄 Restoring original product name: {}", originalProductName);
                updateMagentoProductName(originalProductName);
            } catch (Exception e) {
                LOG.warn("Could not restore original product name: {}", e.getMessage());
            }
        }

        if (httpClient != null) {
            httpClient.close();
        }
    }

    /**
     * ✅ Cache Invalidation Workflow Test
     *
     * Tests complete flow: Magento Update → AEM Cache (old data) → Cache Invalidation → Fresh Data
     * This test is designed for AEM 6.5 only, not AEM Cloud.
     */
    @Test
    @Category({ IgnoreOnCloud.class })
    public void testCacheInvalidationWorkflow() throws Exception {
        LOG.info("=== CACHE INVALIDATION WORKFLOW TEST ===");
        LOG.info("🔄 Testing: Magento Update → Cache → Invalidation → Fresh Data");

        try {
            // Step 1: Get current product name from Magento and AEM
            JsonNode originalProduct = getMagentoProductData(TEST_PRODUCT_SKU);
            originalProductName = originalProduct.get("name").asText();
            String currentAemName = getCurrentProductNameFromAEMPage();

            LOG.info("📦 STEP 1: Current state");
            LOG.info("   Magento: '{}'", originalProductName);
            LOG.info("   AEM Page: '{}'", currentAemName);

            // Step 2: Update product name in Magento
            String timestamp = String.valueOf(System.currentTimeMillis());
            String newProductName = TEST_PRODUCT_NAME + " - (random: " + timestamp + ")";
            updateMagentoProductName(newProductName);
            LOG.info("🔄 STEP 2: Updated Magento to: '{}'", newProductName);

            // Step 3: Verify AEM still shows cached data (check immediately to catch cache before auto-refresh)
            LOG.info("📋 STEP 3: AEM cache checks (should show old data) - checking immediately after Magento update");

            long startTime = System.currentTimeMillis();
            Thread.sleep(1000); // Short wait to ensure Magento update is complete
            long check1Time = System.currentTimeMillis();
            String cachedCheck1 = getCurrentProductNameFromAEMPage();
            long check1EndTime = System.currentTimeMillis();

            Thread.sleep(500);   // Very short wait between checks
            long check2Time = System.currentTimeMillis();
            String cachedCheck2 = getCurrentProductNameFromAEMPage();
            long check2EndTime = System.currentTimeMillis();

            LOG.info("📋 STEP 3: AEM cache checks (should show old data)");
            LOG.info("   Check 1 ({}ms after Magento update): '{}'", (check1Time - startTime), cachedCheck1);
            LOG.info("   Check 1 took: {}ms", (check1EndTime - check1Time));
            LOG.info("   Check 2 ({}ms after Magento update): '{}'", (check2Time - startTime), cachedCheck2);
            LOG.info("   Check 2 took: {}ms", (check2EndTime - check2Time));

            // Check if AEM is NOT showing the new Magento value (meaning cache is still showing old data)
            boolean stillCached = !cachedCheck1.contains(timestamp) && !cachedCheck2.contains(timestamp);
            LOG.info("   Still showing cached data: {}", stillCached ? "✅ YES" : "❌ NO");
            LOG.info("   Debug: cachedCheck1 contains timestamp '{}': {}", timestamp, cachedCheck1.contains(timestamp));
            LOG.info("   Debug: cachedCheck2 contains timestamp '{}': {}", timestamp, cachedCheck2.contains(timestamp));

            // Step 4: Call cache invalidation servlet
            LOG.info("🚀 STEP 4: Calling cache invalidation servlet");
            boolean servletSuccess = callCacheInvalidationServlet();
            assertTrue("Cache invalidation servlet should succeed", servletSuccess);
            LOG.info("   ✅ Cache invalidation servlet called successfully");

            // Step 5: Wait and check for fresh data
            LOG.info("⏳ STEP 5: Waiting for cache invalidation to process...");
            Thread.sleep(8000);

            String freshCheck1 = getCurrentProductNameFromAEMPage();
            Thread.sleep(3000);
            String freshCheck2 = getCurrentProductNameFromAEMPage();

            LOG.info("🔍 STEP 6: Fresh data checks (should show new data)");
            LOG.info("   Check 1: '{}'", freshCheck1);
            LOG.info("   Check 2: '{}'", freshCheck2);

            // Verify fresh data is displayed
            boolean showingFreshData = freshCheck1.contains(timestamp) || freshCheck2.contains(timestamp);

            LOG.info("📊 DETAILED ANALYSIS:");
            LOG.info("   Original AEM name: '{}'", currentAemName);
            LOG.info("   Updated Magento:   '{}'", newProductName);
            LOG.info("   Fresh check 1:     '{}'", freshCheck1);
            LOG.info("   Fresh check 2:     '{}'", freshCheck2);
            LOG.info("   Timestamp to find: '{}'", timestamp);
            LOG.info("   Check1 contains timestamp: {}", freshCheck1.contains(timestamp));
            LOG.info("   Check2 contains timestamp: {}", freshCheck2.contains(timestamp));
            LOG.info("   Either contains timestamp: {}", showingFreshData);
            LOG.info("   Test result: {}", showingFreshData ? "✅ PASS" : "❌ FAIL");

            if (!showingFreshData) {
                LOG.error("❌ FAILURE ANALYSIS:");
                LOG.error("   Expected to find timestamp '{}' in AEM page", timestamp);
                LOG.error("   But AEM is still showing: '{}'", freshCheck2);
                LOG.error("   Possible issues:");
                LOG.error("   1. Cache invalidation servlet didn't work properly");
                LOG.error("   2. AEM needs more time to process cache invalidation");
                LOG.error("   3. Product name extraction from page not working correctly");
                LOG.error("   4. Magento-AEM GraphQL connection issues");
                LOG.error("   5. Cache invalidation node not created properly");

                // Try one more time after extra wait
                LOG.info("🔄 TRYING ONE MORE TIME after extra wait...");
                Thread.sleep(10000);
                String extraCheck = getCurrentProductNameFromAEMPage();
                LOG.info("   Extra check result: '{}'", extraCheck);

                if (extraCheck.contains(timestamp)) {
                    LOG.info("✅ SUCCESS on extra check! Cache invalidation worked but needed more time.");
                    showingFreshData = true;
                }
            }

            assertTrue("Cache invalidation should show fresh data. Check logs above for detailed failure analysis.", showingFreshData);
            LOG.info("🎉 SUCCESS: Cache invalidation workflow complete!");

        } catch (Exception e) {
            LOG.error("❌ Test failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    // ===== HELPER METHODS =====

    /**
     * Get current product data from Magento
     */
    private JsonNode getMagentoProductData(String sku) throws Exception {
        String productUrl = MAGENTO_REST_URL + "/products/" + sku;

        HttpGet request = new HttpGet(productUrl);
        request.setHeader("Authorization", "Bearer " + MAGENTO_ADMIN_TOKEN);
        request.setHeader("Content-Type", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String responseContent = EntityUtils.toString(response.getEntity());

            if (response.getStatusLine().getStatusCode() == 200) {
                return OBJECT_MAPPER.readTree(responseContent);
            } else {
                throw new Exception("Failed to get product data. Status: " + response.getStatusLine().getStatusCode());
            }
        }
    }

    /**
     * Update product name in Magento
     */
    private void updateMagentoProductName(String newName) throws Exception {
        LOG.info("✏️ Updating Magento product name to: '{}'", newName);

        String productUrl = MAGENTO_REST_URL + "/products/" + TEST_PRODUCT_SKU;
        String updatePayload = String.format(
                "{\n" +
                        "    \"product\": {\n" +
                        "        \"sku\": \"%s\",\n" +
                        "        \"name\": \"%s\"\n" +
                        "    }\n" +
                        "}", TEST_PRODUCT_SKU, newName);

        HttpPut request = new HttpPut(productUrl);
        request.setHeader("Authorization", "Bearer " + MAGENTO_ADMIN_TOKEN);
        request.setHeader("Content-Type", "application/json");
        request.setEntity(new StringEntity(updatePayload, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getStatusLine().getStatusCode() == 200) {
                LOG.info("✅ Magento product name updated successfully");
            } else {
                throw new Exception("Failed to update product name. Status: " + response.getStatusLine().getStatusCode());
            }
        }

        Thread.sleep(2000); // Wait for update to process
    }

    /**
     * Get current product name from AEM product page breadcrumb
     */
    private String getCurrentProductNameFromAEMPage() throws ClientException {
        LOG.debug("🔍 Checking product name in AEM page breadcrumb: {}", PRODUCT_PAGE_URL);

        try {
            SlingHttpResponse response = adminAuthor.doGet(PRODUCT_PAGE_URL, 200);
            String pageContent = response.getContent();

            // Parse HTML to find product name in breadcrumb
            Document doc = Jsoup.parse(pageContent);

            // Look for active breadcrumb item with product name
            Elements activeBreadcrumb = doc.select(".cmp-breadcrumb__item--active span[itemprop='name']");
            
            if (activeBreadcrumb.size() > 0) {
                String name = activeBreadcrumb.first().text();
                if (name != null && !name.trim().isEmpty()) {
                    LOG.debug("   Found name in breadcrumb: '{}'", name);
                    return name.trim();
                }
            }

            // Fallback: Look for any breadcrumb item span with itemprop="name" 
            Elements breadcrumbNames = doc.select(".cmp-breadcrumb__item span[itemprop='name']");
            for (Element element : breadcrumbNames) {
                String name = element.text();
                if (name != null && !name.trim().isEmpty() && 
                    (name.contains("Stretch") || name.contains("Belt") || name.contains("Leather") || name.contains("Clasp"))) {
                    LOG.debug("   Found name in breadcrumb fallback: '{}'", name);
                    return name.trim();
                }
            }

            LOG.warn("Could not find product name in breadcrumb for {}", TEST_PRODUCT_NAME);
            return "NOT_FOUND";

        } catch (Exception e) {
            LOG.error("Error checking AEM page breadcrumb: {}", e.getMessage());
            return "ERROR";
        }
    }

    /**
     * Call the AEM cache invalidation servlet using environment-specific URL
     */
    private boolean callCacheInvalidationServlet() {
        try {
            LOG.info("🚀 Calling cache invalidation servlet...");

            String payload = String.format(
                    "{\n" +
                            "    \"productSkus\": [\"%s\"],\n" +
                            "    \"storePath\": \"%s\"\n" +
                            "}", TEST_PRODUCT_SKU, STORE_PATH);

            LOG.info("📝 Request details:");
            LOG.info("   Endpoint: {}", CACHE_INVALIDATION_ENDPOINT);
            LOG.info("   Payload: {}", payload);

            // Use adminAuthor client for environment-specific URL handling
            SlingHttpResponse response = adminAuthor.doPost(
                    CACHE_INVALIDATION_ENDPOINT,
                    new StringEntity(payload, ContentType.APPLICATION_JSON),
                    null,
                    200);

            String responseContent = response.getContent();
            int statusCode = response.getStatusLine().getStatusCode();

            LOG.info("📤 Cache invalidation response:");
            LOG.info("   Status: {}", statusCode);
            LOG.info("   Response: {}", responseContent);

            if (statusCode == 200) {
                LOG.info("✅ Cache invalidation servlet returned success");
                return true;
            } else {
                LOG.warn("⚠️ Cache invalidation servlet returned status: {}", statusCode);
                return false;
            }

        } catch (Exception e) {
            LOG.error("❌ Cache invalidation servlet call failed: {}", e.getMessage(), e);
            return false;
        }
    }

}