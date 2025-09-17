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

    private static final Logger LOG = LoggerFactory.getLogger(CacheInvalidationWorkflowIT.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Magento Configuration
    private static final String MAGENTO_BASE_URL = "https://mcprod.catalogservice-commerce.fun";
    private static final String MAGENTO_REST_URL = MAGENTO_BASE_URL + "/rest/V1";
    private static final String MAGENTO_ADMIN_TOKEN = "etk0tf7974shom72dyphbxqxsqd2eqe5";
    private static final String CACHE_INVALIDATION_ENDPOINT = "/bin/cif/invalidate-cache";
    private static final String STORE_PATH = "/content/venia/us/en";

    private CloseableHttpClient httpClient;

    @Before
    public void setUp() throws Exception {
        httpClient = HttpClients.createDefault();
        LOG.info("=== CACHE INVALIDATION WORKFLOW TEST SETUP ===");
        LOG.info("🌍 Magento URL: {}", MAGENTO_BASE_URL);
        
        // Only apply configurations if running in local development (not CI)
        String isCI = System.getProperty("sling.it.instances"); // CI sets this property
        if (isCI == null) {
            LOG.info("🔧 Local development detected - applying cache configurations");
            try {
                applyLocalCacheConfigurations();
            } catch (Exception e) {
                LOG.warn("⚠️ Failed to apply local cache configurations: {}", e.getMessage());
                LOG.warn("⚠️ Cache may not work properly in this test run");
            }
        } else {
            LOG.info("🚀 CI environment detected - using pre-configured cache settings");
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
    public void test65_Product_CacheInvalidation() throws Exception {
        LOG.info("=== 🎯 AEM 6.5 - PRODUCT CACHE INVALIDATION (productSkus) ===");
        runProductCacheInvalidationTest(
                "BLT-LEA-001", // SKU
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-leather-belts.html", // Category page
                "AEM 6.5 - Product (productSkus method)"
        );
    }

    /**
     * AEM 6.5 - Category Cache Invalidation using categoryUids method
     */
    @Test
    @Category(IgnoreOnCloud.class)
    public void test65_Category_CacheInvalidation() throws Exception {
        LOG.info("=== 🎯 AEM 6.5 - CATEGORY CACHE INVALIDATION (categoryUids) ===");
        runCategoryCacheInvalidationTest(
                "BLT-LEA-001", // SKU for context  
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-leather-belts.html", // Category page
                "venia-leather-belts", // URL key
                "AEM 6.5 - Category (categoryUids method)"
        );
    }


    /**
     * Cloud - Product Cache Invalidation using regexPatterns method
     */
    @Test 
    @Category(IgnoreOn65.class)
    public void testCloud_Product_RegexPattern() throws Exception {
        LOG.info("=== 🎯 CLOUD - PRODUCT CACHE INVALIDATION (regexPatterns) ===");
        String testSku = "BLT-LEA-001"; // Different product for variety
        String categoryPage = "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-leather-belts.html";
        String environment = "Cloud - Product (regexPatterns method)";

        // Run product test using regex pattern instead of productSkus
        runRegexPatternProductTest(environment, testSku, categoryPage);
    }


    /**
     * Cloud - Product Cache Invalidation using cacheNames method (5th cache type)
     */
    @Test
    @Category(IgnoreOn65.class) 
    public void testCloud_Product_CacheNames() throws Exception {
        LOG.info("=== 🎯 CLOUD - PRODUCT CACHE INVALIDATION (cacheNames) ===");
        String testSku = "BLT-LEA-001";
        String categoryPage = "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-leather-belts.html";
        String environment = "Cloud - Product (cacheNames method)";
        
        // Warm up cache for Cloud environment
        warmUpCache(testSku, categoryPage);
        
        // Run product test using cacheNames from GraphQL client configuration
        runCacheNamesProductTest(environment, testSku, categoryPage);
    }

    /**
     * Cloud - Final Comprehensive Test using invalidateAll method
     */
    @Test
    @Category(IgnoreOn65.class)
    public void testCloud_InvalidateAll_Final() throws Exception {
        LOG.info("=== 🎯 CLOUD - COMPREHENSIVE CACHE INVALIDATION (invalidateAll) ===");
        String testSku = "BLT-FAB-001"; 
        String categoryPage = "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-fabric-belts.html";
        String categoryUrlKey = "venia-fabric-belts";
        String environment = "Cloud - Final Test (invalidateAll method)";

        // Warm up cache for Cloud environment
        warmUpCache(testSku, categoryPage);

        // Test complete cache invalidation with both product and category
        runInvalidateAllCacheTest(environment, testSku, categoryPage, categoryUrlKey);
    }

    /**
     * Product cache invalidation test workflow
     */
    private void runProductCacheInvalidationTest(String productSku, String categoryPageUrl, String environment) throws Exception {
        LOG.info("=== PRODUCT CACHE INVALIDATION TEST - {} ===", environment);
        LOG.info("🎯 SKU: {}", productSku);
        LOG.info("📂 Category Page: {}", categoryPageUrl);

        String originalProductName = null;
        String randomSuffix = generateRandomString(6);

        try {
            // STEP 1: Get product name from Magento
            LOG.info("📋 STEP 1: Getting original product name from Magento");
            JsonNode productData = getMagentoProductData(productSku);
            originalProductName = productData.get("name").asText();
            LOG.info("   ✓ Magento Product Name: '{}'", originalProductName);

            // STEP 2: Update product name in Magento
            String updatedProductName = originalProductName + " " + randomSuffix;
            LOG.info("🔄 STEP 2: Updating Magento product name");
            updateMagentoProductName(productSku, updatedProductName);
            LOG.info("   ✓ Updated Magento Product: '{}'", updatedProductName);

            // STEP 3: Verify AEM still shows old data (cache working)
            LOG.info("📋 STEP 3: Checking AEM still shows cached data");
            String aemProductName = getCurrentProductNameFromAEMPage(categoryPageUrl, productSku);
            LOG.info("   AEM Product Shows: '{}'", aemProductName);
            LOG.info("   Updated Magento Product: '{}'", updatedProductName);
            boolean productCacheWorking = !aemProductName.equals(updatedProductName);
            LOG.info("   Product Cache Working: {}", productCacheWorking ? "✅ YES" : "❌ NO");
            
            // COMPREHENSIVE DIAGNOSTICS: Check AEM Configuration Status
            if (!productCacheWorking) {
                LOG.warn("🔍 DEBUG: Product cache not working - AEM showing fresh data immediately");
                LOG.warn("   - Original Magento: '{}'", originalProductName);
                LOG.warn("   - Updated Magento: '{}'", updatedProductName);  
                LOG.warn("   - AEM Shows: '{}'", aemProductName);
                LOG.warn("   - Environment: {}", environment);
                LOG.warn("   - This indicates Data Service cache is not active");
                
                // DIAGNOSTIC 1: Check GraphQL Data Service Configuration
                LOG.warn("🔧 DIAGNOSTIC 1: Checking AEM GraphQL Data Service Configuration...");
                try {
                    String configUrl = adminAuthor.getUrl() + "/system/console/configMgr/com.adobe.cq.commerce.graphql.magento.GraphqlDataServiceImpl~default";
                    SlingHttpResponse configResponse = adminAuthor.doGet(configUrl, 200);
                    String configContent = configResponse.getContent();
                    boolean hasDataServiceConfig = configContent.contains("productCachingEnabled");
                    boolean productCachingEnabled = configContent.contains("productCachingEnabled</td>") && configContent.contains("true");
                    LOG.warn("   📊 Data Service Config Found: {} {}", hasDataServiceConfig ? "✅" : "❌", hasDataServiceConfig ? "YES" : "NO");
                    if (hasDataServiceConfig) {
                        LOG.warn("   📊 Product Caching Enabled: {} {}", productCachingEnabled ? "✅" : "❌", productCachingEnabled ? "YES" : "NO");
                        if (configContent.contains("productCachingTimeMinutes")) {
                            LOG.warn("   📊 Config contains cache timing settings");
                        }
                    }
                } catch (Exception e) {
                    LOG.warn("   ❌ Failed to check Data Service config: {}", e.getMessage());
                }
                
                // DIAGNOSTIC 2: Check GraphQL Client Configuration (should be working since category works)
                LOG.warn("🔧 DIAGNOSTIC 2: Checking AEM GraphQL Client Configuration...");
                try {
                    String clientConfigUrl = adminAuthor.getUrl() + "/system/console/configMgr/com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl~default";
                    SlingHttpResponse clientResponse = adminAuthor.doGet(clientConfigUrl, 200);
                    String clientContent = clientResponse.getContent();
                    boolean hasClientConfig = clientContent.contains("cacheConfigurations");
                    boolean hasProductCache = clientContent.contains("venia/components/commerce/product");
                    LOG.warn("   📊 GraphQL Client Config Found: {} {}", hasClientConfig ? "✅" : "❌", hasClientConfig ? "YES" : "NO");
                    if (hasClientConfig) {
                        LOG.warn("   📊 Product Component Cache Config: {} {}", hasProductCache ? "✅" : "❌", hasProductCache ? "YES" : "NO");
                    }
                } catch (Exception e) {
                    LOG.warn("   ❌ Failed to check GraphQL Client config: {}", e.getMessage());
                }
                
                // DIAGNOSTIC 3: Check OSGi Service Status
                LOG.warn("🔧 DIAGNOSTIC 3: Checking OSGi Service Status...");
                try {
                    String servicesUrl = adminAuthor.getUrl() + "/system/console/services.json";
                    SlingHttpResponse servicesResponse = adminAuthor.doGet(servicesUrl, 200);
                    String servicesContent = servicesResponse.getContent();
                    boolean dataServiceActive = servicesContent.contains("GraphqlDataServiceImpl") && servicesContent.contains("ACTIVE");
                    LOG.warn("   📊 GraphQL Data Service Status: {} {}", dataServiceActive ? "✅ ACTIVE" : "❌ INACTIVE", dataServiceActive ? "RUNNING" : "NOT RUNNING");
                } catch (Exception e) {
                    LOG.warn("   ❌ Failed to check OSGi services: {}", e.getMessage());
                }
                
                // DIAGNOSTIC 4: Check for Multiple Data Service Configurations
                LOG.warn("🔧 DIAGNOSTIC 4: Checking for Multiple Data Service Configurations...");
                try {
                    String allConfigsUrl = adminAuthor.getUrl() + "/system/console/configMgr.json";
                    SlingHttpResponse allConfigsResponse = adminAuthor.doGet(allConfigsUrl, 200);
                    String allConfigsContent = allConfigsResponse.getContent();
                    int dataServiceConfigCount = (allConfigsContent.split("GraphqlDataServiceImpl", -1).length - 1);
                    LOG.warn("   📊 Total GraphQL Data Service Configs Found: {}", dataServiceConfigCount);
                    if (dataServiceConfigCount > 1) {
                        LOG.warn("   ⚠️  Multiple configurations detected - potential conflict!");
                    } else if (dataServiceConfigCount == 0) {
                        LOG.warn("   ❌ NO Data Service configurations found!");
                    }
                } catch (Exception e) {
                    LOG.warn("   ❌ Failed to check config count: {}", e.getMessage());
                }
                
                // DIAGNOSTIC 5: Test Direct GraphQL Query to Magento
                LOG.warn("🔧 DIAGNOSTIC 5: Testing Direct GraphQL Connection to Magento...");
                try {
                    // Test if we can query Magento directly
                    JsonNode productData2 = getMagentoProductData(productSku);
                    String magentoTestName = productData2 != null ? productData2.get("name").asText() : null;
                    boolean magentoConnected = magentoTestName != null && !magentoTestName.isEmpty();
                    LOG.warn("   📊 Direct Magento Connection: {} {}", magentoConnected ? "✅" : "❌", magentoConnected ? "WORKING" : "FAILED");
                    if (magentoConnected) {
                        LOG.warn("   📊 Magento Response: '{}'", magentoTestName);
                        boolean magentoDifferent = !magentoTestName.equals(aemProductName);
                        LOG.warn("   📊 AEM vs Magento Difference: {} {}", magentoDifferent ? "✅" : "❌", magentoDifferent ? "DIFFERENT DATA" : "SAME DATA");
                    }
                } catch (Exception e) {
                    LOG.warn("   ❌ Failed to test Magento connection: {}", e.getMessage());
                }
                
                // FAIL THE TEST - Cache is not working (prevents false positives)
                Assert.fail("❌ TEST FAILED: Product cache is not working! AEM is showing fresh data immediately instead of cached data. " +
                           "Expected AEM to show cached data: '" + originalProductName + "', but got fresh data: '" + aemProductName + "'. " +
                           "Environment: " + environment + ". Check diagnostic logs above for configuration issues.");
            }

            // STEP 4: Call cache invalidation (product only)
            LOG.info("🚀 STEP 4: Calling cache invalidation servlet for PRODUCT only");
            boolean cacheInvalidated = callCacheInvalidationServlet(productSku, null);
            assertTrue("Cache invalidation servlet call failed", cacheInvalidated);

            // STEP 5: Wait for cache invalidation
            LOG.info("⏳ STEP 5: Waiting for cache invalidation...");
            Thread.sleep(10000); // Wait 10 seconds

            // STEP 6: Verify AEM now shows fresh product data
            LOG.info("🔍 STEP 6: Checking AEM now shows fresh product data");
            String freshProductName = getCurrentProductNameFromAEMPage(categoryPageUrl, productSku);
            LOG.info("   Fresh Product Check: '{}'", freshProductName);
            boolean productUpdated = freshProductName.equals(updatedProductName);
            LOG.info("   Product Updated: {}", productUpdated ? "✅ YES" : "❌ NO");

            assertTrue("Product cache invalidation failed - AEM not showing fresh data", productUpdated);
            LOG.info("🎉 SUCCESS: Product cache invalidation test passed!");

        } finally {
            // Restore original product name
            if (originalProductName != null) {
                try {
                    updateMagentoProductName(productSku, originalProductName);
                    LOG.info("🔄 Restored product name: {}", originalProductName);
                } catch (Exception e) {
                    LOG.warn("Could not restore product name: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Category cache invalidation test workflow
     */
    private void runCategoryCacheInvalidationTest(String productSku, String categoryPageUrl, String categoryUrlKey, String environment) throws Exception {
        LOG.info("=== CATEGORY CACHE INVALIDATION TEST - {} ===", environment);
        LOG.info("🎯 SKU: {}", productSku);
        LOG.info("📂 Category Page: {}", categoryPageUrl);
        LOG.info("🔑 Category URL Key: {}", categoryUrlKey);

        String originalCategoryName = null;
        String categoryId = null;
        String randomSuffix = generateRandomString(6);

        try {
            // STEP 1: Get category name from AEM
            LOG.info("📋 STEP 1: Getting original category name from AEM");
            String aemCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            LOG.info("   ✅ AEM Category Name: '{}'", aemCategoryName);

            // STEP 2: Get category data from Magento using GraphQL
            LOG.info("🔍 STEP 2: Getting category data from Magento GraphQL");
            String categoryUid = getCategoryUidFromUrlKey(categoryUrlKey);

            // Extract category ID from UID (Base64 decode)
            try {
                categoryId = new String(java.util.Base64.getDecoder().decode(categoryUid), "UTF-8");
                LOG.info("   Category ID from UID: '{}'", categoryId);
            } catch (Exception e) {
                LOG.warn("Could not decode category UID '{}': {}", categoryUid, e.getMessage());
                categoryId = categoryUid; // fallback
            }

            // Get original name from Magento
            JsonNode categoryData = getMagentoCategoryData(categoryId);
            originalCategoryName = categoryData.get("name").asText();
            LOG.info("   ✓ Magento Category Name: '{}'", originalCategoryName);

            // STEP 3: Update category name in Magento
            String updatedCategoryName = originalCategoryName + " " + randomSuffix;
            LOG.info("🔄 STEP 3: Updating Magento category name");
            updateMagentoCategoryName(categoryId, updatedCategoryName);
            LOG.info("   ✓ Updated Magento Category: '{}'", updatedCategoryName);

            // STEP 4: Verify AEM still shows old data (cache working)
            LOG.info("📋 STEP 4: Checking AEM still shows cached data");
            String currentAemCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            LOG.info("   AEM Category Shows: '{}'", currentAemCategoryName);
            LOG.info("   Updated Magento Category: '{}'", updatedCategoryName);
            boolean categoryCacheWorking = !currentAemCategoryName.equals(updatedCategoryName);
            LOG.info("   Category Cache Working: {}", categoryCacheWorking ? "✅ YES" : "❌ NO");
            
            // FAIL THE TEST if category cache is not working (prevents false positives)
            if (!categoryCacheWorking) {
                Assert.fail("❌ TEST FAILED: Category cache is not working! AEM is showing fresh data immediately instead of cached data. " +
                           "Expected AEM to show cached data: '" + originalCategoryName + "', but got fresh data: '" + currentAemCategoryName + "'. " +
                           "Environment: " + environment + ". Category cache should be working before testing invalidation.");
            }

            // STEP 5: Call cache invalidation (category only)
            LOG.info("🚀 STEP 5: Calling cache invalidation servlet for CATEGORY only");
            boolean cacheInvalidated = callCacheInvalidationServlet(null, categoryUrlKey);
            assertTrue("Cache invalidation servlet call failed", cacheInvalidated);

            // STEP 6: Wait for cache invalidation
            LOG.info("⏳ STEP 6: Waiting for cache invalidation...");
            Thread.sleep(10000); // Wait 10 seconds

            // STEP 7: Verify AEM now shows fresh category data
            LOG.info("🔍 STEP 7: Checking AEM now shows fresh category data");
            String freshCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            LOG.info("   Fresh Category Check: '{}'", freshCategoryName);
            boolean categoryUpdated = freshCategoryName.equals(updatedCategoryName);
            LOG.info("   Category Updated: {}", categoryUpdated ? "✅ YES" : "❌ NO");

            assertTrue("Category cache invalidation failed - AEM not showing fresh data", categoryUpdated);
            LOG.info("🎉 SUCCESS: Category cache invalidation test passed!");

        } finally {
            // Restore original category name
            if (originalCategoryName != null && categoryId != null) {
                try {
                    updateMagentoCategoryName(categoryId, originalCategoryName);
                    LOG.info("🔄 Restored category name: {}", originalCategoryName);
                } catch (Exception e) {
                    LOG.warn("Could not restore category name: {}", e.getMessage());
                }
            }
        }
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
        Thread.sleep(2000); // Wait for Magento to process
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
        Thread.sleep(2000); // Wait for Magento to process
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
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        String configUrl = baseUrl + "/system/console/configMgr/com.adobe.cq.commerce.graphql.magento.GraphqlDataServiceImpl";
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
                    Thread.sleep(3000);
                } else {
                    LOG.warn("⚠️ Configuration response status: {}", statusCode);
                }
            }
        } catch (Exception e) {
            LOG.warn("⚠️ Could not apply GraphQL Data Service config: {}", e.getMessage());
        }
    }

    /**
     * Product cache invalidation test using cache names from GraphQL client configuration
     */
    private void runCacheNamesProductTest(String environment, String testSku, String categoryPageUrl) throws Exception {
        String originalProductName = null;
        String randomSuffix = generateRandomString(6);

        try {
            // STEP 1: Get product name from Magento
            LOG.info("📋 STEP 1: Getting original product name from Magento");
            JsonNode productData = getMagentoProductData(testSku);
            originalProductName = productData.get("name").asText();
            LOG.info("   ✓ Magento Product Name: '{}'", originalProductName);

            // STEP 2: Update product name in Magento
            LOG.info("🔄 STEP 2: Updating Magento product name");
            String updatedProductName = originalProductName + " " + randomSuffix;
            updateMagentoProductName(testSku, updatedProductName);
            LOG.info("   ✓ Updated Magento Product: '{}'", updatedProductName);

            // STEP 3: Verify cache working (AEM should show old name)
            LOG.info("📋 STEP 3: Checking AEM still shows cached data");
            String aemProductName = getCurrentProductNameFromAEMPage(categoryPageUrl, testSku);
            LOG.info("   AEM Product Shows: '{}'", aemProductName);
            LOG.info("   Updated Magento Product: '{}'", updatedProductName);
            boolean productCacheWorking = !aemProductName.equals(updatedProductName);
            LOG.info("   Product Cache Working: {}", productCacheWorking ? "✅ YES" : "❌ NO");
            
            // FAIL THE TEST if product cache is not working (prevents false positives)
            if (!productCacheWorking) {
                Assert.fail("❌ TEST FAILED: Product cache is not working! AEM is showing fresh data immediately instead of cached data. " +
                           "Expected AEM to show cached data: '" + originalProductName + "', but got fresh data: '" + aemProductName + "'. " +
                           "Environment: " + environment + ". Product cache should be working before testing invalidation.");
            }

            // STEP 4: Clear cache using CACHE NAMES from GraphQL client configuration
            LOG.info("🚀 STEP 4: Calling cache invalidation with COMPONENT CACHE NAMES");
            String payload = String.format(
                "{\n" +
                "    \"cacheNames\": [\n" +
                "        \"venia/components/commerce/product\",\n" +
                "        \"venia/components/commerce/productlist\",\n" +
                "        \"venia/components/commerce/navigation\"\n" +
                "    ],\n" +
                "    \"storePath\": \"/content/venia/us/en\"\n" +
                "}");
            
            LOG.info("📝 Cache invalidation payload (product cache names): {}", payload);
            SlingHttpResponse response = adminAuthor.doPost("/bin/cif/invalidate-cache", new StringEntity(payload, ContentType.APPLICATION_JSON), 200);
            LOG.info("📤 Response: Status={}, Content={}", response.getStatusLine(), response.getContent());

            // STEP 5: Wait for cache invalidation
            LOG.info("⏳ STEP 5: Waiting for cache names invalidation...");
            Thread.sleep(10000);
            
            LOG.info("🔍 STEP 6: Verifying product component caches cleared");
            aemProductName = getCurrentProductNameFromAEMPage(categoryPageUrl, testSku);
            boolean productUpdated = aemProductName.contains(randomSuffix);
            LOG.info("   Fresh Product Check: '{}'", aemProductName);
            LOG.info("   Product Updated: {} {}", productUpdated ? "✅" : "❌", productUpdated ? "YES" : "NO");

            if (productUpdated) {
                LOG.info("🎉 SUCCESS: Product cache names invalidation test passed!");
            } else {
                throw new AssertionError("❌ FAILED: Product cache names invalidation failed - AEM still shows old cached data");
            }

        } finally {
            // Cleanup: restore original product name
            if (originalProductName != null) {
                try {
                    updateMagentoProductName(testSku, originalProductName);
                    LOG.info("🔄 Restored product name: {}", originalProductName);
                } catch (Exception e) {
                    LOG.warn("⚠️ Failed to restore product name: {}", e.getMessage());
                }
            }
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
            
            // FAIL THE TEST if product cache is not working (prevents false positives)
            if (!productCacheWorking) {
                Assert.fail("❌ TEST FAILED: Product cache is not working! AEM is showing fresh data immediately instead of cached data. " +
                           "Expected AEM to show cached data: '" + originalProductName + "', but got fresh data: '" + aemProductName + "'. " +
                           "Environment: " + environment + ". Product cache should be working before testing invalidation.");
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
            LOG.info("🧹 Cleanup complete");
        }
    }

    /**
     * Category Cache Test using Cache Names - Focused on category component clearing
     */

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
            
            // FAIL THE TEST if either cache is not working (prevents false positives)
            if (!productCacheWorking && !categoryCacheWorking) {
                Assert.fail("❌ TEST FAILED: Both Product AND Category caches are not working! AEM is showing fresh data immediately instead of cached data. " +
                           "Expected cached data but got fresh data for both. Environment: " + environment + ". Both caches should be working before testing invalidation.");
            } else if (!productCacheWorking) {
                Assert.fail("❌ TEST FAILED: Product cache is not working! AEM is showing fresh data immediately instead of cached data. " +
                           "Expected cached data but got fresh data for product. Environment: " + environment + ". Product cache should be working before testing invalidation.");
            } else if (!categoryCacheWorking) {
                Assert.fail("❌ TEST FAILED: Category cache is not working! AEM is showing fresh data immediately instead of cached data. " +
                           "Expected cached data but got fresh data for category. Environment: " + environment + ". Category cache should be working before testing invalidation.");
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
            
            // FAIL THE TEST if product cache is not working (prevents false positives)
            if (!productCacheWorking) {
                Assert.fail("❌ TEST FAILED: Product cache is not working! AEM is showing fresh data immediately instead of cached data. " +
                           "Expected AEM to show cached data: '" + originalProductName + "', but got fresh data: '" + aemProductName + "'. " +
                           "Environment: " + environment + ". Product cache should be working before testing invalidation.");
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
            LOG.info("🧹 Cleanup complete");
        }
    }

    /**
     * Invalidate All Cache Test - Tests complete cache flush
     */
    private void runInvalidateAllCacheTest(String environment, String testSku, String categoryPageUrl, String categoryUrlKey) throws Exception {
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
            
            // FAIL THE TEST if either cache is not working (prevents false positives)
            if (!productCacheWorking && !categoryCacheWorking) {
                Assert.fail("❌ TEST FAILED: Both Product AND Category caches are not working! AEM is showing fresh data immediately instead of cached data. " +
                           "Expected cached data but got fresh data for both. Environment: " + environment + ". Both caches should be working before testing invalidation.");
            } else if (!productCacheWorking) {
                Assert.fail("❌ TEST FAILED: Product cache is not working! AEM is showing fresh data immediately instead of cached data. " +
                           "Expected cached data but got fresh data for product. Environment: " + environment + ". Product cache should be working before testing invalidation.");
            } else if (!categoryCacheWorking) {
                Assert.fail("❌ TEST FAILED: Category cache is not working! AEM is showing fresh data immediately instead of cached data. " +
                           "Expected cached data but got fresh data for category. Environment: " + environment + ". Category cache should be working before testing invalidation.");
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
            Thread.sleep(15000); // Longer wait for complete flush
            
            LOG.info("🔍 STEP 6: Verifying ALL caches cleared");
            aemProductName = getCurrentProductNameFromAEMPage(getProductPageUrl(testSku), testSku);
            aemCategoryName = getCurrentCategoryNameFromAEMPage(categoryPageUrl);
            boolean productUpdated = aemProductName.contains(randomSuffix);
            boolean categoryUpdated = aemCategoryName.contains(randomSuffix);
            LOG.info("   Fresh Product Check: '{}'", aemProductName);
            LOG.info("   Fresh Category Check: '{}'", aemCategoryName);
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
            LOG.info("🧹 Cleanup complete");
        }
    }

    /**
     * Warm up cache for Cloud environment to avoid cold cache issues
     */
    private void warmUpCache(String testSku, String categoryPageUrl) throws Exception {
        LOG.info("🔥 WARM-UP: Pre-loading cache for product '{}' in Cloud environment", testSku);
        
        try {
            // Make a few cache-respecting requests to warm up the cache
            for (int i = 0; i < 3; i++) {
                makeCacheRespectingRequest(categoryPageUrl);
                Thread.sleep(1000); // Small delay between requests
            }
            
            // Also warm up the specific product
            getCurrentProductNameFromAEMPage(categoryPageUrl, testSku);
            Thread.sleep(1000);
            
            LOG.info("🔥 WARM-UP: Cache warm-up completed");
        } catch (Exception e) {
            LOG.warn("⚠️ WARM-UP: Cache warm-up failed, but continuing test: {}", e.getMessage());
        }
    }
}
