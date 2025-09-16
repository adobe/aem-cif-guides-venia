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
    }

    @After
    public void tearDown() throws Exception {
        if (httpClient != null) {
            httpClient.close();
        }
        LOG.info("🧹 Cleanup complete");
    }

    /**
     * AEM 6.5 Product Cache Invalidation Test
     */
    @Test
    @Category(IgnoreOnCloud.class)
    public void test65_Product_CacheInvalidation() throws Exception {
        runProductCacheInvalidationTest(
                "BLT-LEA-001", // SKU - Back to original
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-leather-belts.html", // Category page
                "AEM 6.5 - Product"
        );
    }

    /**
     * Cloud Product Cache Invalidation Test
     */
    @Test
    @Category(IgnoreOn65.class)
    public void testCloud_Product_CacheInvalidation() throws Exception {
        runProductCacheInvalidationTest(
                "BLT-FAB-001", // SKU - Back to original
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-fabric-belts.html", // Category page
                "Cloud - Product"
        );
    }

    /**
     * AEM 6.5 Category Cache Invalidation Test
     */
    @Test
    @Category(IgnoreOnCloud.class)
    public void test65_Category_CacheInvalidation() throws Exception {
        runCategoryCacheInvalidationTest(
                "BLT-LEA-001", // SKU - Back to original
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-leather-belts.html", // Category page
                "venia-leather-belts", // URL key
                "AEM 6.5 - Category"
        );
    }

    /**
     * Cloud Category Cache Invalidation Test
     */
    @Test
    @Category(IgnoreOn65.class)
    public void testCloud_Category_CacheInvalidation() throws Exception {
        runCategoryCacheInvalidationTest(
                "BLT-FAB-001", // SKU - Back to original
                "/content/venia/us/en/products/category-page.html/venia-accessories/venia-belts/venia-fabric-belts.html", // Category page
                "venia-fabric-belts", // URL key
                "Cloud - Category"
        );
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
            SlingHttpResponse response = adminAuthor.doGet(categoryPageUrl, 200);
            Document doc = Jsoup.parse(response.getContent());

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
            SlingHttpResponse response = adminAuthor.doGet(categoryPageUrl, 200);
            Document doc = Jsoup.parse(response.getContent());

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
}
