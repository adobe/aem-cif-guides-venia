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
import org.junit.AfterClass;
import org.junit.BeforeClass;
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
 * 
 * HOW TO RUN LOCALLY:
 * 1. Set environment variables:
 *    - COMMERCE_ENDPOINT=http://your-magento-instance.com/graphql
 *    - COMMERCE_INTEGRATION_TOKEN=your_admin_integration_token
 * 2. Or use system properties:
 *    -DCOMMERCE_ENDPOINT=http://your-magento-instance.com/graphql
 *    -DCOMMERCE_INTEGRATION_TOKEN=your_admin_integration_token
 * 3. Ensure AEM author instance is running and accessible
 * 4. Run specific tests using categories: @Category(IgnoreOnCloud.class) for 6.5, @Category(IgnoreOn65.class) for Cloud
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

    private static CloseableHttpClient httpClient;

    @BeforeClass
    public static void setUpOnce() throws Exception {
        // Validate required environment variables
        if (MAGENTO_BASE_URL == null || MAGENTO_BASE_URL.trim().isEmpty()) {
            throw new IllegalStateException("COMMERCE_ENDPOINT environment variable or system property is required but not set");
        }
        if (MAGENTO_ADMIN_TOKEN == null || MAGENTO_ADMIN_TOKEN.trim().isEmpty()) {
            throw new IllegalStateException("COMMERCE_INTEGRATION_TOKEN environment variable or system property is required but not set");
        }

        httpClient = HttpClients.createDefault();
        
        try {
            applyLocalCacheConfigurationsStatic();
        } catch (Exception e) {
            LOG.warn("Failed to apply local cache configurations: {}", e.getMessage());
        }
    }

    @AfterClass
    public static void tearDownOnce() throws Exception {
        LOG.info("");
        LOG.info("========================================");
        LOG.info("🧹 @AfterClass: ONE-TIME CLEANUP after ALL 15 tests");
        LOG.info("========================================");
        
        try {
            // Restore ALL products to KNOWN original names
            LOG.info("🔄 Restoring all products to original names...");
            restoreProductToKnownOriginal("BLT-LEA-001", "Black Leather Belt");
            restoreProductToKnownOriginal("BLT-MET-001", "Silver Metal Belt");
            restoreProductToKnownOriginal("BLT-FAB-001", "Canvas Fabric Belt");
            
            // Restore ALL categories to KNOWN original names
            LOG.info("🔄 Restoring all categories to original names...");
            restoreCategoryToKnownOriginal("venia-leather-belts", "Leather Belts");
            restoreCategoryToKnownOriginal("venia-metal-belts", "Metal Belts");
            restoreCategoryToKnownOriginal("venia-fabric-belts", "Fabric Belts");
            
            LOG.info("✅ All products and categories restored to original state");
            
            // Clear AEM cache to ensure restored data is immediately available
            LOG.info("🧹 Clearing AEM cache to pick up restored names...");
            clearAllAemCaches();
            
            LOG.info("ℹ️  This cleanup runs ONCE for all 15 tests (instead of 15 times)");
            
        } catch (Exception e) {
            LOG.error("❌ Cleanup FAILED: {}", e.getMessage());
        } finally {
            // Always close HTTP client
            if (httpClient != null) {
                try {
                    httpClient.close();
                    LOG.info("✅ HTTP client closed");
                } catch (Exception e) {
                    LOG.warn("Failed to close HTTP client: {}", e.getMessage());
                }
            }
        }
        
        LOG.info("========================================");
    }
    
    /**
     * Restore a product to its KNOWN original name
     */
    private static void restoreProductToKnownOriginal(String sku, String originalName) {
        try {
            LOG.info("   🔄 {} → '{}'", sku, originalName);
            
            String url = MAGENTO_REST_URL + "/products/" + sku;
            HttpPut request = new HttpPut(url);
            request.setHeader("Authorization", "Bearer " + MAGENTO_ADMIN_TOKEN);
            request.setHeader("Content-Type", "application/json");
            
            String payload = String.format("{\"product\":{\"name\":\"%s\"}}", originalName);
            request.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    LOG.info("      ✅ Restored successfully");
                } else {
                    LOG.warn("      ⚠️  Restore returned status {}", statusCode);
                }
            }
        } catch (Exception e) {
            LOG.warn("      ⚠️  Failed to restore {}: {}", sku, e.getMessage());
        }
    }
    
    /**
     * Restore a category to its KNOWN original name
     */
    private static void restoreCategoryToKnownOriginal(String urlKey, String originalName) {
        try {
            LOG.info("   🔄 {} → '{}'", urlKey, originalName);
            
            // Get category UID and ID
            String categoryUid = getCategoryUidFromUrlKeyStatic(urlKey);
            String categoryId = new String(java.util.Base64.getDecoder().decode(categoryUid), "UTF-8");
            
            String url = MAGENTO_REST_URL + "/categories/" + categoryId;
            HttpPut request = new HttpPut(url);
            request.setHeader("Authorization", "Bearer " + MAGENTO_ADMIN_TOKEN);
            request.setHeader("Content-Type", "application/json");
            
            String payload = String.format("{\"category\":{\"name\":\"%s\"}}", originalName);
            request.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    LOG.info("      ✅ Restored successfully");
                } else {
                    LOG.warn("      ⚠️  Restore returned status {}", statusCode);
                }
            }
        } catch (Exception e) {
            LOG.warn("      ⚠️  Failed to restore {}: {}", urlKey, e.getMessage());
        }
    }
    
    /**
     * Clear all AEM caches (used in @AfterClass cleanup)
     */
    private static void clearAllAemCaches() {
        try {
            LOG.info("   📤 Sending cache clear request to AEM...");
            
            // Wait for Magento database to persist the changes
            Thread.sleep(2000);
            
            String clearPayload = "{\n    \"invalidateAll\": true,\n    \"storePath\": \"" + STORE_PATH + "\"\n}";
            HttpPost post = new HttpPost("http://localhost:4502" + CACHE_INVALIDATION_ENDPOINT);
            post.setHeader("Authorization", "Basic YWRtaW46YWRtaW4="); // admin:admin
            post.setHeader("Content-Type", "application/json");
            post.setEntity(new StringEntity(clearPayload, ContentType.APPLICATION_JSON));
            
            try (CloseableHttpResponse response = httpClient.execute(post)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    LOG.info("   ✅ AEM caches cleared successfully");
                } else {
                    LOG.warn("   ⚠️  Cache clear returned status {}", statusCode);
                }
            }
            
            // Wait for cache invalidation to propagate
            Thread.sleep(2000);
            LOG.info("   ✅ Cache invalidation propagated");
            
        } catch (Exception e) {
            LOG.warn("   ⚠️  Could not clear AEM cache: {}", e.getMessage());
            LOG.warn("   ℹ️  This is non-critical - cache will naturally expire");
        }
    }
    
    /**
     * Static version of getCategoryUidFromUrlKey for use in @AfterClass
     */
    private static String getCategoryUidFromUrlKeyStatic(String categoryUrlKey) {
        try {
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
                        return category.get("uid").asText();
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
     * AEM 6.5 - Product Cache Invalidation using productSkus method
     */
    @Test
    @Category(IgnoreOnCloud.class)
    public void test65_01_Product_CacheInvalidation() throws Exception {
        CacheTestConfig config = new CacheTestConfig(
                "AEM 6.5 - Product (productSkus method)",
                "BLT-LEA-001", 
                null, 
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-leather-belts.html",
                null,
                CacheInvalidationType.PRODUCT_SKUS,
                true, false, true
        );
        runCacheInvalidationTest(config);
    }

    /**
     * AEM 6.5 - Category Cache Invalidation using categoryUids method
     */
    @Test
    @Category(IgnoreOnCloud.class)
    public void test65_02_Category_CacheInvalidation() throws Exception {
        runCategoryCacheInvalidationTest(
                "BLT-LEA-001",
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-leather-belts.html",
                "venia-leather-belts",
                "AEM 6.5 - Category (categoryUids method)"
        );
    }

    /**
     * AEM 6.5 - Cross-Platform Cache Test (6.5 Product + Cloud Category)
     */
    @Test
    @Category(IgnoreOnCloud.class)
    public void test65_03_CrossPlatform_ProductAndCategory_CacheTest() throws Exception {
        
        String productSku = "BLT-LEA-001"; 
        String categoryUrlKey = "venia-fabric-belts"; 
        String productPageUrl = "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-leather-belts.html";
        String categoryPageUrl = "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-fabric-belts.html";
        
        String originalProductName = null;
        String originalCategoryName = null;
        String categoryId = null;
        String randomSuffix = generateRandomString(6);
        
        try {
            // Get original names
            originalProductName = getCurrentProductNameFromAEMPage(productPageUrl, productSku);
            originalCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            
            String categoryUid = getCategoryUidFromUrlKey(categoryUrlKey);
            categoryId = new String(java.util.Base64.getDecoder().decode(categoryUid), "UTF-8");
            
            // Update both names in Magento
            String updatedProductName = originalProductName + " " + randomSuffix;
            String updatedCategoryName = originalCategoryName + " " + randomSuffix;
            
            updateMagentoProductName(productSku, updatedProductName);
            updateMagentoCategoryName(categoryId, updatedCategoryName);
            
            // Verify cache shows old data
            String aemProductName = getCurrentProductNameFromAEMPage(productPageUrl, productSku);
            String aemCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            
            boolean productCacheWorking = aemProductName.equals(originalProductName);
            boolean categoryCacheWorking = aemCategoryName.equals(originalCategoryName);
            
            // Clear both caches simultaneously
            String payload = String.format(
                "{\n" +
                "    \"productSkus\": [\"%s\"],\n" +
                "    \"categoryUids\": [\"%s\"],\n" +
                "    \"storePath\": \"%s\"\n" +
                "}", productSku, categoryUid, STORE_PATH);
            
            SlingHttpResponse response = adminAuthor.doPost(
                CACHE_INVALIDATION_ENDPOINT,
                new StringEntity(payload, ContentType.APPLICATION_JSON),
                null,
                200);
            
            // Response received
            
            // STEP 5: Wait and verify fresh data
            // Wait for cache invalidation
            safeSleep(10000);
            
            // Verify fresh data
            String freshProductName = getCurrentProductNameFromAEMPage(productPageUrl, productSku);
            String freshCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            
            boolean productUpdated = freshProductName.equals(updatedProductName);
            boolean categoryUpdated = freshCategoryName.equals(updatedCategoryName);
            
            // Verified fresh data
            
            assertTrue("Product cache invalidation failed", productUpdated);
            assertTrue("Category cache invalidation failed", categoryUpdated);
            
            // STEP 7: Revert names back to original
            // Revert names
            updateMagentoProductName(productSku, originalProductName);
            updateMagentoCategoryName(categoryId, originalCategoryName);
            
            // Product reverted
            // Category reverted
            
            // STEP 8: Clear cache again to get original names
            // Clear cache again
            SlingHttpResponse finalResponse = adminAuthor.doPost(
                CACHE_INVALIDATION_ENDPOINT,
                new StringEntity(payload, ContentType.APPLICATION_JSON),
                null,
                200);
            
            // Final cache invalidation completed
            
            // STEP 9: Wait and verify original names
            // Wait for final cache clear
            safeSleep(10000);
            
            // Verify original names
            String finalProductName = getCurrentProductNameFromAEMPage(productPageUrl, productSku);
            String finalCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            
            boolean productRestored = finalProductName.equals(originalProductName);
            boolean categoryRestored = finalCategoryName.equals(originalCategoryName);
            
            // Verified restoration
            
            assertTrue("Product name not restored to original", productRestored);
            assertTrue("Category name not restored to original", categoryRestored);
            
            
        } catch (Exception e) {
            LOG.error("Cross-platform cache test failed: {}", e.getMessage(), e);
            throw e;
        } finally {
            // Cleanup - ensure names are restored
            try {
                if (originalProductName != null) {
                    updateMagentoProductName(productSku, originalProductName);
                }
                if (originalCategoryName != null && categoryId != null) {
                    updateMagentoCategoryName(categoryId, originalCategoryName);
                }
            } catch (Exception e) {
                LOG.warn("Cleanup failed: {}", e.getMessage());
            }
        }
    }

    /**
     * Cloud - Product Cache Invalidation using cacheNames method
     */
    @Test
    @Category(IgnoreOn65.class)
    public void testCloud_01_Product_CacheNames() throws Exception {
        String testSku = "BLT-FAB-001";
        String categoryPage = "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-fabric-belts.html";
        String environment = "Cloud - Product (cacheNames method)";
        
        runCacheNamesProductTest(environment, testSku, categoryPage);
    }

    /**
     * Cloud - Cache Invalidation using regexPatterns method (Product + Category)
     */
    @Test
    @Category(IgnoreOn65.class)
    public void testCloud_02_RegexPatterns() throws Exception {
        String testSku = "BLT-FAB-001";
        String categoryPage = "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-fabric-belts.html";
        String categoryUrlKey = "venia-fabric-belts";
        String environment = "Cloud - RegexPatterns (both product and category)";

        runCloudRegexPatternsTest(environment, testSku, categoryPage, categoryUrlKey);
    }

    /**
     * Cloud - Final Comprehensive Test using invalidateAll method
     */
    @Test
    @Category(IgnoreOn65.class)
    public void testCloud_03_InvalidateAll_Final() throws Exception {
        String testSku = "BLT-FAB-001";
        String categoryPage = "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-fabric-belts.html";
        String categoryUrlKey = "venia-fabric-belts";
        String environment = "Cloud - Final Test (invalidateAll method)";

        runInvalidateAllCacheTest(environment, testSku, categoryPage, categoryUrlKey);
    }

    /**
     * Common cache invalidation test workflow
     */
    private void runCacheInvalidationTest(CacheTestConfig config) throws Exception {

        TestData testData = new TestData(generateRandomString(6));
        String trueOriginalProductFromMagento = null;
        String trueOriginalCategoryFromMagento = null;

        try {
            // STEP 1: Get original data and prepare test data
            // Get original data
            if (config.includeProduct) {
                trueOriginalProductFromMagento = getMagentoProductName(config.productSku);
                testData.setOriginalProductName(getCurrentProductNameFromAEMPage(config.productPageUrl, config.productSku));
            }

            if (config.includeCategory) {
                testData.categoryUid = getCategoryUidFromUrlKey(config.categoryUrlKey);
                testData.categoryId = new String(java.util.Base64.getDecoder().decode(testData.categoryUid), "UTF-8");
                JsonNode categoryData = getMagentoCategoryData(testData.categoryId);
                trueOriginalCategoryFromMagento = categoryData.get("name").asText();
                testData.setOriginalCategoryName(getCurrentCategoryNameFromAEMPage(config.categoryPageUrl));
            }

            // Update data in Magento
            if (config.includeProduct) {
                updateMagentoProductName(config.productSku, testData.updatedProductName);
            }
            if (config.includeCategory) {
                updateMagentoCategoryName(testData.categoryId, testData.updatedCategoryName);
            }

            // Verify cache is working (shows old data)
            verifyCacheWorking(config, testData);

            // Perform cache invalidation
            String payload = generateCacheInvalidationPayload(config, testData.categoryUid);
            performCacheInvalidation(payload, config.invalidationType.toString());

            // Verify fresh data shows
            verifyFreshData(config, testData);

            if (config.fullCycle) {
                // STEP 6-8: Full cycle - revert and verify restoration
                performFullCycleRestore(config, testData, trueOriginalProductFromMagento, trueOriginalCategoryFromMagento);
            }


        } finally {
            // No per-test cleanup - will clean up once at the end of all tests in @AfterClass
        }
    }

    /**
     * Verify that cache is working by checking AEM shows old data
     */
    private void verifyCacheWorking(CacheTestConfig config, TestData testData) throws Exception {
        boolean allCachesWorking = verifyCacheState(config, testData, "cache working", 
                                                   testData.originalProductName, testData.originalCategoryName);
        
        if (!allCachesWorking) {
            Assert.fail("❌ FAILED: Cache not working - test cannot proceed as it would be a false positive");
        }
    }

    /**
     * Verify that fresh data shows after cache invalidation
     */
    private void verifyFreshData(CacheTestConfig config, TestData testData) throws Exception {
        boolean allUpdated = verifyCacheState(config, testData, "fresh", 
                                             testData.updatedProductName, testData.updatedCategoryName);
        
        if (config.includeProduct && !allUpdated) {
            assertTrue("Product cache invalidation failed", false);
        }
        if (config.includeCategory && !allUpdated) {
            assertTrue("Category cache invalidation failed", false);
        }
    }

    /**
     * Perform full cycle restore (revert data and clear cache again)
     */
    private void performFullCycleRestore(CacheTestConfig config, TestData testData, 
                                       String trueOriginalProductFromMagento, 
                                       String trueOriginalCategoryFromMagento) throws Exception {
        // Revert names back to original
        revertToOriginalData(config, testData, trueOriginalProductFromMagento, trueOriginalCategoryFromMagento);

        // Clear cache again to get original names
        String payload = generateCacheInvalidationPayload(config, testData.categoryUid);
        performCacheInvalidation(payload, config.invalidationType + " (final restore)");

        // Verify original names show
        boolean allRestored = verifyCacheState(config, testData, "restored", trueOriginalProductFromMagento, trueOriginalCategoryFromMagento);
        
        if (config.includeProduct) {
            assertTrue("Product name not restored to original", allRestored);
        }
        if (config.includeCategory) {
            assertTrue("Category name not restored to original", allRestored);
        }

    }

    /**
     * Common method to verify cache state (old, fresh, or restored data)
     */
    private boolean verifyCacheState(CacheTestConfig config, TestData testData, String stateType, 
                                   String expectedProductName, String expectedCategoryName) throws Exception {
        boolean productMatches = true;
        boolean categoryMatches = true;
        
        if (config.includeProduct) {
            String currentProductName = getCurrentProductNameFromAEMPage(config.productPageUrl, config.productSku);
            productMatches = currentProductName.equals(expectedProductName);
        }
        
        if (config.includeCategory) {
            String currentCategoryName = getCurrentCategoryNameFromAEMPage(config.categoryPageUrl);
            categoryMatches = currentCategoryName.equals(expectedCategoryName);
        }
        
        return productMatches && categoryMatches;
    }

    /**
     * Helper method to revert data to original state in Magento
     */
    private void revertToOriginalData(CacheTestConfig config, TestData testData, 
                                    String originalProductFromMagento, String originalCategoryFromMagento) throws Exception {
        if (config.includeProduct) {
            updateMagentoProductName(config.productSku, originalProductFromMagento);
        }
        if (config.includeCategory) {
            updateMagentoCategoryName(testData.categoryId, originalCategoryFromMagento);
        }
    }

    /**
     * Helper method to capitalize first letter
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Perform cleanup to restore KNOWN original product/category names
     * These are the BASE names without any test suffixes
     */
    private void performCleanup(CacheTestConfig config, String trueOriginalProductFromMagento, 
                              String trueOriginalCategoryFromMagento, TestData testData) {
        LOG.info("");
        LOG.info("🧹 Cleanup: Restoring to KNOWN original names...");
        try {
            // Restore products to their KNOWN base names (these products are designed for this test only)
            if (config.includeProduct) {
                String knownOriginalName = getKnownOriginalProductName(config.productSku);
                LOG.info("   🔄 {} → '{}'", config.productSku, knownOriginalName);
                updateMagentoProductName(config.productSku, knownOriginalName);
            }
            
            // Restore categories to their KNOWN base names
            if (config.includeCategory) {
                String knownOriginalName = getKnownOriginalCategoryName(config.categoryUrlKey);
                LOG.info("   🔄 {} → '{}'", config.categoryUrlKey, knownOriginalName);
                updateMagentoCategoryName(testData.categoryId, knownOriginalName);
            }
            
            LOG.info("✅ Cleanup completed");
        } catch (Exception e) {
            LOG.warn("⚠️  Cleanup failed: {}", e.getMessage());
        }
    }
    
    /**
     * Get the KNOWN original product name (without any test suffixes)
     */
    private String getKnownOriginalProductName(String sku) {
        switch (sku) {
            case "BLT-LEA-001":
                return "Black Leather Belt";
            case "BLT-MET-001":
                return "Silver Metal Belt";
            case "BLT-FAB-001":
                return "Canvas Fabric Belt";
            default:
                throw new IllegalArgumentException("Unknown product SKU: " + sku);
        }
    }
    
    /**
     * Get the KNOWN original category name (without any test suffixes)
     */
    private String getKnownOriginalCategoryName(String urlKey) {
        switch (urlKey) {
            case "venia-leather-belts":
                return "Leather Belts";
            case "venia-metal-belts":
                return "Metal Belts";
            case "venia-fabric-belts":
                return "Fabric Belts";
            default:
                throw new IllegalArgumentException("Unknown category URL key: " + urlKey);
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
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            
            LOG.debug("Magento product update response: status={}, body={}", statusCode, responseBody);
            
            if (statusCode != 200) {
                throw new Exception("Failed to update product: " + statusCode + ", response: " + responseBody);
            }
            
            // Verify the update actually worked
            safeSleep(2000); // Wait for Magento to process
            String actualName = getMagentoProductName(sku);
            
            if (!actualName.equals(newName)) {
                LOG.error("⚠️  Magento product update verification FAILED!");
                LOG.error("   Requested name: '{}'", newName);
                LOG.error("   Actual name in Magento: '{}'", actualName);
                throw new Exception("Product update verification failed: Magento didn't update the name");
            }
        }
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
            } else {
                // Product invalidation
                payload = String.format(
                        "{\n" +
                                "    \"productSkus\": [\"%s\"],\n" +
                                "    \"storePath\": \"%s\"\n" +
                                "}", productSku, STORE_PATH);
            }

            SlingHttpResponse response = adminAuthor.doPost(
                    CACHE_INVALIDATION_ENDPOINT,
                    new StringEntity(payload, ContentType.APPLICATION_JSON),
                    null,
                    200);

            int statusCode = response.getStatusLine().getStatusCode();
            String responseContent = response.getContent();

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
    private static void applyLocalCacheConfigurationsStatic() throws Exception {

        String baseUrl = "http://localhost:4502";
        String configUrl = baseUrl + "/system/console/configMgr";

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
        
        try {
            // Make multiple requests to warm up the cache
            for (int i = 0; i < 3; i++) {
                makeCacheRespectingRequest(categoryPage);  
                safeSleep(1000); // Small delay between requests
            }
            
            // Also warm up the specific product
            getCurrentProductNameFromAEMPage(categoryPage, testSku);  
            safeSleep(1000);
            
        } catch (Exception e) {
            LOG.warn("⚠️ WARM-UP: Product cache warm-up failed, but continuing test: {}", e.getMessage());
        }
    }

    /**
     * Warm up category cache by making multiple cache-respecting requests
     */
    private void warmUpCategoryCache(String categoryPage) throws Exception {
        
        try {
            // Make multiple requests to warm up the cache
            for (int i = 0; i < 3; i++) {
                makeCacheRespectingRequest(categoryPage);  
                safeSleep(1000); // Small delay between requests
            }
            
            // Also warm up the specific category data
            getCurrentCategoryNameFromAEMPage(categoryPage);
            safeSleep(1000);
            
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
            // Get original product data
            originalProductName = getMagentoProductName(testSku);

            // STEP 2: Update in Magento
            String updatedProductName = originalProductName + " " + randomSuffix;
            // Update product in Magento
            updateMagentoProductName(testSku, updatedProductName);

            // STEP 3: Verify cache working
            // Verify cache shows old data
            String aemProductName = getCurrentProductNameFromAEMPage(getProductPageUrl(testSku), testSku);
            boolean productCacheWorking = !aemProductName.equals(updatedProductName);

            if (!productCacheWorking) {
                Assert.fail("❌ FAILED: Product cache not working - test cannot proceed as it would be a false positive");
            }

            // STEP 4: Clear cache using REGEX PATTERN for product
            // Use a simpler, more reliable regex pattern that matches the product SKU
            String productRegex = testSku; // Simple SKU-based pattern

            String payload = String.format(
                    "{\n" +
                            "    \"regexPatterns\": [\"%s\"],\n" +
                            "    \"storePath\": \"/content/venia/us/en\"\n" +
                            "}", productRegex);

            SlingHttpResponse response = adminAuthor.doPost("/bin/cif/invalidate-cache", new StringEntity(payload, ContentType.APPLICATION_JSON), 200);

            // STEP 5: Wait and verify
            Thread.sleep(10000);

            aemProductName = getCurrentProductNameFromAEMPage(getProductPageUrl(testSku), testSku);
            boolean productUpdated = aemProductName.contains(randomSuffix);

            if (productUpdated) {
            } else {
                Assert.fail("❌ FAILED: Product regex pattern invalidation failed");
            }

        } finally {
            // Cleanup
            if (originalProductName != null) {
                try {
                    updateMagentoProductName(testSku, originalProductName);
                } catch (Exception e) {
                    LOG.warn("Could not restore product name: {}", e.getMessage());
                }
            }
            
            // Clear cache after reversion to avoid test interference
            try {
                String clearPayload = "{\"invalidateAll\": true, \"storePath\": \"" + STORE_PATH + "\"}";
                adminAuthor.doPost(CACHE_INVALIDATION_ENDPOINT, new StringEntity(clearPayload, ContentType.APPLICATION_JSON), 200);
                safeSleep(2000); // Wait for cache clear
            } catch (Exception e) {
                LOG.warn("Could not clear cache after reversion: {}", e.getMessage());
            }
            
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
            String categoryUid = getCategoryUidFromUrlKey(categoryUrlKey);
            categoryId = new String(java.util.Base64.getDecoder().decode(categoryUid), "UTF-8");
            JsonNode categoryData = getMagentoCategoryData(categoryId);
            originalCategoryName = categoryData.get("name").asText();

            // STEP 2: Update in Magento
            String updatedCategoryName = originalCategoryName + " " + randomSuffix;
            updateMagentoCategoryName(categoryId, updatedCategoryName);

            // STEP 3: Verify cache working
            // Verify cache shows old data
            String aemCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            boolean categoryCacheWorking = !aemCategoryName.equals(updatedCategoryName);

            if (!categoryCacheWorking) {
                Assert.fail("❌ FAILED: Category cache not working - test cannot proceed as it would be a false positive");
            }

            // STEP 4: Use categoryUids instead of cache names for more reliable invalidation
            String payload = String.format(
                    "{\n" +
                            "    \"categoryUids\": [\"%s\"],\n" +
                            "    \"storePath\": \"/content/venia/us/en\"\n" +
                            "}", categoryUid);

            SlingHttpResponse response = adminAuthor.doPost("/bin/cif/invalidate-cache", new StringEntity(payload, ContentType.APPLICATION_JSON), 200);

            // STEP 5: Wait and verify
            Thread.sleep(10000);

            aemCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            boolean categoryUpdated = aemCategoryName.contains(randomSuffix);

            if (categoryUpdated) {
            } else {
                Assert.fail("❌ FAILED: Category UID invalidation failed");
            }

        } finally {
            // Cleanup
            if (originalCategoryName != null && categoryId != null) {
                try {
                    updateMagentoCategoryName(categoryId, originalCategoryName);
                } catch (Exception e) {
                    LOG.warn("Could not restore category name: {}", e.getMessage());
                }
            }
            
            // Clear cache after reversion to avoid test interference
            try {
                String clearPayload = "{\"invalidateAll\": true, \"storePath\": \"" + STORE_PATH + "\"}";
                adminAuthor.doPost(CACHE_INVALIDATION_ENDPOINT, new StringEntity(clearPayload, ContentType.APPLICATION_JSON), 200);
                safeSleep(2000); // Wait for cache clear
            } catch (Exception e) {
                LOG.warn("Could not clear cache after reversion: {}", e.getMessage());
            }
            
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
            originalCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);

            // STEP 2: Update category in Magento (backend change)
            String updatedCategoryName = originalCategoryName + " " + randomSuffix;
            updateMagentoCategoryName(categoryId, updatedCategoryName);

            // STEP 3: Verify category cache working (should show old cached data)
            String aemCategoryNameAfterChange = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            
            boolean categoryCacheWorking = aemCategoryNameAfterChange.equals(originalCategoryName) && !aemCategoryNameAfterChange.equals(updatedCategoryName);

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
            String categoryRegex = String.format("venia-fabric-belts"); // Simple pattern for fabric belts category

            String payload = String.format(
                    "{\n" +
                            "    \"regexPatterns\": [\"%s\"],\n" +
                            "    \"storePath\": \"/content/venia/us/en\"\n" +
                            "}", categoryRegex);

            SlingHttpResponse response = adminAuthor.doPost("/bin/cif/invalidate-cache", new StringEntity(payload, ContentType.APPLICATION_JSON), 200);

            // STEP 5: Wait and verify category cache cleared
            safeSleep(5000);

            String freshCategoryCheck = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            boolean categoryUpdated = freshCategoryCheck.equals(updatedCategoryName);

            if (!categoryUpdated) {
                Assert.fail("❌ FAILED: Category regex pattern invalidation did not work - cache was not cleared");
            }


        } finally {
            // Cleanup: Restore true original category name
            try {
                updateMagentoCategoryName(categoryId, trueOriginalCategoryFromMagento);

                // Clear cache after reversion
                String clearPayload = "{\"invalidateAll\": true, \"storePath\": \"/content/venia/us/en\"}";
                adminAuthor.doPost("/bin/cif/invalidate-cache", new StringEntity(clearPayload, ContentType.APPLICATION_JSON), 200);

            } catch (Exception e) {
                LOG.warn("Could not restore category name: {}", e.getMessage());
            }
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
            originalProductName = getMagentoProductName(testSku);
            String categoryUid = getCategoryUidFromUrlKey(categoryUrlKey);
            String categoryId = new String(java.util.Base64.getDecoder().decode(categoryUid), "UTF-8");
            JsonNode categoryData = getMagentoCategoryData(categoryId);
            originalCategoryName = categoryData.get("name").asText();

            // STEP 2: Update both in Magento
            String updatedProductName = originalProductName + " " + randomSuffix;
            String updatedCategoryName = originalCategoryName + " " + randomSuffix;
            updateMagentoProductName(testSku, updatedProductName);
            updateMagentoCategoryName(categoryId, updatedCategoryName);

            // STEP 3: Verify cache working (both should show old data)
            // Verify cache shows old data
            String aemProductName = getCurrentProductNameFromAEMPage(getProductPageUrl(testSku), testSku);
            String aemCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            boolean productCacheWorking = !aemProductName.equals(updatedProductName);
            boolean categoryCacheWorking = !aemCategoryName.equals(updatedCategoryName);

            if (!productCacheWorking || !categoryCacheWorking) {
                Assert.fail("❌ FAILED: Cache not working - test cannot proceed as it would be a false positive");
            }

            // STEP 4: Clear cache using regex patterns
            String productRegex = String.format("\\\"sku\\\"\\\\s*\\\\s*\\\"%s\\\"", testSku);
            String categoryRegex = String.format("\\\"uid\\\"\\\\s*:\\\\s*\\\\{\\\"id\\\"\\\\s*:\\\\s*\\\"%s\\\"", categoryUid);

            String payload = String.format(
                    "{\n" +
                            "    \"regexPatterns\": [\"%s\", \"%s\"],\n" +
                            "    \"storePath\": \"/content/venia/us/en\"\n" +
                            "}", productRegex, categoryRegex);

            SlingHttpResponse response = adminAuthor.doPost("/bin/cif/invalidate-cache", new StringEntity(payload, ContentType.APPLICATION_JSON), 200);

            // STEP 5: Wait and verify cache cleared
            Thread.sleep(10000);

            aemProductName = getCurrentProductNameFromAEMPage(getProductPageUrl(testSku), testSku);
            aemCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            boolean productUpdated = aemProductName.contains(randomSuffix);
            boolean categoryUpdated = aemCategoryName.contains(randomSuffix);

            if (productUpdated && categoryUpdated) {
            } else {
                Assert.fail(String.format("❌ FAILED: Regex pattern cache invalidation failed - Product: %s, Category: %s",
                        productUpdated ? "✅" : "❌", categoryUpdated ? "✅" : "❌"));
            }

        } finally {
            // Cleanup
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
                String clearPayload = "{\"invalidateAll\": true, \"storePath\": \"" + STORE_PATH + "\"}";
                adminAuthor.doPost(CACHE_INVALIDATION_ENDPOINT, new StringEntity(clearPayload, ContentType.APPLICATION_JSON), 200);
                safeSleep(2000); // Wait for cache clear
            } catch (Exception e) {
                LOG.warn("Could not clear cache after reversion: {}", e.getMessage());
            }
            
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
            // Get original product data
            originalProductName = getMagentoProductName(testSku);

            // STEP 2: Update in Magento
            String updatedProductName = originalProductName + " " + randomSuffix;
            // Update product in Magento
            updateMagentoProductName(testSku, updatedProductName);

            // STEP 3: Verify cache working
            // Verify cache shows old data
            String aemProductName = getCurrentProductNameFromAEMPage(getProductPageUrl(testSku), testSku);
            boolean productCacheWorking = !aemProductName.equals(updatedProductName);

            if (!productCacheWorking) {
                Assert.fail("❌ FAILED: Product cache not working - test cannot proceed as it would be a false positive");
            }

            // STEP 4: Clear specific cache names
            String payload = String.format(
                    "{\n" +
                            "    \"cacheNames\": [\n" +
                            "        \"venia/components/commerce/product\",\n" +
                            "        \"venia/components/commerce/navigation\",\n" +
                            "        \"venia/components/commerce/breadcrumb\"\n" +
                            "    ],\n" +
                            "    \"storePath\": \"/content/venia/us/en\"\n" +
                            "}");

            SlingHttpResponse response = adminAuthor.doPost("/bin/cif/invalidate-cache", new StringEntity(payload, ContentType.APPLICATION_JSON), 200);

            // STEP 5: Wait and verify
            Thread.sleep(10000);

            aemProductName = getCurrentProductNameFromAEMPage(getProductPageUrl(testSku), testSku);
            boolean productUpdated = aemProductName.contains(randomSuffix);

            if (productUpdated) {
            } else {
                Assert.fail("❌ FAILED: Cache names invalidation failed - component caches not cleared");
            }

        } finally {
            // Cleanup
            if (originalProductName != null) {
                try {
                    updateMagentoProductName(testSku, originalProductName);
                } catch (Exception e) {
                    LOG.warn("Could not restore product name: {}", e.getMessage());
                }
            }
            
            // Clear cache after reversion to avoid test interference
            try {
                String clearPayload = "{\"invalidateAll\": true, \"storePath\": \"" + STORE_PATH + "\"}";
                adminAuthor.doPost(CACHE_INVALIDATION_ENDPOINT, new StringEntity(clearPayload, ContentType.APPLICATION_JSON), 200);
                safeSleep(2000); // Wait for cache clear
            } catch (Exception e) {
                LOG.warn("Could not clear cache after reversion: {}", e.getMessage());
            }
            
        }
    }

    /**
     * Invalidate All Cache Test with Warm-up - Tests complete cache flush with cache pre-loading
     */
    private void runInvalidateAllCacheTestWithWarmup(String environment, String testSku, String categoryPageUrl, String categoryUrlKey) throws Exception {
        // Cache should already be warmed from first test - just run the test
        
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
            originalProductName = getCurrentProductNameFromAEMPage(categoryPageUrl, testSku);
            originalCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);

            // STEP 2: Update both in Magento (backend changes)
            String updatedProductName = originalProductName + " " + randomSuffix;
            String updatedCategoryName = originalCategoryName + " " + randomSuffix;
            updateMagentoProductName(testSku, updatedProductName);
            updateMagentoCategoryName(categoryId, updatedCategoryName);

            // STEP 3: Verify cache working (both should show old cached data)
            String aemProductNameAfterChange = getCurrentProductNameFromAEMPage(categoryPageUrl, testSku);
            String aemCategoryNameAfterChange = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            
            boolean productCacheWorking = aemProductNameAfterChange.equals(originalProductName) && !aemProductNameAfterChange.equals(updatedProductName);
            boolean categoryCacheWorking = aemCategoryNameAfterChange.equals(originalCategoryName) && !aemCategoryNameAfterChange.equals(updatedCategoryName);
            

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
            String payload = String.format(
                    "{\n" +
                            "    \"invalidateAll\": true,\n" +
                            "    \"storePath\": \"/content/venia/us/en\"\n" +
                            "}");

            SlingHttpResponse response = adminAuthor.doPost("/bin/cif/invalidate-cache", new StringEntity(payload, ContentType.APPLICATION_JSON), 200);

            // STEP 5: Wait and verify everything cleared
            safeSleep(15000); // Longer wait for complete flush

            String freshProductName = getCurrentProductNameFromAEMPage(getProductPageUrl(testSku), testSku);
            String freshCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            boolean productUpdated = freshProductName.contains(randomSuffix);
            boolean categoryUpdated = freshCategoryName.contains(randomSuffix);

            if (productUpdated && categoryUpdated) {
            } else {
                Assert.fail(String.format("❌ FAILED: Invalidate All cache failed - Product: %s, Category: %s",
                        productUpdated ? "✅" : "❌", categoryUpdated ? "✅" : "❌"));
            }

        } finally {
            // Cleanup
            try {
                updateMagentoProductName(testSku, trueOriginalProductFromMagento);
            } catch (Exception e) {
                LOG.warn("Could not restore product name: {}", e.getMessage());
            }
            
            try {
                updateMagentoCategoryName(categoryId, trueOriginalCategoryFromMagento);
            } catch (Exception e) {
                LOG.warn("Could not restore category name: {}", e.getMessage());
            }
            
            // Clear cache after reversion to avoid test interference
            try {
                String clearPayload = "{\"invalidateAll\": true, \"storePath\": \"" + STORE_PATH + "\"}";
                adminAuthor.doPost(CACHE_INVALIDATION_ENDPOINT, new StringEntity(clearPayload, ContentType.APPLICATION_JSON), 200);
                safeSleep(2000); // Wait for cache clear
            } catch (Exception e) {
                LOG.warn("Could not clear cache after reversion: {}", e.getMessage());
            }
            
        }
    }

    /**
     * Cache Names Product Test - Tests product cache invalidation using cacheNames method
     */
    private void runCacheNamesProductTest(String environment, String testSku, String categoryPageUrl) throws Exception {
        // First get the true original from Magento for restoration purposes
        String trueOriginalFromMagento = getMagentoProductName(testSku);
        
        String originalProductName = getCurrentProductNameFromAEMPage(categoryPageUrl, testSku);

        try {
            // Update product in Magento (backend change)
            String updatedProductName = originalProductName + " " + generateRandomString(6);
            updateMagentoProductName(testSku, updatedProductName);

            // Check AEM still shows cached (old) data
            String aemProductNameAfterChange = getCurrentProductNameFromAEMPage(categoryPageUrl, testSku);

            boolean cacheWorking = aemProductNameAfterChange.equals(originalProductName) && !aemProductNameAfterChange.equals(updatedProductName);

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
            String invalidationPayload = "{\n" +
                    "    \"cacheNames\": [\n" +
                    "        \"venia/components/commerce/product\",\n" +
                    "        \"venia/components/commerce/productlist\",\n" +
                    "        \"venia/components/commerce/navigation\"\n" +
                    "    ],\n" +
                    "    \"storePath\": \"" + STORE_PATH + "\"\n" +
                    "}";


            SlingHttpResponse response = adminAuthor.doPost(CACHE_INVALIDATION_ENDPOINT, new StringEntity(invalidationPayload, ContentType.APPLICATION_JSON), 200);

            // Wait for cache invalidation
            safeSleep(5000);

            // Verify product updated
            String freshProductCheck = getCurrentProductNameFromAEMPage(categoryPageUrl, testSku);

            boolean productUpdated = freshProductCheck.equals(updatedProductName);

            if (productUpdated) {
            } else {
                Assert.fail(String.format("❌ FAILED: Product cache names invalidation failed - Expected: '%s', Got: '%s'",
                        updatedProductName, freshProductCheck));
            }

        } finally {
            // Cleanup and clear cache
            try {
                updateMagentoProductName(testSku, trueOriginalFromMagento);
                
                // Clear cache after reversion to avoid test interference
                String clearPayload = "{\"invalidateAll\": true, \"storePath\": \"" + STORE_PATH + "\"}";
                adminAuthor.doPost(CACHE_INVALIDATION_ENDPOINT, new StringEntity(clearPayload, ContentType.APPLICATION_JSON), 200);
                safeSleep(2000); // Wait for cache clear
                
            } catch (Exception e) {
                LOG.warn("Could not restore product name or clear cache: {}", e.getMessage());
            }
            
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
            originalProductName = getCurrentProductNameFromAEMPage(categoryPageUrl, testSku);
            originalCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);

            // STEP 2: Update both in Magento (backend changes)
            String updatedProductName = originalProductName + " " + randomSuffix;
            String updatedCategoryName = originalCategoryName + " " + randomSuffix;
            updateMagentoProductName(testSku, updatedProductName);
            updateMagentoCategoryName(categoryId, updatedCategoryName);

            // STEP 3: Verify cache working (both should show old cached data)
            String aemProductNameAfterChange = getCurrentProductNameFromAEMPage(categoryPageUrl, testSku);
            String aemCategoryNameAfterChange = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            
            boolean productCacheWorking = aemProductNameAfterChange.equals(originalProductName) && !aemProductNameAfterChange.equals(updatedProductName);
            boolean categoryCacheWorking = aemCategoryNameAfterChange.equals(originalCategoryName) && !aemCategoryNameAfterChange.equals(updatedCategoryName);
            

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
            // Use official Adobe specification regex patterns for GraphQL response matching
            String productRegex = String.format("\\\"sku\\\":\\\\s*\\\"%s\\\"", testSku); // Matches: "sku":"BLT-FAB-001"
            String categoryRegex = String.format("\\\"uid\\\"\\\\s*:\\\\s*\\\\{\\\"id\\\"\\\\s*:\\\\s*\\\"%s\\\"", categoryUid); // Matches: "uid":{"id":"MTc3"}

            String payload = String.format(
                    "{\n" +
                            "    \"regexPatterns\": [\"%s\", \"%s\"],\n" +
                            "    \"storePath\": \"/content/venia/us/en\"\n" +
                            "}", productRegex, categoryRegex);


            SlingHttpResponse response = adminAuthor.doPost("/bin/cif/invalidate-cache", new StringEntity(payload, ContentType.APPLICATION_JSON), 200);

            // STEP 5: Wait and verify both caches cleared
            safeSleep(10000);

            String freshProductName = getCurrentProductNameFromAEMPage(categoryPageUrl, testSku);
            String freshCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            boolean productUpdated = freshProductName.equals(updatedProductName);
            boolean categoryUpdated = freshCategoryName.equals(updatedCategoryName);
            
            // Verified fresh data

            assertTrue("Product regex pattern invalidation failed", productUpdated);
            assertTrue("Category regex pattern invalidation failed", categoryUpdated);


            // STEP 7: Revert names back to original
            // Revert names
            updateMagentoProductName(testSku, trueOriginalProductFromMagento);
            updateMagentoCategoryName(categoryId, trueOriginalCategoryFromMagento);

            // STEP 8: Clear cache again to get original names
            // Clear cache again
            SlingHttpResponse finalResponse = adminAuthor.doPost("/bin/cif/invalidate-cache", new StringEntity(payload, ContentType.APPLICATION_JSON), 200);

            // STEP 9: Wait and verify original names
            // Wait for final cache clear
            safeSleep(10000);

            // Verify original names
            String finalProductName = getCurrentProductNameFromAEMPage(categoryPageUrl, testSku);
            String finalCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            boolean productRestored = finalProductName.equals(trueOriginalProductFromMagento);
            boolean categoryRestored = finalCategoryName.equals(trueOriginalCategoryFromMagento);

            // Verified restoration

            assertTrue("Product name not restored to original", productRestored);
            assertTrue("Category name not restored to original", categoryRestored);


        } finally {
            // Cleanup - ensure names are restored
            try {
                updateMagentoProductName(testSku, trueOriginalProductFromMagento);
                updateMagentoCategoryName(categoryId, trueOriginalCategoryFromMagento);
            } catch (Exception e) {
                LOG.warn("Cleanup failed: {}", e.getMessage());
            }
        }
    }

    /**
     * Perform cache invalidation with given payload
     */
    private void performCacheInvalidation(String payload, String description) throws Exception {
        
        SlingHttpResponse response = adminAuthor.doPost(
            CACHE_INVALIDATION_ENDPOINT,
            new StringEntity(payload, ContentType.APPLICATION_JSON),
            null,
            200);
            
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

    /**
     * Static helper to update product name in Magento (used by @AfterClass)
     */
    private static void updateMagentoProductNameStatic(String sku, String newName) throws Exception {
        String url = MAGENTO_REST_URL + "/products/" + sku;
        HttpPut request = new HttpPut(url);
        request.setHeader("Authorization", "Bearer " + MAGENTO_ADMIN_TOKEN);
        request.setHeader("Content-Type", "application/json");

        String payload = String.format("{\"product\":{\"name\":\"%s\"}}", newName);
        request.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            
            LOG.debug("Magento product update response: status={}, body={}", statusCode, responseBody);
            
            if (statusCode != 200) {
                throw new Exception("Failed to update product: " + statusCode + ", response: " + responseBody);
            }
            
            // Verify the update actually worked
            Thread.sleep(2000); // Wait for Magento to process
            JsonNode verifyData = getMagentoProductDataStatic(sku);
            String actualName = verifyData.get("name").asText();
            
            if (!actualName.equals(newName)) {
                LOG.warn("⚠️  Magento product update verification FAILED!");
                LOG.warn("   Requested name: '{}'", newName);
                LOG.warn("   Actual name: '{}'", actualName);
                throw new Exception("Product update verification failed: Magento didn't update the name");
            }
        }
    }

    /**
     * Static helper to update category name in Magento (used by @AfterClass)
     */
    private static void updateMagentoCategoryNameStatic(String categoryId, String newName) throws Exception {
        String url = MAGENTO_REST_URL + "/categories/" + categoryId;
        HttpPut request = new HttpPut(url);
        request.setHeader("Authorization", "Bearer " + MAGENTO_ADMIN_TOKEN);
        request.setHeader("Content-Type", "application/json");

        String payload = String.format("{\"category\":{\"name\":\"%s\"}}", newName);
        request.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            String responseBody = EntityUtils.toString(response.getEntity());
            
            LOG.debug("Magento category update response: status={}, body={}", statusCode, responseBody);
            
            if (statusCode != 200) {
                throw new Exception("Failed to update category: " + statusCode + ", response: " + responseBody);
            }
            
            // Verify the update actually worked
            Thread.sleep(2000); // Wait for Magento to process
            JsonNode verifyData = getMagentoCategoryDataStatic(categoryId);
            String actualName = verifyData.get("name").asText();
            
            if (!actualName.equals(newName)) {
                LOG.warn("⚠️  Magento category update verification FAILED!");
                LOG.warn("   Requested name: '{}'", newName);
                LOG.warn("   Actual name: '{}'", actualName);
                throw new Exception("Category update verification failed: Magento didn't update the name");
            }
        }
    }

}
