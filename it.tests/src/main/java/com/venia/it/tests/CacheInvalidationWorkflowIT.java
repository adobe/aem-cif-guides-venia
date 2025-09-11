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
import com.venia.it.category.IgnoreOn65;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertTrue;

/**
 * Integration test for complete Cache Invalidation workflow.
 * Tests the flow: Magento Update → AEM Cache (old data) → Cache Invalidation → AEM Shows New Data
 */
public class CacheInvalidationWorkflowIT extends CommerceTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(CacheInvalidationWorkflowIT.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Magento Configuration - supports environment override
    private static final String MAGENTO_BASE_URL = System.getProperty("cif.magento.url", "https://mcprod.catalogservice-commerce.fun");
    private static final String MAGENTO_REST_URL = MAGENTO_BASE_URL + "/rest/V1";
    private static final String MAGENTO_ADMIN_TOKEN = System.getProperty("cif.magento.token", System.getenv("COMMERCE_INTEGRATION_TOKEN"));

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
        // Validate token configuration
        if (MAGENTO_ADMIN_TOKEN == null || MAGENTO_ADMIN_TOKEN.trim().isEmpty()) {
            throw new RuntimeException(
                    "❌ MISSING MAGENTO TOKEN CONFIGURATION\n" +
                            "Please provide Magento admin token via:\n" +
                            "1. System property: -Dcif.magento.token=your_token_here\n" +
                            "2. Environment variable: COMMERCE_INTEGRATION_TOKEN=your_token_here");
        }

        httpClient = HttpClients.createDefault();
        LOG.info("=== CACHE INVALIDATION WORKFLOW TEST SETUP ===");
        LOG.info("🌍 Magento URL: {}", MAGENTO_BASE_URL);
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
     * ✅ Cache Invalidation Workflow Test - AEM 6.5 ONLY
     * Tests Stretch Belt with Leather Clasp product (VA10) - runs only on AEM 6.5, ignored on Cloud
     */
    @Test
    @Category({ IgnoreOnCloud.class })
    public void testCacheInvalidationWorkflow() throws Exception {
        LOG.info("=== CACHE INVALIDATION WORKFLOW TEST - AEM 6.5 ONLY ===");
        LOG.info("🔄 Testing: Magento Update → Cache → Invalidation → Fresh Data");
        LOG.info("🎯 Product: Stretch Belt with Leather Clasp (VA10) - AEM 6.5 Environment");

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

            // Step 3: Immediately check cache behavior (should show old data)
            LOG.info("📋 STEP 3: Cache validation - checking immediately after Magento update");
            LOG.info("🔍 Checking AEM page - should still show OLD cached data (first check)");
            String firstCacheCheck = getCurrentProductNameFromAEMPage();
            LOG.info("   First check shows: '{}'", firstCacheCheck);
            
            LOG.info("🔍 Second cache check - refreshing again");
            String secondCacheCheck = getCurrentProductNameFromAEMPage();
            LOG.info("   Second check shows: '{}'", secondCacheCheck);
            LOG.info("   Updated Magento: '{}'", newProductName);

            // CRITICAL ASSERTION: AEM should NOT show the updated Magento name (cache should be working)
            // Both checks should show old data, not the new Magento name
            boolean firstCheckCacheWorking = !firstCacheCheck.equals(newProductName) && !firstCacheCheck.contains(timestamp);
            boolean secondCheckCacheWorking = !secondCacheCheck.equals(newProductName) && !secondCacheCheck.contains(timestamp);
            boolean cacheWorking = firstCheckCacheWorking && secondCheckCacheWorking;
            
            LOG.info("   First check cache working (AEM ≠ Magento): {} {}", firstCheckCacheWorking ? "✅" : "❌", firstCheckCacheWorking ? "YES" : "NO");
            LOG.info("   Second check cache working (AEM ≠ Magento): {} {}", secondCheckCacheWorking ? "✅" : "❌", secondCheckCacheWorking ? "YES" : "NO");
            LOG.info("   Overall cache working: {} {}", cacheWorking ? "✅" : "❌", cacheWorking ? "YES" : "NO");
            LOG.info("   Debug: First AEM name '{}' contains timestamp '{}': {}", firstCacheCheck, timestamp, firstCacheCheck.contains(timestamp));
            LOG.info("   Debug: Second AEM name '{}' contains timestamp '{}': {}", secondCacheCheck, timestamp, secondCacheCheck.contains(timestamp));
            
            assertTrue("❌ CACHE NOT WORKING! AEM page should show OLD data, not the updated Magento name. " +
                      "Expected AEM to show cached data, but first check shows: '" + firstCacheCheck + "' " +
                      "and second check shows: '" + secondCacheCheck + "' " +
                      "which should not match updated Magento: '" + newProductName + "'", cacheWorking);

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
                    (name.contains("Stretch") || name.contains("Belt") || name.contains("Leather") || 
                     name.contains("Clasp") || name.contains("Ombre") || name.contains("Infinity") || name.contains("Scarf"))) {
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

    /**
     * ✅ Cache Invalidation Workflow Test - CLOUD ONLY  
     * Tests Ombre Infinity Scarf product (VA03) - runs only on Cloud, ignored on AEM 6.5
     */
    @Test
    @Category({ IgnoreOn65.class })
    public void testCacheInvalidationWorkflowCloudOnly() throws Exception {
        LOG.info("=== CACHE INVALIDATION WORKFLOW TEST - CLOUD ONLY ===");
        LOG.info("🔄 Testing: Magento Update → Cache → Invalidation → Fresh Data");
        LOG.info("☁️ Product: Ombre Infinity Scarf (VA03) - Cloud Environment");

        runCacheInvalidationWorkflow(
            "VA03", // SKU
            "Ombre Infinity Scarf", // Product name
            "/content/venia/us/en/products/product-page.html/venia-accessories/venia-scarves/ombre-infinity-scarf.html" // Product page URL
        );
    }

    /**
     * Common workflow method for cache invalidation testing
     * @param testSku Product SKU to test
     * @param testProductName Expected product name 
     * @param productPageUrl Product page URL in AEM
     */
    private void runCacheInvalidationWorkflow(String testSku, String testProductName, String productPageUrl) throws Exception {
        CloseableHttpClient workflowHttpClient = HttpClients.createDefault();
        String workflowOriginalProductName = null;

        try {
            // Step 1: Get current product name from Magento and AEM
            JsonNode originalProduct = getMagentoProductData(testSku);
            workflowOriginalProductName = originalProduct.get("name").asText();
            String currentAemName = getCurrentProductNameFromAEMPage(productPageUrl);

            LOG.info("📦 STEP 1: Current state");
            LOG.info("   Magento: '{}'", workflowOriginalProductName);
            LOG.info("   AEM Page: '{}'", currentAemName);

            // Step 2: Update Magento product name with timestamp
            String timestamp = String.valueOf(System.currentTimeMillis());
            String updatedProductName = testProductName + " - (random: " + timestamp + ")";
            LOG.info("✏️ Updating Magento product name to: '{}'", updatedProductName);
            updateMagentoProductName(testSku, updatedProductName);
            LOG.info("✅ Magento product name updated successfully");
            LOG.info("🔄 STEP 2: Updated Magento to: '{}'", updatedProductName);

            // Step 3: Immediately check cache behavior (should show old data)
            LOG.info("📋 STEP 3: Cache validation - checking immediately after Magento update");
            LOG.info("🔍 Checking AEM page - should still show OLD cached data (first check)");
            String firstCacheCheck = getCurrentProductNameFromAEMPage(productPageUrl);
            LOG.info("   First check shows: '{}'", firstCacheCheck);
            
            LOG.info("🔍 Second cache check - refreshing again");
            String secondCacheCheck = getCurrentProductNameFromAEMPage(productPageUrl);
            LOG.info("   Second check shows: '{}'", secondCacheCheck);
            LOG.info("   Updated Magento: '{}'", updatedProductName);

            // CRITICAL ASSERTION: AEM should NOT show the updated Magento name (cache should be working)
            // Both checks should show old data, not the new Magento name
            boolean firstCheckCacheWorking = !firstCacheCheck.equals(updatedProductName) && !firstCacheCheck.contains(timestamp);
            boolean secondCheckCacheWorking = !secondCacheCheck.equals(updatedProductName) && !secondCacheCheck.contains(timestamp);
            boolean cacheWorking = firstCheckCacheWorking && secondCheckCacheWorking;
            
            LOG.info("   First check cache working (AEM ≠ Magento): {} {}", firstCheckCacheWorking ? "✅" : "❌", firstCheckCacheWorking ? "YES" : "NO");
            LOG.info("   Second check cache working (AEM ≠ Magento): {} {}", secondCheckCacheWorking ? "✅" : "❌", secondCheckCacheWorking ? "YES" : "NO");
            LOG.info("   Overall cache working: {} {}", cacheWorking ? "✅" : "❌", cacheWorking ? "YES" : "NO");
            LOG.info("   Debug: First AEM name '{}' contains timestamp '{}': {}", firstCacheCheck, timestamp, firstCacheCheck.contains(timestamp));
            LOG.info("   Debug: Second AEM name '{}' contains timestamp '{}': {}", secondCacheCheck, timestamp, secondCacheCheck.contains(timestamp));
            
            assertTrue("❌ CACHE NOT WORKING! AEM page should show OLD data, not the updated Magento name. " +
                      "Expected AEM to show cached data, but first check shows: '" + firstCacheCheck + "' " +
                      "and second check shows: '" + secondCacheCheck + "' " +
                      "which should not match updated Magento: '" + updatedProductName + "'", cacheWorking);

            // Step 4: Call cache invalidation servlet
            LOG.info("🚀 STEP 4: Calling cache invalidation servlet");
            boolean invalidationSuccess = callCacheInvalidationServlet(testSku);
            assertTrue("Cache invalidation servlet should return success", invalidationSuccess);
            LOG.info("✅ Cache invalidation servlet called successfully");

            // Step 5: Wait for cache invalidation to process
            LOG.info("⏳ STEP 5: Waiting for cache invalidation to process...");
            Thread.sleep(3000); // Wait 3 seconds for cache invalidation

            // Step 6: Verify AEM now shows fresh data
            LOG.info("🔍 STEP 6: Fresh data checks (should show new data)");
            String freshCheck1 = getCurrentProductNameFromAEMPage(productPageUrl);
            String freshCheck2 = getCurrentProductNameFromAEMPage(productPageUrl);
            LOG.info("   Check 1: '{}'", freshCheck1);
            LOG.info("   Check 2: '{}'", freshCheck2);

            // Step 7: Verify results
            LOG.info("📊 DETAILED ANALYSIS:");
            LOG.info("   Original AEM name: '{}'", currentAemName);
            LOG.info("   Updated Magento: '{}'", updatedProductName);
            LOG.info("   Fresh check 1: '{}'", freshCheck1);
            LOG.info("   Fresh check 2: '{}'", freshCheck2);
            LOG.info("   Timestamp to find: '{}'", timestamp);
            LOG.info("   Check1 contains timestamp: {}", freshCheck1.contains(timestamp));
            LOG.info("   Check2 contains timestamp: {}", freshCheck2.contains(timestamp));

            boolean freshDataVisible = freshCheck1.contains(timestamp) || freshCheck2.contains(timestamp);
            LOG.info("   Either contains timestamp: {}", freshDataVisible);

            if (freshDataVisible) {
                LOG.info("   Test result: ✅ PASS");
                LOG.info("🎉 SUCCESS: Cache invalidation workflow complete!");
            } else {
                LOG.info("   Test result: ❌ FAIL");

                LOG.error("❌ FAILURE ANALYSIS:");
                LOG.error("   Expected to find timestamp '{}' in AEM page", timestamp);
                LOG.error("   But AEM is still showing: '{}'", freshCheck1);
                LOG.error("   Possible issues:");
                LOG.error("   1. Cache invalidation servlet didn't work properly");
                LOG.error("   2. AEM needs more time to process cache invalidation");
                LOG.error("   3. Product name extraction from page not working correctly");
                LOG.error("   4. Magento-AEM GraphQL connection issues");
                LOG.error("   5. Cache invalidation node not created properly");

                LOG.info("🔄 TRYING ONE MORE TIME after extra wait...");
                Thread.sleep(5000); // Wait 5 more seconds
                String extraCheck = getCurrentProductNameFromAEMPage(productPageUrl);
                LOG.info("   Extra check result: '{}'", extraCheck);
            }

            assertTrue("Cache invalidation should show fresh data. Check logs above for detailed failure analysis.",
                    freshDataVisible);

        } finally {
            // Cleanup: Restore original product name
            if (workflowOriginalProductName != null) {
                try {
                    LOG.info("🔄 Restoring original product name: {}", workflowOriginalProductName);
                    updateMagentoProductName(testSku, workflowOriginalProductName);
                } catch (Exception e) {
                    LOG.warn("Could not restore original product name: {}", e.getMessage());
                }
            }

            if (workflowHttpClient != null) {
                workflowHttpClient.close();
            }
        }
    }

    /**
     * Get current product name from a specific AEM page using breadcrumb extraction
     * @param productPageUrl the product page URL to check
     * @return current product name shown on AEM page
     */
    private String getCurrentProductNameFromAEMPage(String productPageUrl) throws ClientException {
        LOG.debug("🔍 Checking product name in AEM page breadcrumb: {}", productPageUrl);
        try {
            SlingHttpResponse response = adminAuthor.doGet(productPageUrl, 200);
            String pageContent = response.getContent();
            Document doc = Jsoup.parse(pageContent);

            // Try to find active breadcrumb item first
            Elements activeBreadcrumb = doc.select(".cmp-breadcrumb__item--active span[itemprop='name']");
            if (activeBreadcrumb.size() > 0) {
                String name = activeBreadcrumb.first().text();
                if (name != null && !name.trim().isEmpty()) {
                    LOG.debug("   Found name in breadcrumb: '{}'", name);
                    return name.trim();
                }
            }

            // Fallback: look for any breadcrumb item with product-related keywords
            Elements breadcrumbNames = doc.select(".cmp-breadcrumb__item span[itemprop='name']");
            for (Element element : breadcrumbNames) {
                String name = element.text();
                if (name != null && !name.trim().isEmpty() &&
                    (name.contains("Stretch") || name.contains("Belt") || name.contains("Leather") || 
                     name.contains("Clasp") || name.contains("Ombre") || name.contains("Infinity") || name.contains("Scarf"))) {
                    LOG.debug("   Found name in breadcrumb fallback: '{}'", name);
                    return name.trim();
                }
            }

            LOG.warn("Could not find product name in breadcrumb for page {}", productPageUrl);
            return "NOT_FOUND";

        } catch (Exception e) {
            LOG.error("Error checking AEM page breadcrumb: {}", e.getMessage());
            return "ERROR";
        }
    }

    /**
     * Call cache invalidation servlet for a specific product SKU
     * @param testSku the product SKU to invalidate
     * @return true if successful, false otherwise
     */
    private boolean callCacheInvalidationServlet(String testSku) {
        try {
            LOG.info("🚀 Calling cache invalidation servlet...");

            String payload = String.format(
                    "{\n" +
                            "    \"productSkus\": [\"%s\"],\n" +
                            "    \"storePath\": \"%s\"\n" +
                            "}", testSku, STORE_PATH);

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

    /**
     * Update Magento product name via REST API
     * @param sku Product SKU to update
     * @param newName New product name
     */
    private void updateMagentoProductName(String sku, String newName) throws Exception {
        String url = MAGENTO_REST_URL + "/products/" + sku;
        HttpPut request = new HttpPut(url);
        request.setHeader("Authorization", "Bearer " + MAGENTO_ADMIN_TOKEN);
        request.setHeader("Content-Type", "application/json");

        String payload = String.format("{\"product\":{\"name\":\"%s\"}}", newName);
        request.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                String responseBody = EntityUtils.toString(response.getEntity());
                throw new RuntimeException("Failed to update Magento product: " + statusCode + " - " + responseBody);
            }
        }
    }

}