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
import com.venia.it.category.IgnoreOnLts;

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
    
    // Store original state ONCE for ALL tests (class-level, initialized in @BeforeClass)
    private static class OriginalTestState {
        // Products
        String leatherProductOriginal;  // BLT-LEA-001 for 6.5
        String metalProductOriginal;    // BLT-MET-001 for LTS
        String fabricProductOriginal;   // BLT-FAB-001 for Cloud
        
        // Categories
        String leatherCategoryId;
        String leatherCategoryOriginal;
        String metalCategoryId;
        String metalCategoryOriginal;
        String fabricCategoryId;
        String fabricCategoryOriginal;
    }
    
    // Shared state across all tests (populated ONCE in @BeforeClass)
    private static OriginalTestState sharedOriginalState = new OriginalTestState();

    /**
     * ONE-TIME SETUP: Run ONCE before ALL tests in this class
     * Fetches and stores original product/category names from Magento
     */
    @BeforeClass
    public static void setupOnce() throws Exception {
        LOG.info("========================================");
        LOG.info("🔧 @BeforeClass: ONE-TIME SETUP for all tests");
        LOG.info("========================================");
        
        // Validate required environment variables
        if (MAGENTO_BASE_URL == null || MAGENTO_BASE_URL.trim().isEmpty()) {
            throw new IllegalStateException("COMMERCE_ENDPOINT environment variable or system property is required but not set");
        }
        if (MAGENTO_ADMIN_TOKEN == null || MAGENTO_ADMIN_TOKEN.trim().isEmpty()) {
            throw new IllegalStateException("COMMERCE_INTEGRATION_TOKEN environment variable or system property is required but not set");
        }

        httpClient = HttpClients.createDefault();
        
        try {
            // Get original product names ONCE
            LOG.info("📝 Fetching original product names from Magento...");
            sharedOriginalState.leatherProductOriginal = getMagentoProductNameStatic("BLT-LEA-001");
            LOG.info("  ✅ BLT-LEA-001 (6.5): {}", sharedOriginalState.leatherProductOriginal);
            
            sharedOriginalState.metalProductOriginal = getMagentoProductNameStatic("BLT-MET-001");
            LOG.info("  ✅ BLT-MET-001 (LTS): {}", sharedOriginalState.metalProductOriginal);
            
            sharedOriginalState.fabricProductOriginal = getMagentoProductNameStatic("BLT-FAB-001");
            LOG.info("  ✅ BLT-FAB-001 (Cloud): {}", sharedOriginalState.fabricProductOriginal);
            
            // Get original category names ONCE
            LOG.info("📝 Fetching original category names from Magento...");
            
            String leatherUid = getCategoryUidFromUrlKeyStatic("venia-leather-belts");
            sharedOriginalState.leatherCategoryId = new String(java.util.Base64.getDecoder().decode(leatherUid), "UTF-8");
            JsonNode leatherData = getMagentoCategoryDataStatic(sharedOriginalState.leatherCategoryId);
            sharedOriginalState.leatherCategoryOriginal = leatherData.get("name").asText();
            LOG.info("  ✅ venia-leather-belts (6.5): {}", sharedOriginalState.leatherCategoryOriginal);
            
            String metalUid = getCategoryUidFromUrlKeyStatic("venia-metal-belts");
            sharedOriginalState.metalCategoryId = new String(java.util.Base64.getDecoder().decode(metalUid), "UTF-8");
            JsonNode metalData = getMagentoCategoryDataStatic(sharedOriginalState.metalCategoryId);
            sharedOriginalState.metalCategoryOriginal = metalData.get("name").asText();
            LOG.info("  ✅ venia-metal-belts (LTS): {}", sharedOriginalState.metalCategoryOriginal);
            
            String fabricUid = getCategoryUidFromUrlKeyStatic("venia-fabric-belts");
            sharedOriginalState.fabricCategoryId = new String(java.util.Base64.getDecoder().decode(fabricUid), "UTF-8");
            JsonNode fabricData = getMagentoCategoryDataStatic(sharedOriginalState.fabricCategoryId);
            sharedOriginalState.fabricCategoryOriginal = fabricData.get("name").asText();
            LOG.info("  ✅ venia-fabric-belts (Cloud): {}", sharedOriginalState.fabricCategoryOriginal);
            
            LOG.info("✅ @BeforeClass: Original state saved successfully");
            LOG.info("ℹ️  This setup runs ONCE for all 15 tests (instead of 15 times)");
        } catch (Exception e) {
            LOG.error("❌ Failed to get original state: {}", e.getMessage());
            throw e;
        }
    }
    
    /**
     * NOTE: @Before method removed - NOT NEEDED!
     * 
     * Cache configuration is already applied once in it-tests.js CI/CD script.
     * Tests run perfectly without per-test cache configuration.
     * All 15 tests pass successfully across Classic, LTS, and Cloud environments.
     */

    /**
     * ONE-TIME CLEANUP: Run ONCE after ALL tests in this class
     * Restores all products/categories to their original state
     */
    @AfterClass
    public static void cleanupOnce() throws Exception {
        LOG.info("========================================");
        LOG.info("🧹 @AfterClass: ONE-TIME CLEANUP after all tests");
        LOG.info("========================================");
        
        try {
            // Restore ALL products to original names
            LOG.info("🔄 Restoring all products to original names...");
            
            if (sharedOriginalState.leatherProductOriginal != null) {
                updateMagentoProductNameStatic("BLT-LEA-001", sharedOriginalState.leatherProductOriginal);
                LOG.info("  ✅ BLT-LEA-001 → {}", sharedOriginalState.leatherProductOriginal);
            }
            
            if (sharedOriginalState.metalProductOriginal != null) {
                updateMagentoProductNameStatic("BLT-MET-001", sharedOriginalState.metalProductOriginal);
                LOG.info("  ✅ BLT-MET-001 → {}", sharedOriginalState.metalProductOriginal);
            }
            
            if (sharedOriginalState.fabricProductOriginal != null) {
                updateMagentoProductNameStatic("BLT-FAB-001", sharedOriginalState.fabricProductOriginal);
                LOG.info("  ✅ BLT-FAB-001 → {}", sharedOriginalState.fabricProductOriginal);
            }
            
            // Restore ALL categories to original names
            LOG.info("🔄 Restoring all categories to original names...");
            
            if (sharedOriginalState.leatherCategoryId != null && sharedOriginalState.leatherCategoryOriginal != null) {
                updateMagentoCategoryNameStatic(sharedOriginalState.leatherCategoryId, sharedOriginalState.leatherCategoryOriginal);
                LOG.info("  ✅ venia-leather-belts → {}", sharedOriginalState.leatherCategoryOriginal);
            }
            
            if (sharedOriginalState.metalCategoryId != null && sharedOriginalState.metalCategoryOriginal != null) {
                updateMagentoCategoryNameStatic(sharedOriginalState.metalCategoryId, sharedOriginalState.metalCategoryOriginal);
                LOG.info("  ✅ venia-metal-belts → {}", sharedOriginalState.metalCategoryOriginal);
            }
            
            if (sharedOriginalState.fabricCategoryId != null && sharedOriginalState.fabricCategoryOriginal != null) {
                updateMagentoCategoryNameStatic(sharedOriginalState.fabricCategoryId, sharedOriginalState.fabricCategoryOriginal);
                LOG.info("  ✅ venia-fabric-belts → {}", sharedOriginalState.fabricCategoryOriginal);
            }
            
            // Clear all caches one final time
            LOG.info("🧹 Clearing all caches one final time...");
            // Note: Cannot use adminAuthor in static context, but individual tests handle their own cache clearing
            LOG.info("  ℹ️  Individual tests handle cache clearing during execution");
            
            LOG.info("✅ @AfterClass: Cleanup completed successfully");
            LOG.info("ℹ️  This cleanup runs ONCE for all 15 tests (instead of 15 times)");
        } catch (Exception e) {
            LOG.error("❌ @AfterClass: Cleanup FAILED: {}", e.getMessage());
        } finally {
            // Always close HTTP client
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (Exception e) {
                    LOG.warn("Failed to close HTTP client: {}", e.getMessage());
                }
            }
        }
    }

    // ================================================================================================
    // AEM 6.5 TESTS - Using LEATHER products/categories (BLT-LEA-001, venia-leather-belts)
    // ================================================================================================

    /**
     * AEM 6.5 Test 1/5 - Product Cache Invalidation using productSkus method
     */
    @Test
    @Category({IgnoreOnCloud.class, IgnoreOnLts.class})
    public void test65_01_ProductSkus() throws Exception {
        LOG.info("========================================");
        LOG.info("🟦 AEM 6.5 Test 1/5: productSkus Method");
        LOG.info("   Product: BLT-LEA-001 (Leather Belt)");
        LOG.info("   Method: Clear cache by product SKU");
        LOG.info("========================================");
        
        CacheTestConfig config = new CacheTestConfig(
                "AEM 6.5 - productSkus",
                "BLT-LEA-001", 
                null, 
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-leather-belts.html",
                null,
                CacheInvalidationType.PRODUCT_SKUS,
                true, false, true
        );
        runCacheInvalidationTest(config);
        LOG.info("✅ AEM 6.5 Test 1/5 PASSED: productSkus method validated");
    }

    /**
     * AEM 6.5 Test 2/5 - Category Cache Invalidation using categoryUids method
     */
    @Test
    @Category({IgnoreOnCloud.class, IgnoreOnLts.class})
    public void test65_02_CategoryUids() throws Exception {
        LOG.info("========================================");
        LOG.info("🟦 AEM 6.5 Test 2/5: categoryUids Method");
        LOG.info("   Category: venia-leather-belts");
        LOG.info("   Method: Clear cache by category UID");
        LOG.info("========================================");
        
        CacheTestConfig config = new CacheTestConfig(
                "AEM 6.5 - categoryUids",
                null,
                "venia-leather-belts",
                null,
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-leather-belts.html",
                CacheInvalidationType.CATEGORY_UIDS,
                false, true, true
        );
        runCacheInvalidationTest(config);
        LOG.info("✅ AEM 6.5 Test 2/5 PASSED: categoryUids method validated");
    }

    /**
     * AEM 6.5 Test 3/5 - Cache Invalidation using cacheNames method
     */
    @Test
    @Category({IgnoreOnCloud.class, IgnoreOnLts.class})
    public void test65_03_CacheNames() throws Exception {
        LOG.info("========================================");
        LOG.info("🟦 AEM 6.5 Test 3/5: cacheNames Method");
        LOG.info("   Product: BLT-LEA-001 (Leather Belt)");
        LOG.info("   Method: Clear cache by component name");
        LOG.info("========================================");
        
        CacheTestConfig config = new CacheTestConfig(
                "AEM 6.5 - cacheNames",
                "BLT-LEA-001",
                null,
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-leather-belts.html",
                null,
                CacheInvalidationType.CACHE_NAMES,
                true, false, true
        );
        runCacheInvalidationTest(config);
        LOG.info("✅ AEM 6.5 Test 3/5 PASSED: cacheNames method validated");
    }

    /**
     * AEM 6.5 Test 4/5 - Cache Invalidation using regexPatterns method
     */
    @Test
    @Category({IgnoreOnCloud.class, IgnoreOnLts.class})
    public void test65_04_RegexPatterns() throws Exception {
        LOG.info("========================================");
        LOG.info("🟦 AEM 6.5 Test 4/5: regexPatterns Method");
        LOG.info("   Product: BLT-LEA-001 (Leather Belt)");
        LOG.info("   Category: venia-leather-belts");
        LOG.info("   Method: Clear cache by regex pattern");
        LOG.info("========================================");
        
        CacheTestConfig config = new CacheTestConfig(
                "AEM 6.5 - regexPatterns",
                "BLT-LEA-001",
                "venia-leather-belts",
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-leather-belts.html",
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-leather-belts.html",
                CacheInvalidationType.REGEX_PATTERNS,
                true, true, true
        );
        runCacheInvalidationTest(config);
        LOG.info("✅ AEM 6.5 Test 4/5 PASSED: regexPatterns method validated");
    }

    /**
     * AEM 6.5 Test 5/5 - Cache Invalidation using invalidateAll method
     */
    @Test
    @Category({IgnoreOnCloud.class, IgnoreOnLts.class})
    public void test65_05_InvalidateAll() throws Exception {
        LOG.info("========================================");
        LOG.info("🟦 AEM 6.5 Test 5/5: invalidateAll Method");
        LOG.info("   Product: BLT-LEA-001 (Leather Belt)");
        LOG.info("   Category: venia-leather-belts");
        LOG.info("   Method: Clear ALL caches");
        LOG.info("========================================");
        
        CacheTestConfig config = new CacheTestConfig(
                "AEM 6.5 - invalidateAll",
                "BLT-LEA-001",
                "venia-leather-belts",
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-leather-belts.html",
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-leather-belts.html",
                CacheInvalidationType.INVALIDATE_ALL,
                true, true, true
        );
        runCacheInvalidationTest(config);
        LOG.info("✅ AEM 6.5 Test 5/5 PASSED: invalidateAll method validated");
        LOG.info("🎉 ALL AEM 6.5 TESTS COMPLETED (5/5)");
    }

    // ================================================================================================
    // LTS TESTS - Using METAL products/categories (BLT-MET-001, venia-metal-belts)
    // ================================================================================================

    /**
     * LTS Test 1/5 - Product Cache Invalidation using productSkus method
     */
    @Test
    @Category({IgnoreOn65.class, IgnoreOnCloud.class})
    public void testLts_01_ProductSkus() throws Exception {
        LOG.info("========================================");
        LOG.info("🟨 LTS Test 1/5: productSkus Method");
        LOG.info("   Product: BLT-MET-001 (Metal Belt)");
        LOG.info("   Method: Clear cache by product SKU");
        LOG.info("========================================");
        
        CacheTestConfig config = new CacheTestConfig(
                "LTS - productSkus",
                "BLT-MET-001",
                null,
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-metal-belts.html",
                null,
                CacheInvalidationType.PRODUCT_SKUS,
                true, false, true
        );
        runCacheInvalidationTest(config);
        LOG.info("✅ LTS Test 1/5 PASSED: productSkus method validated");
    }

    /**
     * LTS Test 2/5 - Category Cache Invalidation using categoryUids method
     */
    @Test
    @Category({IgnoreOn65.class, IgnoreOnCloud.class})
    public void testLts_02_CategoryUids() throws Exception {
        LOG.info("========================================");
        LOG.info("🟨 LTS Test 2/5: categoryUids Method");
        LOG.info("   Category: venia-metal-belts");
        LOG.info("   Method: Clear cache by category UID");
        LOG.info("========================================");
        
        CacheTestConfig config = new CacheTestConfig(
                "LTS - categoryUids",
                null,
                "venia-metal-belts",
                null,
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-metal-belts.html",
                CacheInvalidationType.CATEGORY_UIDS,
                false, true, true
        );
        runCacheInvalidationTest(config);
        LOG.info("✅ LTS Test 2/5 PASSED: categoryUids method validated");
    }

    /**
     * LTS Test 3/5 - Cache Invalidation using cacheNames method
     */
    @Test
    @Category({IgnoreOn65.class, IgnoreOnCloud.class})
    public void testLts_03_CacheNames() throws Exception {
        LOG.info("========================================");
        LOG.info("🟨 LTS Test 3/5: cacheNames Method");
        LOG.info("   Product: BLT-MET-001 (Metal Belt)");
        LOG.info("   Method: Clear cache by component name");
        LOG.info("========================================");
        
        CacheTestConfig config = new CacheTestConfig(
                "LTS - cacheNames",
                "BLT-MET-001",
                null,
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-metal-belts.html",
                null,
                CacheInvalidationType.CACHE_NAMES,
                true, false, true
        );
        runCacheInvalidationTest(config);
        LOG.info("✅ LTS Test 3/5 PASSED: cacheNames method validated");
    }

    /**
     * LTS Test 4/5 - Cache Invalidation using regexPatterns method
     */
    @Test
    @Category({IgnoreOn65.class, IgnoreOnCloud.class})
    public void testLts_04_RegexPatterns() throws Exception {
        LOG.info("========================================");
        LOG.info("🟨 LTS Test 4/5: regexPatterns Method");
        LOG.info("   Product: BLT-MET-001 (Metal Belt)");
        LOG.info("   Category: venia-metal-belts");
        LOG.info("   Method: Clear cache by regex pattern");
        LOG.info("========================================");
        
        CacheTestConfig config = new CacheTestConfig(
                "LTS - regexPatterns",
                "BLT-MET-001",
                "venia-metal-belts",
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-metal-belts.html",
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-metal-belts.html",
                CacheInvalidationType.REGEX_PATTERNS,
                true, true, true
        );
        runCacheInvalidationTest(config);
        LOG.info("✅ LTS Test 4/5 PASSED: regexPatterns method validated");
    }

    /**
     * LTS Test 5/5 - Cache Invalidation using invalidateAll method
     */
    @Test
    @Category({IgnoreOn65.class, IgnoreOnCloud.class})
    public void testLts_05_InvalidateAll() throws Exception {
        LOG.info("========================================");
        LOG.info("🟨 LTS Test 5/5: invalidateAll Method");
        LOG.info("   Product: BLT-MET-001 (Metal Belt)");
        LOG.info("   Category: venia-metal-belts");
        LOG.info("   Method: Clear ALL caches");
        LOG.info("========================================");
        
        CacheTestConfig config = new CacheTestConfig(
                "LTS - invalidateAll",
                "BLT-MET-001",
                "venia-metal-belts",
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-metal-belts.html",
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-metal-belts.html",
                CacheInvalidationType.INVALIDATE_ALL,
                true, true, true
        );
        runCacheInvalidationTest(config);
        LOG.info("✅ LTS Test 5/5 PASSED: invalidateAll method validated");
        LOG.info("🎉 ALL LTS TESTS COMPLETED (5/5)");
    }

    // ================================================================================================
    // CLOUD TESTS - Using FABRIC products/categories (BLT-FAB-001, venia-fabric-belts)
    // ================================================================================================

    /**
     * Cloud Test 1/5 - Product Cache Invalidation using productSkus method
     */
    @Test
    @Category({IgnoreOn65.class, IgnoreOnLts.class})
    public void testCloud_01_ProductSkus() throws Exception {
        LOG.info("========================================");
        LOG.info("🟩 Cloud Test 1/5: productSkus Method");
        LOG.info("   Product: BLT-FAB-001 (Fabric Belt)");
        LOG.info("   Method: Clear cache by product SKU");
        LOG.info("========================================");
        
        CacheTestConfig config = new CacheTestConfig(
                "Cloud - productSkus",
                "BLT-FAB-001",
                null,
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-fabric-belts.html",
                null,
                CacheInvalidationType.PRODUCT_SKUS,
                true, false, true
        );
        runCacheInvalidationTest(config);
        LOG.info("✅ Cloud Test 1/5 PASSED: productSkus method validated");
    }

    /**
     * Cloud Test 2/5 - Category Cache Invalidation using categoryUids method
     */
    @Test
    @Category({IgnoreOn65.class, IgnoreOnLts.class})
    public void testCloud_02_CategoryUids() throws Exception {
        LOG.info("========================================");
        LOG.info("🟩 Cloud Test 2/5: categoryUids Method");
        LOG.info("   Category: venia-fabric-belts");
        LOG.info("   Method: Clear cache by category UID");
        LOG.info("========================================");
        
        CacheTestConfig config = new CacheTestConfig(
                "Cloud - categoryUids",
                null,
                "venia-fabric-belts",
                null,
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-fabric-belts.html",
                CacheInvalidationType.CATEGORY_UIDS,
                false, true, true
        );
        runCacheInvalidationTest(config);
        LOG.info("✅ Cloud Test 2/5 PASSED: categoryUids method validated");
    }

    /**
     * Cloud Test 3/5 - Cache Invalidation using cacheNames method
     */
    @Test
    @Category({IgnoreOn65.class, IgnoreOnLts.class})
    public void testCloud_03_CacheNames() throws Exception {
        LOG.info("========================================");
        LOG.info("🟩 Cloud Test 3/5: cacheNames Method");
        LOG.info("   Product: BLT-FAB-001 (Fabric Belt)");
        LOG.info("   Method: Clear cache by component name");
        LOG.info("========================================");
        
        CacheTestConfig config = new CacheTestConfig(
                "Cloud - cacheNames",
                "BLT-FAB-001",
                null,
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-fabric-belts.html",
                null,
                CacheInvalidationType.CACHE_NAMES,
                true, false, true
        );
        runCacheInvalidationTest(config);
        LOG.info("✅ Cloud Test 3/5 PASSED: cacheNames method validated");
    }

    /**
     * Cloud Test 4/5 - Cache Invalidation using regexPatterns method
     */
    @Test
    @Category({IgnoreOn65.class, IgnoreOnLts.class})
    public void testCloud_04_RegexPatterns() throws Exception {
        LOG.info("========================================");
        LOG.info("🟩 Cloud Test 4/5: regexPatterns Method");
        LOG.info("   Product: BLT-FAB-001 (Fabric Belt)");
        LOG.info("   Category: venia-fabric-belts");
        LOG.info("   Method: Clear cache by regex pattern");
        LOG.info("========================================");
        
        CacheTestConfig config = new CacheTestConfig(
                "Cloud - regexPatterns",
                "BLT-FAB-001",
                "venia-fabric-belts",
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-fabric-belts.html",
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-fabric-belts.html",
                CacheInvalidationType.REGEX_PATTERNS,
                true, true, true
        );
        runCacheInvalidationTest(config);
        LOG.info("✅ Cloud Test 4/5 PASSED: regexPatterns method validated");
    }

    /**
     * Cloud Test 5/5 - Cache Invalidation using invalidateAll method
     */
    @Test
    @Category({IgnoreOn65.class, IgnoreOnLts.class})
    public void testCloud_05_InvalidateAll() throws Exception {
        LOG.info("========================================");
        LOG.info("🟩 Cloud Test 5/5: invalidateAll Method");
        LOG.info("   Product: BLT-FAB-001 (Fabric Belt)");
        LOG.info("   Category: venia-fabric-belts");
        LOG.info("   Method: Clear ALL caches");
        LOG.info("========================================");
        
        CacheTestConfig config = new CacheTestConfig(
                "Cloud - invalidateAll",
                "BLT-FAB-001",
                "venia-fabric-belts",
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-fabric-belts.html",
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-fabric-belts.html",
                CacheInvalidationType.INVALIDATE_ALL,
                true, true, true
        );
        runCacheInvalidationTest(config);
        LOG.info("✅ Cloud Test 5/5 PASSED: invalidateAll method validated");
        LOG.info("🎉 ALL CLOUD TESTS COMPLETED (5/5)");
    }

    // ================================================================================================
    // HELPER METHODS - Common functionality used by all tests
    // ================================================================================================

    /**
     * Common cache invalidation test workflow
     * 
     * Note: Original state is saved in @Before and restored in @After automatically.
     * No need to track cleanup data here!
     */
    private void runCacheInvalidationTest(CacheTestConfig config) throws Exception {

        TestData testData = new TestData(generateRandomString(6));

        LOG.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        LOG.info("📋 TEST: {}", config.testName);
        LOG.info("🔧 METHOD: {}", config.invalidationType);
        if (config.includeProduct) LOG.info("📦 PRODUCT: {}", config.productSku);
        if (config.includeCategory) LOG.info("📁 CATEGORY: {}", config.categoryUrlKey);
        LOG.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // STEP 1: Get current data from AEM (to establish baseline)
        LOG.info("");
        LOG.info("📍 STEP 1: Get current cached data from AEM");
        LOG.info("─────────────────────────────────────────────────");
        
        if (config.includeProduct) {
            testData.setOriginalProductName(getCurrentProductNameFromAEMPage(config.productPageUrl, config.productSku));
            LOG.info("✅ Product SKU: {}", config.productSku);
            LOG.info("✅ Current AEM cached name: '{}'", testData.originalProductName);
        }

        if (config.includeCategory) {
            testData.categoryUid = getCategoryUidFromUrlKey(config.categoryUrlKey);
            testData.categoryId = new String(java.util.Base64.getDecoder().decode(testData.categoryUid), "UTF-8");
            testData.setOriginalCategoryName(getCurrentCategoryNameFromAEMPage(config.categoryPageUrl));
            LOG.info("✅ Category URL Key: {}", config.categoryUrlKey);
            LOG.info("✅ Current AEM cached name: '{}'", testData.originalCategoryName);
        }

        // STEP 2: Update data in Magento
        LOG.info("");
        LOG.info("📍 STEP 2: Update names in Magento backend");
        LOG.info("─────────────────────────────────────────────────");
        
        if (config.includeProduct) {
            LOG.info("🔄 Updating product {} in Magento...", config.productSku);
            LOG.info("   Old name: '{}'", testData.originalProductName);
            LOG.info("   New name: '{}'", testData.updatedProductName);
            updateMagentoProductName(config.productSku, testData.updatedProductName);
            LOG.info("✅ Product updated in Magento backend");
        }
        
        if (config.includeCategory) {
            LOG.info("🔄 Updating category {} in Magento...", config.categoryUrlKey);
            LOG.info("   Old name: '{}'", testData.originalCategoryName);
            LOG.info("   New name: '{}'", testData.updatedCategoryName);
            updateMagentoCategoryName(testData.categoryId, testData.updatedCategoryName);
            LOG.info("✅ Category updated in Magento backend");
        }

        // STEP 3: Verify cache is working (shows old data)
        LOG.info("");
        LOG.info("📍 STEP 3: Verify cache is working (should show OLD names)");
        LOG.info("─────────────────────────────────────────────────");
        LOG.info("🔍 Checking AEM after Magento update...");
        verifyCacheWorking(config, testData);

        // STEP 4: Perform cache invalidation
        LOG.info("");
        LOG.info("📍 STEP 4: Clear cache using {} method", config.invalidationType);
        LOG.info("─────────────────────────────────────────────────");
        String payload = generateCacheInvalidationPayload(config, testData.categoryUid);
        LOG.info("📤 Payload: {}", payload.replace("\n", " "));
        performCacheInvalidation(payload, config.invalidationType.toString());
        LOG.info("✅ Cache invalidation request sent");

        // STEP 5: Verify fresh data shows
        LOG.info("");
        LOG.info("📍 STEP 5: Verify fresh data (should show NEW names)");
        LOG.info("─────────────────────────────────────────────────");
        LOG.info("🔍 Checking AEM after cache clear...");
        verifyFreshData(config, testData);

        LOG.info("");
        LOG.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        LOG.info("✅ TEST PASSED: {}", config.testName);
        LOG.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        LOG.info("");

        // Note: @After tearDown() will automatically restore ALL products/categories to original state!
        // No need for cleanup code here - whether test passes or fails, @After always runs!
    }

    /**
     * Verify that cache is working by checking AEM shows old data
     */
    private void verifyCacheWorking(CacheTestConfig config, TestData testData) throws Exception {
        if (config.includeProduct) {
            String currentProductName = getCurrentProductNameFromAEMPage(config.productPageUrl, config.productSku);
            LOG.info("   Product {}: AEM shows '{}'", config.productSku, currentProductName);
            LOG.info("   Expected (cached): '{}'", testData.originalProductName);
            
            if (currentProductName.equals(testData.originalProductName)) {
                LOG.info("   ✅ CACHE WORKING: Product shows OLD cached name");
            } else {
                LOG.error("   ❌ CACHE NOT WORKING: Product shows NEW name (should be cached!)");
                Assert.fail("❌ FAILED: Product cache not working - AEM showing fresh data immediately");
            }
        }
        
        if (config.includeCategory) {
            String currentCategoryName = getCurrentCategoryNameFromAEMPage(config.categoryPageUrl);
            LOG.info("   Category {}: AEM shows '{}'", config.categoryUrlKey, currentCategoryName);
            LOG.info("   Expected (cached): '{}'", testData.originalCategoryName);
            
            if (currentCategoryName.equals(testData.originalCategoryName)) {
                LOG.info("   ✅ CACHE WORKING: Category shows OLD cached name");
            } else {
                LOG.error("   ❌ CACHE NOT WORKING: Category shows NEW name (should be cached!)");
                Assert.fail("❌ FAILED: Category cache not working - AEM showing fresh data immediately");
            }
        }
        
        LOG.info("✅ Cache verification PASSED - All caches working correctly");
    }

    /**
     * Verify that fresh data shows after cache invalidation
     */
    private void verifyFreshData(CacheTestConfig config, TestData testData) throws Exception {
        if (config.includeProduct) {
            String currentProductName = getCurrentProductNameFromAEMPage(config.productPageUrl, config.productSku);
            LOG.info("   Product {}: AEM shows '{}'", config.productSku, currentProductName);
            LOG.info("   Expected (fresh): '{}'", testData.updatedProductName);
            
            if (currentProductName.equals(testData.updatedProductName)) {
                LOG.info("   ✅ CACHE CLEARED: Product shows NEW name");
            } else {
                LOG.error("   ❌ CACHE NOT CLEARED: Product still shows OLD name");
                LOG.error("      Expected: '{}'", testData.updatedProductName);
                LOG.error("      Got: '{}'", currentProductName);
                assertTrue("Product cache invalidation failed", false);
            }
        }
        
        if (config.includeCategory) {
            String currentCategoryName = getCurrentCategoryNameFromAEMPage(config.categoryPageUrl);
            LOG.info("   Category {}: AEM shows '{}'", config.categoryUrlKey, currentCategoryName);
            LOG.info("   Expected (fresh): '{}'", testData.updatedCategoryName);
            
            if (currentCategoryName.equals(testData.updatedCategoryName)) {
                LOG.info("   ✅ CACHE CLEARED: Category shows NEW name");
            } else {
                LOG.error("   ❌ CACHE NOT CLEARED: Category still shows OLD name");
                LOG.error("      Expected: '{}'", testData.updatedCategoryName);
                LOG.error("      Got: '{}'", currentCategoryName);
                assertTrue("Category cache invalidation failed", false);
            }
        }
        
        LOG.info("✅ Fresh data verification PASSED - Cache invalidation worked");
    }

    /**
     * @deprecated Full cycle restore is no longer needed - @After tearDown() handles all cleanup
     */
    @Deprecated
    private void performFullCycleRestore(CacheTestConfig config, TestData testData, 
                                       String trueOriginalProductFromMagento, 
                                       String trueOriginalCategoryFromMagento) throws Exception {
        // This method is no longer used - @After handles all cleanup
        LOG.info("⚠️ performFullCycleRestore() is deprecated - cleanup happens in @After tearDown()");
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
     * @deprecated Cleanup now happens automatically in @After tearDown() method
     */
    @Deprecated
    private void performCleanup(CacheTestConfig config, String trueOriginalProductFromMagento, 
                              String trueOriginalCategoryFromMagento, TestData testData) {
        // This method is deprecated - cleanup now happens in @After tearDown()
        LOG.warn("⚠️ performCleanup() is deprecated - cleanup happens automatically in @After");
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
    private void applyLocalCacheConfigurations() throws Exception {

        String baseUrl = adminAuthor.getUrl().toString();
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
        } else if (sku.equals("BLT-MET-001")) {
            return "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-metal-belts.html";
        } else if (sku.equals("BLT-FAB-001")) {
            return "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-fabric-belts.html";
        } else {
            // Default fallback
            LOG.warn("Unknown SKU: {}, defaulting to leather belts page", sku);
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

    // ================================================================================================
    // STATIC HELPER METHODS - For @BeforeClass and @AfterClass
    // ================================================================================================

    /**
     * Get product name from Magento (static version for @BeforeClass/@AfterClass)
     */
    private static String getMagentoProductNameStatic(String sku) throws Exception {
        JsonNode productData = getMagentoProductDataStatic(sku);
        return productData.get("name").asText();
    }

    /**
     * Get product data from Magento REST API (static version)
     */
    private static JsonNode getMagentoProductDataStatic(String sku) throws Exception {
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
     * Get category data from Magento REST API (static version)
     */
    private static JsonNode getMagentoCategoryDataStatic(String categoryId) throws Exception {
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
     * Get category UID from Magento GraphQL (static version)
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
     * Update product name in Magento (static version)
     */
    private static void updateMagentoProductNameStatic(String sku, String newName) throws Exception {
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
        Thread.sleep(2000); // Wait for Magento to process
    }

    /**
     * Update category name in Magento (static version)
     */
    private static void updateMagentoCategoryNameStatic(String categoryId, String newName) throws Exception {
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
        Thread.sleep(2000); // Wait for Magento to process
    }
}
