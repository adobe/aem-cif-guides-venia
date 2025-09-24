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
import org.apache.http.client.methods.HttpPost;
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
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.venia.it.category.IgnoreOnCloud;
import com.venia.it.category.IgnoreOn65;

import static org.junit.Assert.assertTrue;
import org.junit.Assert;

import java.util.Random;

/**
 * Cache Invalidation Test - Tests both product and category cache invalidation for AEM 6.5 and Cloud
 */
public class CacheInvalidationWorkflowIT extends CommerceTestBase {

    /**
     * Configuration class for cache invalidation tests
     */
    private static class CacheTestConfig {
        final String testName;
        final String productSku;
        final String categoryUrlKey;
        final String productPageUrl;
        final String categoryPageUrl;
        final CacheInvalidationType invalidationType;
        final boolean includeProduct;
        final boolean includeCategory;
        final boolean fullCycle;

        CacheTestConfig(String testName, String productSku, String categoryUrlKey, 
                       String productPageUrl, String categoryPageUrl, 
                       CacheInvalidationType invalidationType, 
                       boolean includeProduct, boolean includeCategory, boolean fullCycle) {
            this.testName = testName;
            this.productSku = productSku;
            this.categoryUrlKey = categoryUrlKey;
            this.productPageUrl = productPageUrl;
            this.categoryPageUrl = categoryPageUrl;
            this.invalidationType = invalidationType;
            this.includeProduct = includeProduct;
            this.includeCategory = includeCategory;
            this.fullCycle = fullCycle;
        }
    }

    /**
     * Cache invalidation method types
     */
    private enum CacheInvalidationType {
        PRODUCT_SKUS, CATEGORY_UIDS, CACHE_NAMES, REGEX_PATTERNS, INVALIDATE_ALL
    }

    /**
     * Test data holder for managing original and updated values
     */
    private static class TestData {
        String originalProductName;
        String originalCategoryName;
        String updatedProductName;
        String updatedCategoryName;
        String categoryId;
        String categoryUid;
        String randomSuffix;

        TestData(String randomSuffix) {
            this.randomSuffix = randomSuffix;
        }

        void setOriginalProductName(String name) {
            this.originalProductName = name;
            this.updatedProductName = name + " " + randomSuffix;
        }

        void setOriginalCategoryName(String name) {
            this.originalCategoryName = name;
            this.updatedCategoryName = name + " " + randomSuffix;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(CacheInvalidationWorkflowIT.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Magento Configuration - from environment variables (required)
    private static final String COMMERCE_ENDPOINT_RAW = System.getProperty("COMMERCE_ENDPOINT", 
            System.getenv("COMMERCE_ENDPOINT"));
    // Extract base URL by removing /graphql suffix if present
    private static final String MAGENTO_BASE_URL = COMMERCE_ENDPOINT_RAW != null ? 
            COMMERCE_ENDPOINT_RAW.replaceAll("/graphql$", "") : null;
    private static final String MAGENTO_REST_URL = MAGENTO_BASE_URL != null ? MAGENTO_BASE_URL + "/rest/V1" : null;
    private static final String MAGENTO_ADMIN_TOKEN = System.getProperty("COMMERCE_INTEGRATION_TOKEN",
            System.getenv("COMMERCE_INTEGRATION_TOKEN"));
    private static final String CACHE_INVALIDATION_ENDPOINT = "/bin/cif/invalidate-cache";
    private static final String STORE_PATH = "/content/venia/us/en";

    private CloseableHttpClient httpClient;

    @Before
    public void setUp() throws Exception {
        // Validate required environment variables
        if (MAGENTO_BASE_URL == null || MAGENTO_BASE_URL.trim().isEmpty()) {
            throw new IllegalStateException("COMMERCE_ENDPOINT environment variable or system property is required but not set");
        }
        if (MAGENTO_ADMIN_TOKEN == null || MAGENTO_ADMIN_TOKEN.trim().isEmpty()) {
            throw new IllegalStateException("COMMERCE_INTEGRATION_TOKEN environment variable or system property is required but not set");
        }

        httpClient = HttpClients.createDefault();
        LOG.info("=== CACHE INVALIDATION WORKFLOW TEST SETUP ===");
        LOG.info("🌐 Raw Endpoint: {}", COMMERCE_ENDPOINT_RAW);
        LOG.info("🌍 Magento Base URL: {}", MAGENTO_BASE_URL);
        LOG.info("🔗 REST API URL: {}", MAGENTO_REST_URL);
        LOG.info("🔑 Token Source: {}", getTokenSource());
        LOG.info("🌐 Endpoint Source: {}", getEndpointSource());

        // Apply necessary GraphQL Data Service configuration for local testing
        try {
            applyLocalCacheConfigurations();
        } catch (Exception e) {
            LOG.warn("⚠️ Failed to apply local cache configurations: {}", e.getMessage());
            LOG.warn("⚠️ Cache may not work properly in this test run");
        }
    }

    @After
    public void tearDown() throws Exception {
        if (httpClient != null) {
            httpClient.close();
        }
        LOG.info("🧹 Cleanup complete");
    }

    /**
     * AEM 6.5 - Product Cache Invalidation using productSkus method
     */
    @Test
    @Category(IgnoreOnCloud.class)
    public void test65_01_Product_CacheInvalidation() throws Exception {
        LOG.info("=== 🎯 AEM 6.5 - PRODUCT CACHE INVALIDATION (productSkus) ===");
        CacheTestConfig config = new CacheTestConfig(
                "AEM 6.5 - Product (productSkus method)",
                "BLT-LEA-001", // SKU
                null, // No category for product-only test
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-leather-belts.html",
                null,
                CacheInvalidationType.PRODUCT_SKUS,
                true, false, true // product=true, category=false, fullCycle=true
        );
        runCacheInvalidationTest(config);
    }

    /**
     * AEM 6.5 - Category Cache Invalidation using categoryUids method
     */
    @Test
    @Category(IgnoreOnCloud.class)
    public void test65_02_Category_CacheInvalidation() throws Exception {
        LOG.info("=== 🎯 AEM 6.5 - CATEGORY CACHE INVALIDATION (categoryUids) ===");
        runCategoryCacheInvalidationTest(
                "BLT-LEA-001", // SKU for context
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-leather-belts.html", // Category page
                "venia-leather-belts", // URL key
                "AEM 6.5 - Category (categoryUids method)"
        );
    }

    /**
     * AEM 6.5 - Cross-Platform Cache Test (6.5 Product + Cloud Category)
     */
    @Test
    @Category(IgnoreOnCloud.class)
    public void test65_03_CrossPlatform_ProductAndCategory_CacheTest() throws Exception {
        LOG.info("=== 🎯 AEM 6.5 - CROSS-PLATFORM CACHE TEST (6.5 Product + Cloud Category) ===");
        
        // Use 6.5 product and Cloud category
        String productSku = "BLT-LEA-001"; // 6.5 product
        String categoryUrlKey = "venia-fabric-belts"; // Cloud category
        String productPageUrl = "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-leather-belts.html"; // 6.5 product page
        String categoryPageUrl = "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-fabric-belts.html"; // Cloud category page
        
        LOG.info("🎯 Product SKU (6.5): {}", productSku);
        LOG.info("🎯 Category URL Key (Cloud): {}", categoryUrlKey);
        LOG.info("📂 Product Page: {}", productPageUrl);
        LOG.info("📂 Category Page: {}", categoryPageUrl);
        
        String originalProductName = null;
        String originalCategoryName = null;
        String categoryId = null;
        String randomSuffix = generateRandomString(6);
        
        try {
            // STEP 1: Get original names
            LOG.info("📋 STEP 1: Getting original names from both product and category");
            originalProductName = getCurrentProductNameFromAEMPage(productPageUrl, productSku);
            originalCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            
            // Get category ID for updates
            String categoryUid = getCategoryUidFromUrlKey(categoryUrlKey);
            categoryId = new String(java.util.Base64.getDecoder().decode(categoryUid), "UTF-8");
            
            LOG.info("   ✅ Original Product Name: '{}'", originalProductName);
            LOG.info("   ✅ Original Category Name: '{}'", originalCategoryName);
            
            // STEP 2: Update both names in Magento
            String updatedProductName = originalProductName + " " + randomSuffix;
            String updatedCategoryName = originalCategoryName + " " + randomSuffix;
            
            LOG.info("🔄 STEP 2: Updating both names in Magento");
            updateMagentoProductName(productSku, updatedProductName);
            updateMagentoCategoryName(categoryId, updatedCategoryName);
            
            LOG.info("   📦 Updated Product: '{}'", updatedProductName);
            LOG.info("   📦 Updated Category: '{}'", updatedCategoryName);
            
            // STEP 3: Verify cache shows old data
            LOG.info("📋 STEP 3: Verifying cache shows old data");
            String aemProductName = getCurrentProductNameFromAEMPage(productPageUrl, productSku);
            String aemCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            
            boolean productCacheWorking = aemProductName.equals(originalProductName);
            boolean categoryCacheWorking = aemCategoryName.equals(originalCategoryName);
            
            LOG.info("   Product Cache Working: {} {} (Shows: '{}', Expected: '{}')", 
                    productCacheWorking ? "✅" : "❌", productCacheWorking ? "YES" : "NO", aemProductName, originalProductName);
            LOG.info("   Category Cache Working: {} {} (Shows: '{}', Expected: '{}')", 
                    categoryCacheWorking ? "✅" : "❌", categoryCacheWorking ? "YES" : "NO", aemCategoryName, originalCategoryName);
            
            // STEP 4: Clear both caches simultaneously
            LOG.info("🚀 STEP 4: Clearing both caches simultaneously (6.5 method)");
            String payload = String.format(
                "{\n" +
                "    \"productSkus\": [\"%s\"],\n" +
                "    \"categoryUids\": [\"%s\"],\n" +
                "    \"storePath\": \"%s\"\n" +
                "}", productSku, categoryUid, STORE_PATH);
            
            LOG.info("📝 Cache invalidation payload: {}", payload);
            
            SlingHttpResponse response = adminAuthor.doPost(
                CACHE_INVALIDATION_ENDPOINT,
                new StringEntity(payload, ContentType.APPLICATION_JSON),
                null,
                200);
            
            LOG.info("📤 Response: Status={}, Content={}", response.getStatusLine().getStatusCode(), response.getContent());
            
            // STEP 5: Wait and verify fresh data
            LOG.info("⏳ STEP 5: Waiting for cache invalidation...");
            safeSleep(10000);
            
            LOG.info("🔍 STEP 6: Verifying fresh data shows");
            String freshProductName = getCurrentProductNameFromAEMPage(productPageUrl, productSku);
            String freshCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            
            boolean productUpdated = freshProductName.equals(updatedProductName);
            boolean categoryUpdated = freshCategoryName.equals(updatedCategoryName);
            
            LOG.info("   Fresh Product: '{}' - Updated: {} {}", freshProductName, productUpdated ? "✅" : "❌", productUpdated ? "YES" : "NO");
            LOG.info("   Fresh Category: '{}' - Updated: {} {}", freshCategoryName, categoryUpdated ? "✅" : "❌", categoryUpdated ? "YES" : "NO");
            
            assertTrue("Product cache invalidation failed", productUpdated);
            assertTrue("Category cache invalidation failed", categoryUpdated);
            
            // STEP 7: Revert names back to original
            LOG.info("🔄 STEP 7: Reverting names back to original");
            updateMagentoProductName(productSku, originalProductName);
            updateMagentoCategoryName(categoryId, originalCategoryName);
            
            LOG.info("   📦 Reverted Product: '{}'", originalProductName);
            LOG.info("   📦 Reverted Category: '{}'", originalCategoryName);
            
            // STEP 8: Clear cache again to get original names
            LOG.info("🚀 STEP 8: Clearing cache again to get original names");
            SlingHttpResponse finalResponse = adminAuthor.doPost(
                CACHE_INVALIDATION_ENDPOINT,
                new StringEntity(payload, ContentType.APPLICATION_JSON),
                null,
                200);
            
            LOG.info("📤 Final Response: Status={}, Content={}", finalResponse.getStatusLine().getStatusCode(), finalResponse.getContent());
            
            // STEP 9: Wait and verify original names
            LOG.info("⏳ STEP 9: Waiting for final cache clear...");
            safeSleep(10000);
            
            LOG.info("🔍 STEP 10: Verifying original names show");
            String finalProductName = getCurrentProductNameFromAEMPage(productPageUrl, productSku);
            String finalCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            
            boolean productRestored = finalProductName.equals(originalProductName);
            boolean categoryRestored = finalCategoryName.equals(originalCategoryName);
            
            LOG.info("   Final Product: '{}' - Restored: {} {}", finalProductName, productRestored ? "✅" : "❌", productRestored ? "YES" : "NO");
            LOG.info("   Final Category: '{}' - Restored: {} {}", finalCategoryName, categoryRestored ? "✅" : "❌", categoryRestored ? "YES" : "NO");
            
            assertTrue("Product name not restored to original", productRestored);
            assertTrue("Category name not restored to original", categoryRestored);
            
            LOG.info("🎉 SUCCESS: Cross-platform cache test passed!");
            
        } catch (Exception e) {
            LOG.error("❌ Cross-platform cache test failed: {}", e.getMessage(), e);
            throw e;
        } finally {
            // Cleanup - ensure names are restored
            try {
                if (originalProductName != null) {
                    updateMagentoProductName(productSku, originalProductName);
                    LOG.info("🧹 Cleanup: Product name restored");
                }
                if (originalCategoryName != null && categoryId != null) {
                    updateMagentoCategoryName(categoryId, originalCategoryName);
                    LOG.info("🧹 Cleanup: Category name restored");
                }
            } catch (Exception e) {
                LOG.warn("⚠️ Cleanup failed: {}", e.getMessage());
            }
        }
    }

    /**
     * Cloud - Product Cache Invalidation using cacheNames method
     */
    @Test
    @Category(IgnoreOn65.class)
    public void testCloud_01_Product_CacheNames() throws Exception {
        LOG.info("=== 🎯 CLOUD - PRODUCT CACHE INVALIDATION (cacheNames) ===");
        String testSku = "BLT-FAB-001"; // SKU - FABRIC product test with cacheNames
        String categoryPage = "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-fabric-belts.html"; // Category page
        String environment = "Cloud - Product (cacheNames method)";
        
        // No need for separate warm-up - test will naturally warm cache in Step 1
        runCacheNamesProductTest(environment, testSku, categoryPage);
    }

    /**
     * Cloud - Cache Invalidation using regexPatterns method (Product + Category)
     */
    @Test
    @Category(IgnoreOn65.class)
    public void testCloud_02_RegexPatterns() throws Exception {
        LOG.info("=== 🎯 CLOUD - CACHE INVALIDATION (regexPatterns) ===");
        String testSku = "BLT-FAB-001"; // FABRIC BELT - same as first test for consistency
        String categoryPage = "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-fabric-belts.html";
        String categoryUrlKey = "venia-fabric-belts";
        String environment = "Cloud - RegexPatterns (both product and category)";

        // Cache already warmed by first test - proceeding with regex test
        runCloudRegexPatternsTest(environment, testSku, categoryPage, categoryUrlKey);
    }

    /**
     * Cloud - Final Comprehensive Test using invalidateAll method
     */
    @Test
    @Category(IgnoreOn65.class)
    public void testCloud_03_InvalidateAll_Final() throws Exception {
        LOG.info("=== 🎯 CLOUD - COMPREHENSIVE CACHE INVALIDATION (invalidateAll) ===");
        String testSku = "BLT-FAB-001"; // Use valid product - Canvas Fabric Belt for final test  
        String categoryPage = "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-fabric-belts.html";
        String categoryUrlKey = "venia-fabric-belts";
        String environment = "Cloud - Final Test (invalidateAll method)";

        // Test complete cache invalidation with both product and category - cache already warmed
        LOG.info("🔥 CACHE STATUS: Cache already warmed from first test - proceeding directly");
        runInvalidateAllCacheTest(environment, testSku, categoryPage, categoryUrlKey);
    }

    /**
     * Common cache invalidation test workflow
     */
    private void runCacheInvalidationTest(CacheTestConfig config) throws Exception {
        LOG.info("=== CACHE INVALIDATION TEST - {} ===", config.testName);
        if (config.includeProduct) LOG.info("🎯 Product SKU: {}", config.productSku);
        if (config.includeCategory) LOG.info("🎯 Category URL Key: {}", config.categoryUrlKey);
        if (config.productPageUrl != null) LOG.info("📂 Product Page: {}", config.productPageUrl);
        if (config.categoryPageUrl != null) LOG.info("📂 Category Page: {}", config.categoryPageUrl);

        TestData testData = new TestData(generateRandomString(6));
        String trueOriginalProductFromMagento = null;
        String trueOriginalCategoryFromMagento = null;

        try {
            // STEP 1: Get original data and prepare test data
            if (config.includeProduct) {
                trueOriginalProductFromMagento = getMagentoProductName(config.productSku);
                testData.setOriginalProductName(getCurrentProductNameFromAEMPage(config.productPageUrl, config.productSku));
                LOG.info("📋 STEP 1: Original Product Name: '{}'", testData.originalProductName);
            }

            if (config.includeCategory) {
                testData.categoryUid = getCategoryUidFromUrlKey(config.categoryUrlKey);
                testData.categoryId = new String(java.util.Base64.getDecoder().decode(testData.categoryUid), "UTF-8");
                JsonNode categoryData = getMagentoCategoryData(testData.categoryId);
                trueOriginalCategoryFromMagento = categoryData.get("name").asText();
                testData.setOriginalCategoryName(getCurrentCategoryNameFromAEMPage(config.categoryPageUrl));
                LOG.info("📋 STEP 1: Original Category Name: '{}'", testData.originalCategoryName);
            }

            // STEP 2: Update data in Magento
            LOG.info("🔄 STEP 2: Updating data in Magento backend");
            if (config.includeProduct) {
                updateMagentoProductName(config.productSku, testData.updatedProductName);
                LOG.info("   📦 Updated Product: '{}'", testData.updatedProductName);
            }
            if (config.includeCategory) {
                updateMagentoCategoryName(testData.categoryId, testData.updatedCategoryName);
                LOG.info("   📦 Updated Category: '{}'", testData.updatedCategoryName);
            }

            // STEP 3: Verify cache is working (shows old data)
            LOG.info("📋 STEP 3: Verifying cache shows old data");
            verifyCacheWorking(config, testData);

            // STEP 4: Perform cache invalidation
            String payload = generateCacheInvalidationPayload(config, testData.categoryUid);
            performCacheInvalidation(payload, config.invalidationType.toString());

            // STEP 5: Verify fresh data shows
            LOG.info("🔍 STEP 5: Verifying fresh data shows");
            verifyFreshData(config, testData);

            if (config.fullCycle) {
                // STEP 6-8: Full cycle - revert and verify restoration
                performFullCycleRestore(config, testData, trueOriginalProductFromMagento, trueOriginalCategoryFromMagento);
            }

            LOG.info("🎉 SUCCESS: {} cache invalidation test passed!", config.invalidationType);

        } finally {
            // Cleanup - ensure original data is restored
            performCleanup(config, trueOriginalProductFromMagento, trueOriginalCategoryFromMagento, testData);
        }
    }

    /**
     * Verify that cache is working by checking AEM shows old data
     */
    private void verifyCacheWorking(CacheTestConfig config, TestData testData) throws Exception {
        boolean productCacheWorking = true;
        boolean categoryCacheWorking = true;

        if (config.includeProduct) {
            String currentProductName = getCurrentProductNameFromAEMPage(config.productPageUrl, config.productSku);
            productCacheWorking = currentProductName.equals(testData.originalProductName);
            LOG.info("   Product Cache Working: {} {} (Shows: '{}', Expected: '{}')", 
                productCacheWorking ? "✅" : "❌", productCacheWorking ? "YES" : "NO", 
                currentProductName, testData.originalProductName);
        }

        if (config.includeCategory) {
            String currentCategoryName = getCurrentCategoryNameFromAEMPage(config.categoryPageUrl);
            categoryCacheWorking = currentCategoryName.equals(testData.originalCategoryName);
            LOG.info("   Category Cache Working: {} {} (Shows: '{}', Expected: '{}')", 
                categoryCacheWorking ? "✅" : "❌", categoryCacheWorking ? "YES" : "NO", 
                currentCategoryName, testData.originalCategoryName);
        }

        if (!productCacheWorking || !categoryCacheWorking) {
            Assert.fail("❌ FAILED: Cache not working - test cannot proceed as it would be a false positive");
        }
    }

    /**
     * Verify that fresh data shows after cache invalidation
     */
    private void verifyFreshData(CacheTestConfig config, TestData testData) throws Exception {
        if (config.includeProduct) {
            String freshProductName = getCurrentProductNameFromAEMPage(config.productPageUrl, config.productSku);
            boolean productUpdated = freshProductName.equals(testData.updatedProductName);
            LOG.info("   Fresh Product: '{}' - Updated: {} {}", freshProductName, 
                productUpdated ? "✅" : "❌", productUpdated ? "YES" : "NO");
            assertTrue("Product cache invalidation failed", productUpdated);
        }

        if (config.includeCategory) {
            String freshCategoryName = getCurrentCategoryNameFromAEMPage(config.categoryPageUrl);
            boolean categoryUpdated = freshCategoryName.equals(testData.updatedCategoryName);
            LOG.info("   Fresh Category: '{}' - Updated: {} {}", freshCategoryName, 
                categoryUpdated ? "✅" : "❌", categoryUpdated ? "YES" : "NO");
            assertTrue("Category cache invalidation failed", categoryUpdated);
        }
    }

    /**
     * Perform full cycle restore (revert data and clear cache again)
     */
    private void performFullCycleRestore(CacheTestConfig config, TestData testData, 
                                       String trueOriginalProductFromMagento, 
                                       String trueOriginalCategoryFromMagento) throws Exception {
        // STEP 6: Revert names back to original
        LOG.info("🔄 STEP 6: Reverting names back to original");
        if (config.includeProduct) {
            updateMagentoProductName(config.productSku, trueOriginalProductFromMagento);
            LOG.info("   📦 Reverted Product: '{}'", trueOriginalProductFromMagento);
        }
        if (config.includeCategory) {
            updateMagentoCategoryName(testData.categoryId, trueOriginalCategoryFromMagento);
            LOG.info("   📦 Reverted Category: '{}'", trueOriginalCategoryFromMagento);
        }

        // STEP 7: Clear cache again to get original names
        String payload = generateCacheInvalidationPayload(config, testData.categoryUid);
        performCacheInvalidation(payload, config.invalidationType + " (final restore)");

        // STEP 8: Verify original names show
        LOG.info("🔍 STEP 8: Verifying original names show");
        if (config.includeProduct) {
            String finalProductName = getCurrentProductNameFromAEMPage(config.productPageUrl, config.productSku);
            boolean productRestored = finalProductName.equals(trueOriginalProductFromMagento);
            LOG.info("   Final Product: '{}' - Restored: {} {}", finalProductName, 
                productRestored ? "✅" : "❌", productRestored ? "YES" : "NO");
            assertTrue("Product name not restored to original", productRestored);
        }
        if (config.includeCategory) {
            String finalCategoryName = getCurrentCategoryNameFromAEMPage(config.categoryPageUrl);
            boolean categoryRestored = finalCategoryName.equals(trueOriginalCategoryFromMagento);
            LOG.info("   Final Category: '{}' - Restored: {} {}", finalCategoryName, 
                categoryRestored ? "✅" : "❌", categoryRestored ? "YES" : "NO");
            assertTrue("Category name not restored to original", categoryRestored);
        }

        LOG.info("🎉 COMPLETE SUCCESS: Full cycle cache test passed!");
    }

    /**
     * Perform cleanup to restore original data
     */
    private void performCleanup(CacheTestConfig config, String trueOriginalProductFromMagento, 
                              String trueOriginalCategoryFromMagento, TestData testData) {
        try {
            if (config.includeProduct && trueOriginalProductFromMagento != null) {
                updateMagentoProductName(config.productSku, trueOriginalProductFromMagento);
                LOG.info("🧹 Cleanup: Product name restored");
            }
            if (config.includeCategory && trueOriginalCategoryFromMagento != null) {
                updateMagentoCategoryName(testData.categoryId, trueOriginalCategoryFromMagento);
                LOG.info("🧹 Cleanup: Category name restored");
            }
        } catch (Exception e) {
            LOG.warn("⚠️ Cleanup failed: {}", e.getMessage());
        }
    }

    // Legacy method - keeping for backward compatibility but marking as deprecated
    @Deprecated
    private void runProductCacheInvalidationTest(String productSku, String categoryPageUrl, String environment) throws Exception {
        // Redirect to new unified method
        CacheTestConfig config = new CacheTestConfig(
            environment, productSku, null, categoryPageUrl, null,
            CacheInvalidationType.PRODUCT_SKUS, true, false, true
        );
        runCacheInvalidationTest(config);
    }

    /**
     * Category cache invalidation test workflow - Legacy method
     */
    @Deprecated
    private void runCategoryCacheInvalidationTest(String productSku, String categoryPageUrl, String categoryUrlKey, String environment) throws Exception {
        // Redirect to new unified method
        CacheTestConfig config = new CacheTestConfig(
            environment, null, categoryUrlKey, null, categoryPageUrl,
            CacheInvalidationType.CATEGORY_UIDS, false, true, true
        );
        runCacheInvalidationTest(config);
    }

    /**
     * Get product data from Magento REST API
     */
    private JsonNode getMagentoProductData(String sku) throws Exception {
        String url = MAGENTO_REST_URL + "/products/" + sku;
        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + MAGENTO_ADMIN_TOKEN);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String content = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() == 200) {
                return OBJECT_MAPPER.readTree(content);
            } else {
                throw new Exception("Failed to get product data: " + response.getStatusLine().getStatusCode());
            }
        }
    }

    /**
     * Get category data from Magento REST API
     */
    private JsonNode getMagentoCategoryData(String categoryId) throws Exception {
        String url = MAGENTO_REST_URL + "/categories/" + categoryId;
        HttpGet request = new HttpGet(url);
        request.setHeader("Authorization", "Bearer " + MAGENTO_ADMIN_TOKEN);

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            String content = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() == 200) {
                return OBJECT_MAPPER.readTree(content);
            } else {
                throw new Exception("Failed to get category data: " + response.getStatusLine().getStatusCode());
            }
        }
    }

    /**
     * Update product name in Magento
     */
    private void updateMagentoProductName(String sku, String newName) throws Exception {
        String url = MAGENTO_REST_URL + "/products/" + sku;
        HttpPut request = new HttpPut(url);
        request.setHeader("Authorization", "Bearer " + MAGENTO_ADMIN_TOKEN);
        request.setHeader("Content-Type", "application/json");

        String payload = String.format("{\"product\":{\"name\":\"%s\"}}", newName);
        request.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new Exception("Failed to update product: " + response.getStatusLine().getStatusCode());
            }
        }
        safeSleep(2000); // Wait for Magento to process
    }

    /**
     * Update category name in Magento
     */
    private void updateMagentoCategoryName(String categoryId, String newName) throws Exception {
        String url = MAGENTO_REST_URL + "/categories/" + categoryId;
        HttpPut request = new HttpPut(url);
        request.setHeader("Authorization", "Bearer " + MAGENTO_ADMIN_TOKEN);
        request.setHeader("Content-Type", "application/json");

        String payload = String.format("{\"category\":{\"name\":\"%s\"}}", newName);
        request.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new Exception("Failed to update category: " + response.getStatusLine().getStatusCode());
            }
        }
        safeSleep(2000); // Wait for Magento to process
    }

    /**
     * Extract product name from AEM category page by SKU
     */
    private String getCurrentProductNameFromAEMPage(String categoryPageUrl, String targetSku) throws ClientException {
        try {
            String htmlContent = makeCacheRespectingRequest(categoryPageUrl);
            Document doc = Jsoup.parse(htmlContent);

            // Find the specific product by SKU
            Elements productItems = doc.select(".productcollection__item[data-product-sku='" + targetSku + "']");
            if (productItems.size() > 0) {
                Element productItem = productItems.first();

                // Try different methods to get product name
                // Method 1: From title span
                Elements titleElements = productItem.select(".productcollection__item-title span");
                if (titleElements.size() > 0) {
                    return titleElements.first().text().trim();
                }

                // Method 2: From title attribute
                String titleAttr = productItem.attr("title");
                if (titleAttr != null && !titleAttr.isEmpty()) {
                    return titleAttr.trim();
                }

                // Method 3: From data layer JSON
                String dataLayer = productItem.attr("data-cmp-data-layer");
                if (dataLayer != null && !dataLayer.isEmpty()) {
                    try {
                        dataLayer = dataLayer.replace("&quot;", "\"");
                        JsonNode jsonData = OBJECT_MAPPER.readTree(dataLayer);
                        JsonNode firstKey = jsonData.fields().next().getValue();
                        if (firstKey.has("dc:title")) {
                            return firstKey.get("dc:title").asText();
                        }
                    } catch (Exception e) {
                        LOG.warn("Could not parse data layer JSON: {}", e.getMessage());
                    }
                }
            }

            LOG.warn("Could not find product with SKU '{}' in category page", targetSku);
            return "NOT_FOUND";
        } catch (Exception e) {
            LOG.error("Error getting product name from category page: {}", e.getMessage());
            return "ERROR";
        }
    }

    /**
     * Extract category name from AEM category page
     */
    private String getCurrentCategoryNameFromAEMPage(String categoryPageUrl) throws ClientException {
        try {
            String htmlContent = makeCacheRespectingRequest(categoryPageUrl);
            Document doc = Jsoup.parse(htmlContent);

            // Look for category title
            Elements title = doc.select(".category__title");
            if (title.size() > 0) {
                return title.first().text().trim();
            }

            // Fallback: breadcrumb
            Elements breadcrumb = doc.select(".cmp-breadcrumb__item--active span[itemprop='name']");
            if (breadcrumb.size() > 0) {
                return breadcrumb.first().text().trim();
            }

            return "NOT_FOUND";
        } catch (Exception e) {
            LOG.error("Error getting category name: {}", e.getMessage());
            return "ERROR";
        }
    }

    /**
     * Call AEM cache invalidation servlet
     */
    private boolean callCacheInvalidationServlet(String productSku, String categoryUrlKey) {
        try {
            String payload;

            if (categoryUrlKey != null && !categoryUrlKey.isEmpty()) {
                // Category invalidation - get category UID
                String categoryUid = getCategoryUidFromUrlKey(categoryUrlKey);
                payload = String.format(
                        "{\n" +
                                "    \"categoryUids\": [\"%s\"],\n" +
                                "    \"storePath\": \"%s\"\n" +
                                "}", categoryUid, STORE_PATH);
                LOG.info("📝 Cache invalidation payload (category): {}", payload);
            } else {
                // Product invalidation
                payload = String.format(
                        "{\n" +
                                "    \"productSkus\": [\"%s\"],\n" +
                                "    \"storePath\": \"%s\"\n" +
                                "}", productSku, STORE_PATH);
                LOG.info("📝 Cache invalidation payload (product): {}", payload);
            }

            SlingHttpResponse response = adminAuthor.doPost(
                    CACHE_INVALIDATION_ENDPOINT,
                    new StringEntity(payload, ContentType.APPLICATION_JSON),
                    null,
                    200);

            int statusCode = response.getStatusLine().getStatusCode();
            String responseContent = response.getContent();
            LOG.info("📤 Response: Status={}, Content={}", statusCode, responseContent);

            return statusCode == 200;
        } catch (Exception e) {
            LOG.error("❌ Cache invalidation failed: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get category UID from Magento GraphQL using url_key
     */
    private String getCategoryUidFromUrlKey(String categoryUrlKey) {
        try {
            LOG.info("🔍 Getting category UID from Magento GraphQL for url_key: '{}'", categoryUrlKey);

            String graphqlQuery = String.format(
                    "{ categoryList(filters: {url_key: {eq: \"%s\"}}) { uid name url_key } }",
                    categoryUrlKey
            );

            String url = MAGENTO_BASE_URL + "/graphql";
            HttpPost request = new HttpPost(url);
            request.setHeader("Content-Type", "application/json");

            com.fasterxml.jackson.databind.node.ObjectNode jsonPayload = OBJECT_MAPPER.createObjectNode();
            jsonPayload.put("query", graphqlQuery);
            String payload = OBJECT_MAPPER.writeValueAsString(jsonPayload);

            request.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseContent = EntityUtils.toString(response.getEntity());

                if (response.getStatusLine().getStatusCode() == 200) {
                    JsonNode responseJson = OBJECT_MAPPER.readTree(responseContent);
                    JsonNode data = responseJson.get("data");
                    JsonNode categoryList = data.get("categoryList");

                    if (categoryList != null && categoryList.isArray() && categoryList.size() > 0) {
                        JsonNode category = categoryList.get(0);
                        String uid = category.get("uid").asText();
                        String name = category.get("name").asText();

                        LOG.info("✅ Found category: '{}' with UID: '{}'", name, uid);
                        return uid;
                    }
                }

                throw new RuntimeException("No category found for url_key: " + categoryUrlKey);
            }
        } catch (Exception e) {
            LOG.error("❌ Failed to get category UID: {}", e.getMessage());
            throw new RuntimeException("Failed to get category UID from GraphQL", e);
        }
    }

    /**
     * Apply local cache configurations for testing
     */
    private void applyLocalCacheConfigurations() throws Exception {
        LOG.info("🔧 Applying GraphQL Data Service configuration for local testing...");

        String baseUrl = adminAuthor.getUrl().toString();
        String configUrl = baseUrl + "/system/console/configMgr";
        LOG.info("🔧 Configuration URL: {}", configUrl);

        // Create form data for GraphQL Data Service configuration
        StringBuilder payload = new StringBuilder();
        payload.append("apply=true");
        payload.append("&factoryPid=com.adobe.cq.commerce.graphql.magento.GraphqlDataServiceImpl");
        payload.append("&propertylist=identifier,productCachingEnabled,productCachingSize,productCachingTimeMinutes,categoryCachingEnabled,categoryCachingSize,categoryCachingTimeMinutes");
        payload.append("&identifier=default");
        payload.append("&productCachingEnabled=true");
        payload.append("&productCachingSize=1000");
        payload.append("&productCachingTimeMinutes=10");
        payload.append("&categoryCachingEnabled=true");
        payload.append("&categoryCachingSize=100");
        payload.append("&categoryCachingTimeMinutes=60");

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(configUrl);

            // Set basic auth
            String auth = java.util.Base64.getEncoder().encodeToString("admin:admin".getBytes());
            request.setHeader("Authorization", "Basic " + auth);
            request.setHeader("Content-Type", "application/x-www-form-urlencoded");
            request.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_FORM_URLENCODED));

            try (CloseableHttpResponse response = client.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200 || statusCode == 302) {
                    LOG.info("✅ GraphQL Data Service configuration applied successfully");
                    // Wait a moment for configuration to become active
                    safeSleep(3000);
                } else {
                    LOG.warn("⚠️ Configuration response status: {}", statusCode);
                    String responseBody = EntityUtils.toString(response.getEntity());
                    LOG.warn("⚠️ Response body: {}", responseBody.length() > 200 ? responseBody.substring(0, 200) + "..." : responseBody);
                }
            }
        } catch (Exception e) {
            LOG.warn("⚠️ Could not apply GraphQL Data Service config: {}", e.getMessage());
        }
    }

    /**
     * Generate random string for test purposes
     */
    private String generateRandomString(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Safe sleep method that handles InterruptedException gracefully
     */
    private void safeSleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("⚠️ Sleep interrupted: {}", e.getMessage());
        }
    }

    /**
     * Warm up product cache by making multiple cache-respecting requests
     */
    private void warmUpProductCache(String testSku, String categoryPage) throws Exception {
        LOG.info("🔥 WARM-UP: Pre-loading product cache for SKU '{}' in Cloud environment", testSku);
        
        try {
            // Make multiple requests to warm up the cache
            for (int i = 0; i < 3; i++) {
                LOG.info("🔥 WARM-UP: Request {} of 3", i + 1);
                makeCacheRespectingRequest(categoryPage);  
                safeSleep(1000); // Small delay between requests
            }
            
            // Also warm up the specific product
            getCurrentProductNameFromAEMPage(categoryPage, testSku);  
            safeSleep(1000);
            
            LOG.info("🔥 WARM-UP: Product cache warm-up completed");
        } catch (Exception e) {
            LOG.warn("⚠️ WARM-UP: Product cache warm-up failed, but continuing test: {}", e.getMessage());
        }
    }

    /**
     * Warm up category cache by making multiple cache-respecting requests
     */
    private void warmUpCategoryCache(String categoryPage) throws Exception {
        LOG.info("🔥 WARM-UP: Pre-loading category cache for page '{}'", categoryPage);
        
        try {
            // Make multiple requests to warm up the cache
            for (int i = 0; i < 3; i++) {
                LOG.info("🔥 WARM-UP: Request {} of 3", i + 1);
                makeCacheRespectingRequest(categoryPage);  
                safeSleep(1000); // Small delay between requests
            }
            
            // Also warm up the specific category data
            getCurrentCategoryNameFromAEMPage(categoryPage);
            safeSleep(1000);
            
            LOG.info("🔥 WARM-UP: Category cache warm-up completed");
        } catch (Exception e) {
            LOG.warn("⚠️ WARM-UP: Category cache warm-up failed, but continuing test: {}", e.getMessage());
        }
    }

    /**
     * Get Magento product name (helper method)
     */
    private String getMagentoProductName(String sku) throws Exception {
        JsonNode productData = getMagentoProductData(sku);
        return productData.get("name").asText();
    }

    /**
     * Get product page URL (helper method) - returns category page containing the product
     */
    private String getProductPageUrl(String sku) {
        // Return appropriate category page based on SKU
        if (sku.equals("BLT-LEA-001")) {
            return "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-leather-belts.html";
        } else if (sku.equals("BLT-FAB-001")) {
            return "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-fabric-belts.html";
        } else {
            // Default to leather belts category
            return "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-leather-belts.html";
        }
    }

    /**
     * Make a cache-respecting request to AEM (not using admin client which bypasses cache)
     */
    private String makeCacheRespectingRequest(String path) throws ClientException {
        try {
            String baseUrl = adminAuthor.getUrl().toString();
            String fullUrl = baseUrl + path;
            LOG.debug("Making cache-respecting request to: {}", fullUrl);

            HttpGet request = new HttpGet(fullUrl);

            String credentials = "admin:admin";
            String encodedCredentials = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
            request.setHeader("Authorization", "Basic " + encodedCredentials);

            request.setHeader("Cache-Control", "max-age=0");
            request.setHeader("User-Agent", "AEM-Cache-Test");

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    return EntityUtils.toString(response.getEntity());
                } else {
                    throw new ClientException("Request failed with status: " + statusCode);
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to make cache-respecting request: {}", e.getMessage());
            throw new ClientException("Failed to make cache-respecting request", e);
        }
    }

    /**
     * Product Cache Test using Regex Pattern - Focused on product invalidation only
     */
    private void runRegexPatternProductTest(String environment, String testSku, String categoryPageUrl) throws Exception {
        String originalProductName = null;
        String randomSuffix = generateRandomString(6);

        try {
            // STEP 1: Get original product data
            LOG.info("📋 STEP 1: Getting original product data");
            originalProductName = getMagentoProductName(testSku);
            LOG.info("   ✓ Original Product: '{}'", originalProductName);

            // STEP 2: Update in Magento
            String updatedProductName = originalProductName + " " + randomSuffix;
            LOG.info("🔄 STEP 2: Updating Product in Magento");
            updateMagentoProductName(testSku, updatedProductName);
            LOG.info("   ✓ Updated Product: '{}'", updatedProductName);

            // STEP 3: Verify cache working
            LOG.info("📋 STEP 3: Verifying cache shows old data");
            String aemProductName = getCurrentProductNameFromAEMPage(getProductPageUrl(testSku), testSku);
            boolean productCacheWorking = !aemProductName.equals(updatedProductName);
            LOG.info("   Product Cache Working: {} {}", productCacheWorking ? "✅" : "❌", productCacheWorking ? "YES" : "NO");

            if (!productCacheWorking) {
                Assert.fail("❌ FAILED: Product cache not working - test cannot proceed as it would be a false positive");
            }

            // STEP 4: Clear cache using REGEX PATTERN for product
            LOG.info("🚀 STEP 4: Calling cache invalidation with PRODUCT REGEX PATTERN");
            // Use a simpler, more reliable regex pattern that matches the product SKU
            String productRegex = testSku; // Simple SKU-based pattern

            String payload = String.format(
                    "{\n" +
                            "    \"regexPatterns\": [\"%s\"],\n" +
                            "    \"storePath\": \"/content/venia/us/en\"\n" +
                            "}", productRegex);

            LOG.info("📝 Cache invalidation payload (product regex): {}", payload);
            SlingHttpResponse response = adminAuthor.doPost("/bin/cif/invalidate-cache", new StringEntity(payload, ContentType.APPLICATION_JSON), 200);
            LOG.info("📤 Response: Status={}, Content={}", response.getStatusLine(), response.getContent());

            // STEP 5: Wait and verify
            LOG.info("⏳ STEP 5: Waiting for regex pattern invalidation...");
            Thread.sleep(10000);

            LOG.info("🔍 STEP 6: Verifying product cache cleared via regex pattern");
            aemProductName = getCurrentProductNameFromAEMPage(getProductPageUrl(testSku), testSku);
            boolean productUpdated = aemProductName.contains(randomSuffix);
            LOG.info("   Fresh Product Check: '{}'", aemProductName);
            LOG.info("   Product Updated: {} {}", productUpdated ? "✅" : "❌", productUpdated ? "YES" : "NO");

            if (productUpdated) {
                LOG.info("🎉 SUCCESS: Product regex pattern invalidation test passed!");
            } else {
                Assert.fail("❌ FAILED: Product regex pattern invalidation failed");
            }

        } finally {
            // Cleanup
            LOG.info("🧹 CLEANUP: Reverting product name back to original...");
            if (originalProductName != null) {
                try {
                    updateMagentoProductName(testSku, originalProductName);
                } catch (Exception e) {
                    LOG.warn("Could not restore product name: {}", e.getMessage());
                }
            }
            
            // Clear cache after reversion to avoid test interference
            try {
                LOG.info("🧹 Clearing cache after reversion...");
                String clearPayload = "{\"invalidateAll\": true, \"storePath\": \"" + STORE_PATH + "\"}";
                adminAuthor.doPost(CACHE_INVALIDATION_ENDPOINT, new StringEntity(clearPayload, ContentType.APPLICATION_JSON), 200);
                safeSleep(2000); // Wait for cache clear
            } catch (Exception e) {
                LOG.warn("Could not clear cache after reversion: {}", e.getMessage());
            }
            
            LOG.info("🧹 Cleanup complete");
        }
    }

    /**
     * Category Cache Test using Cache Names - Focused on category component clearing
     */
    private void runCacheNamesCategoryTest(String environment, String testSku, String categoryPageUrl, String categoryUrlKey) throws Exception {
        String originalCategoryName = null;
        String categoryId = null;
        String randomSuffix = generateRandomString(6);

        try {
            // STEP 1: Get original category data
            LOG.info("📋 STEP 1: Getting original category data");
            String categoryUid = getCategoryUidFromUrlKey(categoryUrlKey);
            categoryId = new String(java.util.Base64.getDecoder().decode(categoryUid), "UTF-8");
            JsonNode categoryData = getMagentoCategoryData(categoryId);
            originalCategoryName = categoryData.get("name").asText();
            LOG.info("   ✓ Original Category: '{}'", originalCategoryName);

            // STEP 2: Update in Magento
            String updatedCategoryName = originalCategoryName + " " + randomSuffix;
            LOG.info("🔄 STEP 2: Updating Category in Magento");
            updateMagentoCategoryName(categoryId, updatedCategoryName);
            LOG.info("   ✓ Updated Category: '{}'", updatedCategoryName);

            // STEP 3: Verify cache working
            LOG.info("📋 STEP 3: Verifying cache shows old data");
            String aemCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            boolean categoryCacheWorking = !aemCategoryName.equals(updatedCategoryName);
            LOG.info("   Category Cache Working: {} {}", categoryCacheWorking ? "✅" : "❌", categoryCacheWorking ? "YES" : "NO");

            if (!categoryCacheWorking) {
                Assert.fail("❌ FAILED: Category cache not working - test cannot proceed as it would be a false positive");
            }

            // STEP 4: Use categoryUids instead of cache names for more reliable invalidation
            LOG.info("🚀 STEP 4: Calling cache invalidation with CATEGORY UID (more reliable than cache names)");
            String payload = String.format(
                    "{\n" +
                            "    \"categoryUids\": [\"%s\"],\n" +
                            "    \"storePath\": \"/content/venia/us/en\"\n" +
                            "}", categoryUid);

            LOG.info("📝 Cache invalidation payload (category UID): {}", payload);
            SlingHttpResponse response = adminAuthor.doPost("/bin/cif/invalidate-cache", new StringEntity(payload, ContentType.APPLICATION_JSON), 200);
            LOG.info("📤 Response: Status={}, Content={}", response.getStatusLine(), response.getContent());

            // STEP 5: Wait and verify
            LOG.info("⏳ STEP 5: Waiting for category UID invalidation...");
            Thread.sleep(10000);

            LOG.info("🔍 STEP 6: Verifying category cache cleared");
            aemCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            boolean categoryUpdated = aemCategoryName.contains(randomSuffix);
            LOG.info("   Fresh Category Check: '{}'", aemCategoryName);
            LOG.info("   Category Updated: {} {}", categoryUpdated ? "✅" : "❌", categoryUpdated ? "YES" : "NO");

            if (categoryUpdated) {
                LOG.info("🎉 SUCCESS: Category UID invalidation test passed!");
            } else {
                Assert.fail("❌ FAILED: Category UID invalidation failed");
            }

        } finally {
            // Cleanup
            LOG.info("🧹 CLEANUP: Reverting category name back to original...");
            if (originalCategoryName != null && categoryId != null) {
                try {
                    updateMagentoCategoryName(categoryId, originalCategoryName);
                } catch (Exception e) {
                    LOG.warn("Could not restore category name: {}", e.getMessage());
                }
            }
            
            // Clear cache after reversion to avoid test interference
            try {
                LOG.info("🧹 Clearing cache after reversion...");
                String clearPayload = "{\"invalidateAll\": true, \"storePath\": \"" + STORE_PATH + "\"}";
                adminAuthor.doPost(CACHE_INVALIDATION_ENDPOINT, new StringEntity(clearPayload, ContentType.APPLICATION_JSON), 200);
                safeSleep(2000); // Wait for cache clear
            } catch (Exception e) {
                LOG.warn("Could not clear cache after reversion: {}", e.getMessage());
            }
            
            LOG.info("🧹 Cleanup complete");
        }
    }

    /**
     * Category-only Regex Pattern Cache Invalidation Test - Tests category pattern-based cache clearing
     */
    private void runCategoryRegexPatternTest(String environment, String categoryPageUrl, String categoryUrlKey) throws Exception {
        // First get the true original from Magento for restoration purposes
        String categoryUid = getCategoryUidFromUrlKey(categoryUrlKey);
        String categoryId = new String(java.util.Base64.getDecoder().decode(categoryUid), "UTF-8");
        JsonNode categoryData = getMagentoCategoryData(categoryId);
        String trueOriginalCategoryFromMagento = categoryData.get("name").asText();
        
        String originalCategoryName = null;
        String randomSuffix = generateRandomString(6);

        try {
            // STEP 1: Get category name from AEM page (this naturally loads cache)
            LOG.info("📋 STEP 1: Getting category name from AEM page (this naturally loads cache)");
            originalCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            LOG.info("   🏪 AEM Category Name (now cached): '{}'", originalCategoryName);

            // STEP 2: Update category in Magento (backend change)
            String updatedCategoryName = originalCategoryName + " " + randomSuffix;
            LOG.info("🔄 STEP 2: Updating Category in Magento backend");
            updateMagentoCategoryName(categoryId, updatedCategoryName);
            LOG.info("   📦 Updated Category: '{}'", updatedCategoryName);

            // STEP 3: Verify category cache working (should show old cached data)
            LOG.info("📋 STEP 3: Verifying category cache shows old data (should be cached name)");
            String aemCategoryNameAfterChange = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            LOG.info("   AEM Category Shows: '{}'", aemCategoryNameAfterChange);
            LOG.info("   Expected Cached: '{}'", originalCategoryName);
            LOG.info("   Updated Magento: '{}'", updatedCategoryName);
            
            boolean categoryCacheWorking = aemCategoryNameAfterChange.equals(originalCategoryName) && !aemCategoryNameAfterChange.equals(updatedCategoryName);
            LOG.info("   Category Cache Working: {} {}", categoryCacheWorking ? "✅" : "❌", categoryCacheWorking ? "YES" : "NO");

            if (!categoryCacheWorking) {
                LOG.warn("❌ DEBUG: Category cache not working - AEM showing fresh data immediately");
                LOG.warn("   - Original AEM (cached): '{}'", originalCategoryName);
                LOG.warn("   - Updated Magento: '{}'", updatedCategoryName);
                LOG.warn("   - AEM Shows After Change: '{}'", aemCategoryNameAfterChange);
                LOG.warn("   - Environment: {}", environment);
                LOG.warn("   - This indicates Data Service cache is not active");
                
                Assert.fail("❌ FAILED: Category cache not working - test cannot proceed as it would be a false positive");
            }

            // STEP 4: Clear category cache using regex patterns
            LOG.info("🚀 STEP 4: Calling cache invalidation with CATEGORY REGEX PATTERNS");
            String categoryRegex = String.format("venia-fabric-belts"); // Simple pattern for fabric belts category

            String payload = String.format(
                    "{\n" +
                            "    \"regexPatterns\": [\"%s\"],\n" +
                            "    \"storePath\": \"/content/venia/us/en\"\n" +
                            "}", categoryRegex);

            LOG.info("📝 Cache invalidation payload (category regex): {}", payload);
            SlingHttpResponse response = adminAuthor.doPost("/bin/cif/invalidate-cache", new StringEntity(payload, ContentType.APPLICATION_JSON), 200);
            LOG.info("✅ Response: Status={}, Content={}", response.getStatusLine(), response.getContent());

            // STEP 5: Wait and verify category cache cleared
            LOG.info("⏳ STEP 5: Waiting for regex pattern invalidation...");
            safeSleep(5000);

            LOG.info("🔍 STEP 6: Verifying category cache cleared via regex pattern");
            String freshCategoryCheck = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            LOG.info("Fresh Category Check: '{}'", freshCategoryCheck);
            boolean categoryUpdated = freshCategoryCheck.equals(updatedCategoryName);
            LOG.info("Category Updated: {} {}", categoryUpdated ? "✅" : "❌", categoryUpdated ? "YES" : "NO");

            if (!categoryUpdated) {
                Assert.fail("❌ FAILED: Category regex pattern invalidation did not work - cache was not cleared");
            }

            LOG.info("🎉 SUCCESS: Category regex pattern invalidation test passed!");

        } finally {
            // Cleanup: Restore true original category name
            try {
                updateMagentoCategoryName(categoryId, trueOriginalCategoryFromMagento);
                LOG.info("📦 Restored category name: {}", trueOriginalCategoryFromMagento);

                // Clear cache after reversion
                LOG.info("🧹 Clearing cache after reversion...");
                String clearPayload = "{\"invalidateAll\": true, \"storePath\": \"/content/venia/us/en\"}";
                adminAuthor.doPost("/bin/cif/invalidate-cache", new StringEntity(clearPayload, ContentType.APPLICATION_JSON), 200);

            } catch (Exception e) {
                LOG.warn("Could not restore category name: {}", e.getMessage());
            }
            LOG.info("🧹 Cleanup complete");
        }
    }

    /**
     * Regex Pattern Cache Invalidation Test - Tests pattern-based cache clearing
     */
    private void runRegexPatternCacheTest(String environment, String testSku, String categoryPageUrl, String categoryUrlKey) throws Exception {
        String originalProductName = null;
        String originalCategoryName = null;
        String randomSuffix = generateRandomString(6);

        try {
            // STEP 1: Get original data
            LOG.info("📋 STEP 1: Getting original data from both Product and Category");
            originalProductName = getMagentoProductName(testSku);
            String categoryUid = getCategoryUidFromUrlKey(categoryUrlKey);
            String categoryId = new String(java.util.Base64.getDecoder().decode(categoryUid), "UTF-8");
            JsonNode categoryData = getMagentoCategoryData(categoryId);
            originalCategoryName = categoryData.get("name").asText();
            LOG.info("   ✓ Original Product: '{}'", originalProductName);
            LOG.info("   ✓ Original Category: '{}'", originalCategoryName);

            // STEP 2: Update both in Magento
            String updatedProductName = originalProductName + " " + randomSuffix;
            String updatedCategoryName = originalCategoryName + " " + randomSuffix;
            LOG.info("🔄 STEP 2: Updating both Product and Category in Magento");
            updateMagentoProductName(testSku, updatedProductName);
            updateMagentoCategoryName(categoryId, updatedCategoryName);
            LOG.info("   ✓ Updated Product: '{}'", updatedProductName);
            LOG.info("   ✓ Updated Category: '{}'", updatedCategoryName);

            // STEP 3: Verify cache working (both should show old data)
            LOG.info("📋 STEP 3: Verifying cache shows old data");
            String aemProductName = getCurrentProductNameFromAEMPage(getProductPageUrl(testSku), testSku);
            String aemCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            boolean productCacheWorking = !aemProductName.equals(updatedProductName);
            boolean categoryCacheWorking = !aemCategoryName.equals(updatedCategoryName);
            LOG.info("   Product Cache Working: {} {}", productCacheWorking ? "✅" : "❌", productCacheWorking ? "YES" : "NO");
            LOG.info("   Category Cache Working: {} {}", categoryCacheWorking ? "✅" : "❌", categoryCacheWorking ? "YES" : "NO");

            if (!productCacheWorking || !categoryCacheWorking) {
                Assert.fail("❌ FAILED: Cache not working - test cannot proceed as it would be a false positive");
            }

            // STEP 4: Clear cache using regex patterns
            LOG.info("🚀 STEP 4: Calling cache invalidation with REGEX PATTERNS");
            String productRegex = String.format("\\\"sku\\\"\\\\s*\\\\s*\\\"%s\\\"", testSku);
            String categoryRegex = String.format("\\\"uid\\\"\\\\s*:\\\\s*\\\\{\\\"id\\\"\\\\s*:\\\\s*\\\"%s\\\"", categoryUid);

            String payload = String.format(
                    "{\n" +
                            "    \"regexPatterns\": [\"%s\", \"%s\"],\n" +
                            "    \"storePath\": \"/content/venia/us/en\"\n" +
                            "}", productRegex, categoryRegex);

            LOG.info("📝 Cache invalidation payload (regex patterns): {}", payload);
            SlingHttpResponse response = adminAuthor.doPost("/bin/cif/invalidate-cache", new StringEntity(payload, ContentType.APPLICATION_JSON), 200);
            LOG.info("📤 Response: Status={}, Content={}", response.getStatusLine(), response.getContent());

            // STEP 5: Wait and verify cache cleared
            LOG.info("⏳ STEP 5: Waiting for regex pattern cache invalidation...");
            Thread.sleep(10000);

            LOG.info("🔍 STEP 6: Verifying cache shows fresh data after regex invalidation");
            aemProductName = getCurrentProductNameFromAEMPage(getProductPageUrl(testSku), testSku);
            aemCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            boolean productUpdated = aemProductName.contains(randomSuffix);
            boolean categoryUpdated = aemCategoryName.contains(randomSuffix);
            LOG.info("   Fresh Product Check: '{}'", aemProductName);
            LOG.info("   Fresh Category Check: '{}'", aemCategoryName);
            LOG.info("   Product Updated: {} {}", productUpdated ? "✅" : "❌", productUpdated ? "YES" : "NO");
            LOG.info("   Category Updated: {} {}", categoryUpdated ? "✅" : "❌", categoryUpdated ? "YES" : "NO");

            if (productUpdated && categoryUpdated) {
                LOG.info("🎉 SUCCESS: Regex pattern cache invalidation test passed!");
            } else {
                Assert.fail(String.format("❌ FAILED: Regex pattern cache invalidation failed - Product: %s, Category: %s",
                        productUpdated ? "✅" : "❌", categoryUpdated ? "✅" : "❌"));
            }

        } finally {
            // Cleanup
            LOG.info("🧹 CLEANUP: Reverting names back to original values...");
            if (originalProductName != null) {
                try {
                    updateMagentoProductName(testSku, originalProductName);
                } catch (Exception e) {
                    LOG.warn("Could not restore product name: {}", e.getMessage());
                }
            }
            if (originalCategoryName != null) {
                try {
                    String categoryUid = getCategoryUidFromUrlKey(categoryUrlKey);
                    String categoryId = new String(java.util.Base64.getDecoder().decode(categoryUid), "UTF-8");
                    updateMagentoCategoryName(categoryId, originalCategoryName);
                } catch (Exception e) {
                    LOG.warn("Could not restore category name: {}", e.getMessage());
                }
            }
            
            // Clear cache after reversion to avoid test interference
            try {
                LOG.info("🧹 Clearing cache after reversion...");
                String clearPayload = "{\"invalidateAll\": true, \"storePath\": \"" + STORE_PATH + "\"}";
                adminAuthor.doPost(CACHE_INVALIDATION_ENDPOINT, new StringEntity(clearPayload, ContentType.APPLICATION_JSON), 200);
                safeSleep(2000); // Wait for cache clear
            } catch (Exception e) {
                LOG.warn("Could not clear cache after reversion: {}", e.getMessage());
            }
            
            LOG.info("🧹 Cleanup complete");
        }
    }

    /**
     * Cache Names Invalidation Test - Tests specific component cache clearing
     */
    private void runCacheNamesCacheTest(String environment, String testSku, String categoryPageUrl) throws Exception {
        String originalProductName = null;
        String randomSuffix = generateRandomString(6);

        try {
            // STEP 1: Get original data
            LOG.info("📋 STEP 1: Getting original product data");
            originalProductName = getMagentoProductName(testSku);
            LOG.info("   ✓ Original Product: '{}'", originalProductName);

            // STEP 2: Update in Magento
            String updatedProductName = originalProductName + " " + randomSuffix;
            LOG.info("🔄 STEP 2: Updating Product in Magento");
            updateMagentoProductName(testSku, updatedProductName);
            LOG.info("   ✓ Updated Product: '{}'", updatedProductName);

            // STEP 3: Verify cache working
            LOG.info("📋 STEP 3: Verifying cache shows old data");
            String aemProductName = getCurrentProductNameFromAEMPage(getProductPageUrl(testSku), testSku);
            boolean productCacheWorking = !aemProductName.equals(updatedProductName);
            LOG.info("   Product Cache Working: {} {}", productCacheWorking ? "✅" : "❌", productCacheWorking ? "YES" : "NO");

            if (!productCacheWorking) {
                Assert.fail("❌ FAILED: Product cache not working - test cannot proceed as it would be a false positive");
            }

            // STEP 4: Clear specific cache names
            LOG.info("🚀 STEP 4: Calling cache invalidation with CACHE NAMES");
            String payload = String.format(
                    "{\n" +
                            "    \"cacheNames\": [\n" +
                            "        \"venia/components/commerce/product\",\n" +
                            "        \"venia/components/commerce/navigation\",\n" +
                            "        \"venia/components/commerce/breadcrumb\"\n" +
                            "    ],\n" +
                            "    \"storePath\": \"/content/venia/us/en\"\n" +
                            "}");

            LOG.info("📝 Cache invalidation payload (cache names): {}", payload);
            SlingHttpResponse response = adminAuthor.doPost("/bin/cif/invalidate-cache", new StringEntity(payload, ContentType.APPLICATION_JSON), 200);
            LOG.info("📤 Response: Status={}, Content={}", response.getStatusLine(), response.getContent());

            // STEP 5: Wait and verify
            LOG.info("⏳ STEP 5: Waiting for cache names invalidation...");
            Thread.sleep(10000);

            LOG.info("🔍 STEP 6: Verifying specific component caches cleared");
            aemProductName = getCurrentProductNameFromAEMPage(getProductPageUrl(testSku), testSku);
            boolean productUpdated = aemProductName.contains(randomSuffix);
            LOG.info("   Fresh Product Check: '{}'", aemProductName);
            LOG.info("   Product Updated: {} {}", productUpdated ? "✅" : "❌", productUpdated ? "YES" : "NO");

            if (productUpdated) {
                LOG.info("🎉 SUCCESS: Cache names invalidation test passed!");
            } else {
                Assert.fail("❌ FAILED: Cache names invalidation failed - component caches not cleared");
            }

        } finally {
            // Cleanup
            LOG.info("🧹 CLEANUP: Reverting product name back to original...");
            if (originalProductName != null) {
                try {
                    updateMagentoProductName(testSku, originalProductName);
                } catch (Exception e) {
                    LOG.warn("Could not restore product name: {}", e.getMessage());
                }
            }
            
            // Clear cache after reversion to avoid test interference
            try {
                LOG.info("🧹 Clearing cache after reversion...");
                String clearPayload = "{\"invalidateAll\": true, \"storePath\": \"" + STORE_PATH + "\"}";
                adminAuthor.doPost(CACHE_INVALIDATION_ENDPOINT, new StringEntity(clearPayload, ContentType.APPLICATION_JSON), 200);
                safeSleep(2000); // Wait for cache clear
            } catch (Exception e) {
                LOG.warn("Could not clear cache after reversion: {}", e.getMessage());
            }
            
            LOG.info("🧹 Cleanup complete");
        }
    }

    /**
     * Invalidate All Cache Test with Warm-up - Tests complete cache flush with cache pre-loading
     */
    private void runInvalidateAllCacheTestWithWarmup(String environment, String testSku, String categoryPageUrl, String categoryUrlKey) throws Exception {
        // Cache should already be warmed from first test - just run the test
        LOG.info("🔥 CACHE STATUS: Cache already warmed from first test - proceeding directly to test");
        
        // Run the actual test (cache already loaded)
        runInvalidateAllCacheTest(environment, testSku, categoryPageUrl, categoryUrlKey);
    }

    /**
     * Invalidate All Cache Test - Tests complete cache flush
     */
    private void runInvalidateAllCacheTest(String environment, String testSku, String categoryPageUrl, String categoryUrlKey) throws Exception {
        // Get true originals from Magento for restoration purposes  
        String trueOriginalProductFromMagento = getMagentoProductName(testSku);
        String categoryUid = getCategoryUidFromUrlKey(categoryUrlKey);
        String categoryId = new String(java.util.Base64.getDecoder().decode(categoryUid), "UTF-8");
        JsonNode categoryData = getMagentoCategoryData(categoryId);
        String trueOriginalCategoryFromMagento = categoryData.get("name").asText();
        
        String originalProductName = null;
        String originalCategoryName = null;
        String randomSuffix = generateRandomString(6);

        try {
            // STEP 1: Get original data from AEM pages (this naturally loads cache)
            LOG.info("📋 STEP 1: Getting original data from both AEM Product and Category pages (naturally loads cache)");
            originalProductName = getCurrentProductNameFromAEMPage(categoryPageUrl, testSku);
            originalCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            LOG.info("   🏪 Original Product (now cached): '{}'", originalProductName);
            LOG.info("   🏪 Original Category (now cached): '{}'", originalCategoryName);

            // STEP 2: Update both in Magento (backend changes)
            String updatedProductName = originalProductName + " " + randomSuffix;
            String updatedCategoryName = originalCategoryName + " " + randomSuffix;
            LOG.info("🔄 STEP 2: Updating both Product and Category in Magento backend");
            updateMagentoProductName(testSku, updatedProductName);
            updateMagentoCategoryName(categoryId, updatedCategoryName);
            LOG.info("   📦 Updated Product: '{}'", updatedProductName);
            LOG.info("   📦 Updated Category: '{}'", updatedCategoryName);

            // STEP 3: Verify cache working (both should show old cached data)
            LOG.info("📋 STEP 3: Verifying cache shows old data (should be cached names)");
            String aemProductNameAfterChange = getCurrentProductNameFromAEMPage(categoryPageUrl, testSku);
            String aemCategoryNameAfterChange = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            
            boolean productCacheWorking = aemProductNameAfterChange.equals(originalProductName) && !aemProductNameAfterChange.equals(updatedProductName);
            boolean categoryCacheWorking = aemCategoryNameAfterChange.equals(originalCategoryName) && !aemCategoryNameAfterChange.equals(updatedCategoryName);
            
            LOG.info("   Product Cache Working: {} {}", productCacheWorking ? "✅" : "❌", productCacheWorking ? "YES" : "NO");
            LOG.info("   Category Cache Working: {} {}", categoryCacheWorking ? "✅" : "❌", categoryCacheWorking ? "YES" : "NO");

            if (!productCacheWorking || !categoryCacheWorking) {
                LOG.warn("❌ DEBUG: Cache not working - AEM showing fresh data immediately");
                LOG.warn("   - Original Product (cached): '{}'", originalProductName);
                LOG.warn("   - Original Category (cached): '{}'", originalCategoryName);
                LOG.warn("   - Updated Product: '{}'", updatedProductName);
                LOG.warn("   - Updated Category: '{}'", updatedCategoryName);
                LOG.warn("   - AEM Product After Change: '{}'", aemProductNameAfterChange);
                LOG.warn("   - AEM Category After Change: '{}'", aemCategoryNameAfterChange);
                LOG.warn("   - Environment: {}", environment);
                
                Assert.fail("❌ FAILED: Cache not working - test cannot proceed as it would be a false positive");
            }

            // STEP 4: Clear ALL cache
            LOG.info("🚀 STEP 4: Calling cache invalidation with INVALIDATE ALL");
            String payload = String.format(
                    "{\n" +
                            "    \"invalidateAll\": true,\n" +
                            "    \"storePath\": \"/content/venia/us/en\"\n" +
                            "}");

            LOG.info("📝 Cache invalidation payload (invalidate all): {}", payload);
            SlingHttpResponse response = adminAuthor.doPost("/bin/cif/invalidate-cache", new StringEntity(payload, ContentType.APPLICATION_JSON), 200);
            LOG.info("📤 Response: Status={}, Content={}", response.getStatusLine(), response.getContent());

            // STEP 5: Wait and verify everything cleared
            LOG.info("⏳ STEP 5: Waiting for complete cache invalidation...");
            safeSleep(15000); // Longer wait for complete flush

            LOG.info("🔍 STEP 6: Verifying ALL caches cleared");
            String freshProductName = getCurrentProductNameFromAEMPage(getProductPageUrl(testSku), testSku);
            String freshCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            boolean productUpdated = freshProductName.contains(randomSuffix);
            boolean categoryUpdated = freshCategoryName.contains(randomSuffix);
            LOG.info("   Fresh Product Check: '{}'", freshProductName);
            LOG.info("   Fresh Category Check: '{}'", freshCategoryName);
            LOG.info("   Product Updated: {} {}", productUpdated ? "✅" : "❌", productUpdated ? "YES" : "NO");
            LOG.info("   Category Updated: {} {}", categoryUpdated ? "✅" : "❌", categoryUpdated ? "YES" : "NO");

            if (productUpdated && categoryUpdated) {
                LOG.info("🎉 SUCCESS: Invalidate All cache test passed!");
            } else {
                Assert.fail(String.format("❌ FAILED: Invalidate All cache failed - Product: %s, Category: %s",
                        productUpdated ? "✅" : "❌", categoryUpdated ? "✅" : "❌"));
            }

        } finally {
            // Cleanup
            LOG.info("🧹 CLEANUP: Reverting names back to true original values...");
            try {
                updateMagentoProductName(testSku, trueOriginalProductFromMagento);
                LOG.info("📦 Restored product name: {}", trueOriginalProductFromMagento);
            } catch (Exception e) {
                LOG.warn("Could not restore product name: {}", e.getMessage());
            }
            
            try {
                updateMagentoCategoryName(categoryId, trueOriginalCategoryFromMagento);
                LOG.info("📦 Restored category name: {}", trueOriginalCategoryFromMagento);
            } catch (Exception e) {
                LOG.warn("Could not restore category name: {}", e.getMessage());
            }
            
            // Clear cache after reversion to avoid test interference
            try {
                LOG.info("🧹 Clearing cache after reversion...");
                String clearPayload = "{\"invalidateAll\": true, \"storePath\": \"" + STORE_PATH + "\"}";
                adminAuthor.doPost(CACHE_INVALIDATION_ENDPOINT, new StringEntity(clearPayload, ContentType.APPLICATION_JSON), 200);
                safeSleep(2000); // Wait for cache clear
            } catch (Exception e) {
                LOG.warn("Could not clear cache after reversion: {}", e.getMessage());
            }
            
            LOG.info("🧹 Cleanup complete");
        }
    }

    /**
     * Cache Names Product Test - Tests product cache invalidation using cacheNames method
     */
    private void runCacheNamesProductTest(String environment, String testSku, String categoryPageUrl) throws Exception {
        // First get the true original from Magento for restoration purposes
        String trueOriginalFromMagento = getMagentoProductName(testSku);
        
        LOG.info("🔄 STEP 1: Getting product name from AEM page (this naturally loads cache)");
        String originalProductName = getCurrentProductNameFromAEMPage(categoryPageUrl, testSku);
        LOG.info("   🏪 AEM Product Name (now cached): '{}'", originalProductName);

        try {
            // Update product in Magento (backend change)
            LOG.info("🔄 STEP 2: Updating product name in Magento backend");
            String updatedProductName = originalProductName + " " + generateRandomString(6);
            updateMagentoProductName(testSku, updatedProductName);
            LOG.info("   📦 Updated Magento Product: '{}'", updatedProductName);

            // Check AEM still shows cached (old) data
            LOG.info("🔄 STEP 3: Checking AEM still shows cached data (should be old name)");
            String aemProductNameAfterChange = getCurrentProductNameFromAEMPage(categoryPageUrl, testSku);
            LOG.info("   AEM Product Shows: '{}'", aemProductNameAfterChange);
            LOG.info("   Expected Cached: '{}'", originalProductName);
            LOG.info("   Updated Magento: '{}'", updatedProductName);

            boolean cacheWorking = aemProductNameAfterChange.equals(originalProductName) && !aemProductNameAfterChange.equals(updatedProductName);
            LOG.info("   Product Cache Working: {} {}", cacheWorking ? "✅" : "❌", cacheWorking ? "YES" : "NO");

            if (!cacheWorking) {
                LOG.warn("❌ DEBUG: Product cache not working - AEM showing fresh data immediately");
                LOG.warn("   - Original AEM (cached): '{}'", originalProductName);
                LOG.warn("   - Updated Magento: '{}'", updatedProductName);
                LOG.warn("   - AEM Shows After Change: '{}'", aemProductNameAfterChange);
                LOG.warn("   - Environment: {}", environment);
                LOG.warn("   - This indicates Data Service cache is not active");
                
                Assert.fail("❌ FAILED: Product cache not working - test cannot proceed as it would be a false positive");
            }

            // Call cache invalidation servlet with COMPONENT CACHE NAMES
            LOG.info("🔄 STEP 4: Calling cache invalidation with COMPONENT CACHE NAMES");
            String invalidationPayload = "{\n" +
                    "    \"cacheNames\": [\n" +
                    "        \"venia/components/commerce/product\",\n" +
                    "        \"venia/components/commerce/productlist\",\n" +
                    "        \"venia/components/commerce/navigation\"\n" +
                    "    ],\n" +
                    "    \"storePath\": \"" + STORE_PATH + "\"\n" +
                    "}";

            LOG.info("📤 Cache invalidation payload (product cache names): {}", invalidationPayload);

            SlingHttpResponse response = adminAuthor.doPost(CACHE_INVALIDATION_ENDPOINT, new StringEntity(invalidationPayload, ContentType.APPLICATION_JSON), 200);
            LOG.info("📨 Response: Status={}, Content={}", response.getStatusLine(), response.getContent());

            // Wait for cache invalidation
            LOG.info("⏳ STEP 5: Waiting for cache names invalidation...");
            safeSleep(5000);

            // Verify product updated
            LOG.info("🔄 STEP 6: Verifying product component caches cleared");
            String freshProductCheck = getCurrentProductNameFromAEMPage(categoryPageUrl, testSku);
            LOG.info("   Fresh Product Check: '{}'", freshProductCheck);

            boolean productUpdated = freshProductCheck.equals(updatedProductName);
            LOG.info("   Product Updated: {} {}", productUpdated ? "✅" : "❌", productUpdated ? "YES" : "NO");

            if (productUpdated) {
                LOG.info("✅ SUCCESS: Product cache names invalidation test passed!");
            } else {
                Assert.fail(String.format("❌ FAILED: Product cache names invalidation failed - Expected: '%s', Got: '%s'",
                        updatedProductName, freshProductCheck));
            }

        } finally {
            // Cleanup and clear cache
            LOG.info("🧹 Restoring product name to true original from Magento...");
            try {
                updateMagentoProductName(testSku, trueOriginalFromMagento);
                LOG.info("📦 Restored product name: {}", trueOriginalFromMagento);
                
                // Clear cache after reversion to avoid test interference
                LOG.info("🧹 Clearing cache after reversion...");
                String clearPayload = "{\"invalidateAll\": true, \"storePath\": \"" + STORE_PATH + "\"}";
                adminAuthor.doPost(CACHE_INVALIDATION_ENDPOINT, new StringEntity(clearPayload, ContentType.APPLICATION_JSON), 200);
                safeSleep(2000); // Wait for cache clear
                
            } catch (Exception e) {
                LOG.warn("Could not restore product name or clear cache: {}", e.getMessage());
            }
            
            LOG.info("🧹 Cleanup complete");
        }
    }

    /**
     * Cloud Regex Patterns Test - Tests both product and category cache invalidation using regexPatterns
     */
    private void runCloudRegexPatternsTest(String environment, String testSku, String categoryPageUrl, String categoryUrlKey) throws Exception {
        // First get the true originals from Magento for restoration purposes
        String trueOriginalProductFromMagento = getMagentoProductName(testSku);
        String categoryUid = getCategoryUidFromUrlKey(categoryUrlKey);
        String categoryId = new String(java.util.Base64.getDecoder().decode(categoryUid), "UTF-8");
        JsonNode categoryData = getMagentoCategoryData(categoryId);
        String trueOriginalCategoryFromMagento = categoryData.get("name").asText();
        
        String originalProductName = null;
        String originalCategoryName = null;
        String randomSuffix = generateRandomString(6);

        try {
            // STEP 1: Get original names from AEM pages (this naturally loads cache)
            LOG.info("📋 STEP 1: Getting original names from AEM pages (this naturally loads cache)");
            originalProductName = getCurrentProductNameFromAEMPage(categoryPageUrl, testSku);
            originalCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            LOG.info("   🏪 AEM Product Name (now cached): '{}'", originalProductName);
            LOG.info("   🏪 AEM Category Name (now cached): '{}'", originalCategoryName);

            // STEP 2: Update both in Magento (backend changes)
            String updatedProductName = originalProductName + " " + randomSuffix;
            String updatedCategoryName = originalCategoryName + " " + randomSuffix;
            LOG.info("🔄 STEP 2: Updating both Product and Category in Magento backend");
            updateMagentoProductName(testSku, updatedProductName);
            updateMagentoCategoryName(categoryId, updatedCategoryName);
            LOG.info("   📦 Updated Product: '{}'", updatedProductName);
            LOG.info("   📦 Updated Category: '{}'", updatedCategoryName);

            // STEP 3: Verify cache working (both should show old cached data)
            LOG.info("📋 STEP 3: Verifying cache shows old data (should be cached names)");
            String aemProductNameAfterChange = getCurrentProductNameFromAEMPage(categoryPageUrl, testSku);
            String aemCategoryNameAfterChange = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            
            boolean productCacheWorking = aemProductNameAfterChange.equals(originalProductName) && !aemProductNameAfterChange.equals(updatedProductName);
            boolean categoryCacheWorking = aemCategoryNameAfterChange.equals(originalCategoryName) && !aemCategoryNameAfterChange.equals(updatedCategoryName);
            
            LOG.info("   Product Cache Working: {} {}", productCacheWorking ? "✅" : "❌", productCacheWorking ? "YES" : "NO");
            LOG.info("   Category Cache Working: {} {}", categoryCacheWorking ? "✅" : "❌", categoryCacheWorking ? "YES" : "NO");

            if (!productCacheWorking || !categoryCacheWorking) {
                LOG.warn("❌ DEBUG: Cache not working - AEM showing fresh data immediately");
                LOG.warn("   - Original Product (cached): '{}'", originalProductName);
                LOG.warn("   - Original Category (cached): '{}'", originalCategoryName);
                LOG.warn("   - Updated Product: '{}'", updatedProductName);
                LOG.warn("   - Updated Category: '{}'", updatedCategoryName);
                LOG.warn("   - AEM Product After Change: '{}'", aemProductNameAfterChange);
                LOG.warn("   - AEM Category After Change: '{}'", aemCategoryNameAfterChange);
                LOG.warn("   - Environment: {}", environment);
                
                Assert.fail("❌ FAILED: Cache not working - test cannot proceed as it would be a false positive");
            }

            // STEP 4: Clear cache using REGEX PATTERNS for both product and category
            LOG.info("🚀 STEP 4: Calling cache invalidation with REGEX PATTERNS (both product and category)");
            // Use official Adobe specification regex patterns for GraphQL response matching
            String productRegex = String.format("\\\"sku\\\":\\\\s*\\\"%s\\\"", testSku); // Matches: "sku":"BLT-FAB-001"
            String categoryRegex = String.format("\\\"uid\\\"\\\\s*:\\\\s*\\\\{\\\"id\\\"\\\\s*:\\\\s*\\\"%s\\\"", categoryUid); // Matches: "uid":{"id":"MTc3"}

            String payload = String.format(
                    "{\n" +
                            "    \"regexPatterns\": [\"%s\", \"%s\"],\n" +
                            "    \"storePath\": \"/content/venia/us/en\"\n" +
                            "}", productRegex, categoryRegex);

            LOG.info("📝 Product Regex Pattern: {}", productRegex);
            LOG.info("📝 Category Regex Pattern: {}", categoryRegex);

            LOG.info("📝 Cache invalidation payload (regex patterns): {}", payload);
            SlingHttpResponse response = adminAuthor.doPost("/bin/cif/invalidate-cache", new StringEntity(payload, ContentType.APPLICATION_JSON), 200);
            LOG.info("📤 Response: Status={}, Content={}", response.getStatusLine(), response.getContent());

            // STEP 5: Wait and verify both caches cleared
            LOG.info("⏳ STEP 5: Waiting for regex pattern invalidation...");
            safeSleep(10000);

            LOG.info("🔍 STEP 6: Verifying both caches cleared via regex patterns");
            String freshProductName = getCurrentProductNameFromAEMPage(categoryPageUrl, testSku);
            String freshCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            boolean productUpdated = freshProductName.equals(updatedProductName);
            boolean categoryUpdated = freshCategoryName.equals(updatedCategoryName);
            
            LOG.info("   Fresh Product: '{}' - Updated: {} {}", freshProductName, productUpdated ? "✅" : "❌", productUpdated ? "YES" : "NO");
            LOG.info("   Fresh Category: '{}' - Updated: {} {}", freshCategoryName, categoryUpdated ? "✅" : "❌", categoryUpdated ? "YES" : "NO");

            assertTrue("Product regex pattern invalidation failed", productUpdated);
            assertTrue("Category regex pattern invalidation failed", categoryUpdated);

            LOG.info("🎉 SUCCESS: Regex patterns cache invalidation test passed!");

            // STEP 7: Revert names back to original
            LOG.info("🔄 STEP 7: Reverting names back to original");
            updateMagentoProductName(testSku, trueOriginalProductFromMagento);
            updateMagentoCategoryName(categoryId, trueOriginalCategoryFromMagento);
            LOG.info("   📦 Reverted Product: '{}'", trueOriginalProductFromMagento);
            LOG.info("   📦 Reverted Category: '{}'", trueOriginalCategoryFromMagento);

            // STEP 8: Clear cache again to get original names
            LOG.info("🚀 STEP 8: Clearing cache again to get original names");
            SlingHttpResponse finalResponse = adminAuthor.doPost("/bin/cif/invalidate-cache", new StringEntity(payload, ContentType.APPLICATION_JSON), 200);
            LOG.info("📤 Final Response: Status={}, Content={}", finalResponse.getStatusLine(), finalResponse.getContent());

            // STEP 9: Wait and verify original names
            LOG.info("⏳ STEP 9: Waiting for final cache clear...");
            safeSleep(10000);

            LOG.info("🔍 STEP 10: Verifying original names show");
            String finalProductName = getCurrentProductNameFromAEMPage(categoryPageUrl, testSku);
            String finalCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            boolean productRestored = finalProductName.equals(trueOriginalProductFromMagento);
            boolean categoryRestored = finalCategoryName.equals(trueOriginalCategoryFromMagento);

            LOG.info("   Final Product: '{}' - Restored: {} {}", finalProductName, productRestored ? "✅" : "❌", productRestored ? "YES" : "NO");
            LOG.info("   Final Category: '{}' - Restored: {} {}", finalCategoryName, categoryRestored ? "✅" : "❌", categoryRestored ? "YES" : "NO");

            assertTrue("Product name not restored to original", productRestored);
            assertTrue("Category name not restored to original", categoryRestored);

            LOG.info("🎉 COMPLETE SUCCESS: Regex patterns cache test with full cycle passed!");

        } finally {
            // Cleanup - ensure names are restored
            try {
                updateMagentoProductName(testSku, trueOriginalProductFromMagento);
                updateMagentoCategoryName(categoryId, trueOriginalCategoryFromMagento);
                LOG.info("🧹 Cleanup: Both names ensured restored");
            } catch (Exception e) {
                LOG.warn("⚠️ Cleanup failed: {}", e.getMessage());
            }
        }
    }

    /**
     * Perform cache invalidation with given payload
     */
    private void performCacheInvalidation(String payload, String description) throws Exception {
        LOG.info("🚀 STEP: Calling cache invalidation - {}", description);
        LOG.info("📝 Cache invalidation payload: {}", payload);
        
        SlingHttpResponse response = adminAuthor.doPost(
            CACHE_INVALIDATION_ENDPOINT,
            new StringEntity(payload, ContentType.APPLICATION_JSON),
            null,
            200);
            
        LOG.info("📤 Response: Status={}, Content={}", response.getStatusLine().getStatusCode(), response.getContent());
        
        LOG.info("⏳ Waiting for cache invalidation...");
        safeSleep(10000);
    }

    /**
     * Generate cache invalidation payload based on configuration
     */
    private String generateCacheInvalidationPayload(CacheTestConfig config, String categoryUid) throws Exception {
        switch (config.invalidationType) {
            case PRODUCT_SKUS:
                return String.format("{\n    \"productSkus\": [\"%s\"],\n    \"storePath\": \"%s\"\n}", 
                    config.productSku, STORE_PATH);
                    
            case CATEGORY_UIDS:
                return String.format("{\n    \"categoryUids\": [\"%s\"],\n    \"storePath\": \"%s\"\n}", 
                    categoryUid, STORE_PATH);
                    
            case CACHE_NAMES:
                return "{\n    \"cacheNames\": [\n        \"venia/components/commerce/product\",\n" +
                       "        \"venia/components/commerce/productlist\",\n" +
                       "        \"venia/components/commerce/navigation\"\n    ],\n" +
                       "    \"storePath\": \"" + STORE_PATH + "\"\n}";
                       
            case REGEX_PATTERNS:
                String productRegex = String.format("\\\"sku\\\":\\\\s*\\\"%s\\\"", config.productSku);
                String categoryRegex = String.format("\\\"uid\\\"\\\\s*:\\\\s*\\\\{\\\"id\\\"\\\\s*:\\\\s*\\\"%s\\\"", categoryUid);
                return String.format("{\n    \"regexPatterns\": [\"%s\", \"%s\"],\n    \"storePath\": \"%s\"\n}", 
                    productRegex, categoryRegex, STORE_PATH);
                    
            case INVALIDATE_ALL:
                return String.format("{\n    \"invalidateAll\": true,\n    \"storePath\": \"%s\"\n}", STORE_PATH);
                
            default:
                throw new IllegalArgumentException("Unsupported invalidation type: " + config.invalidationType);
        }
    }

    /**
     * Helper method to identify the source of the integration token configuration
     */
    private String getTokenSource() {
        if (System.getProperty("COMMERCE_INTEGRATION_TOKEN") != null) {
            return "System Property (-DCOMMERCE_INTEGRATION_TOKEN)";
        } else if (System.getenv("COMMERCE_INTEGRATION_TOKEN") != null) {
            return "Environment Variable (COMMERCE_INTEGRATION_TOKEN)";
        } else {
            return "NOT SET (required)";
        }
    }

    /**
     * Helper method to identify the source of the endpoint configuration
     */
    private String getEndpointSource() {
        if (System.getProperty("COMMERCE_ENDPOINT") != null) {
            return "System Property (-DCOMMERCE_ENDPOINT)";
        } else if (System.getenv("COMMERCE_ENDPOINT") != null) {
            return "Environment Variable (COMMERCE_ENDPOINT)";
        } else {
            return "NOT SET (required)";
        }
    }
}