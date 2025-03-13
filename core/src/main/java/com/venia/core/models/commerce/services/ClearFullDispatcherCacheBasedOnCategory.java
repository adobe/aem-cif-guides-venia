/*******************************************************************************
 *
 *    Copyright 2025 Adobe. All rights reserved.
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

package com.venia.core.models.commerce.services;

import com.adobe.cq.commerce.core.cacheinvalidation.spi.DispatcherCacheInvalidationContext;
import com.adobe.cq.commerce.core.cacheinvalidation.spi.DispatcherCacheInvalidationStrategy;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.*;
import com.google.gson.reflect.TypeToken;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import java.lang.reflect.Type;
import java.util.*;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Strategy implementation for clearing the dispatcher cache based on category changes.
 * This service invalidates the entire store cache when category changes affect the navigation structure.
 */
@Component(
    service = DispatcherCacheInvalidationStrategy.class,
    property = {"invalidateRequestParameter=categoryUids" })
public class ClearFullDispatcherCacheBasedOnCategory implements DispatcherCacheInvalidationStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClearFullDispatcherCacheBasedOnCategory.class);

    // Constants for navigation structure
    private static final String HEADER_FRAGMENT_PATH = "/content/experience-fragments/venia/us/en/site/header/master";
    private static final String NAVIGATION_NODE_PATH = "jcr:content/root/navigation";
    private static final String STRUCTURE_DEPTH_PROPERTY = "structureDepth";
    private static final String CATEGORY_LIST_KEY = "categoryList";
    private static final String LEVEL_KEY = "level";
    private static final String ITEMS_KEY = "items";

    @Override
    public String getPattern() {
        return null;
    }

    @Override
    public String[] getPathsToInvalidate(DispatcherCacheInvalidationContext context) {
        if (context == null) {
            LOGGER.warn("Context is null when getting paths to invalidate");
            return new String[0];
        }

        // Get navigation structure depth from configuration
        Integer navigationStructureDepth = getNavigationStructureDepth(context.getResourceResolver());
        if (navigationStructureDepth == null) {
            return new String[0];
        }

        // Extract and validate category UIDs
        String[] categoryUids = extractCategoryUidsFromContext(context);
        if (!isValidCategoryUids(categoryUids)) {
            return new String[0];
        }

        // Fetch category data and determine if cache invalidation is needed
        List<Map<String, Object>> categories = fetchCategoryData(context, categoryUids);
        if (categories.isEmpty()) {
            return new String[0];
        }
        
        if (shouldInvalidateFullCache(categories, navigationStructureDepth)) {
            return new String[] { context.getStorePath() };
        }
        
        return new String[0];
    }

    /**
     * Extracts category UIDs from the context's attribute data.
     *
     * @param context The cache invalidation context
     * @return Array of category UIDs
     */
    private String[] extractCategoryUidsFromContext(DispatcherCacheInvalidationContext context) {
        if (context == null) {
            LOGGER.warn("Context is null when extracting category UIDs");
            return new String[0];
        }

        Map.Entry<String, String[]> attributeData = context.getAttributeData();
        return attributeData != null ? attributeData.getValue() : new String[0];
    }

    /**
     * Fetches category information using GraphQL query.
     *
     * @param context The cache invalidation context
     * @param categoryUids Array of category UIDs to query
     * @return List of category data maps
     */
    private List<Map<String, Object>> fetchCategoryData(DispatcherCacheInvalidationContext context, String[] categoryUids) {
        if (context == null || categoryUids == null) {
            LOGGER.warn("Invalid parameters for fetching category data");
            return Collections.emptyList();
        }

        String query = buildCategoryQuery(categoryUids);
        if (query == null) {
            LOGGER.warn("Failed to build category query");
            return Collections.emptyList();
        }

        Map<String, Object> responseData = executeGraphqlQuery(context.getGraphqlClient(), query);
        List<Map<String, Object>> categories = extractCategoriesFromResponse(responseData);

        if (categories.isEmpty()) {
            LOGGER.debug("No categories found for UIDs: {}", (Object) categoryUids);
        }

        return categories;
    }

    /**
     * Executes a GraphQL query and returns the response data.
     *
     * @param client The GraphQL client
     * @param query The GraphQL query to execute
     * @return The response data as a map
     */
    private Map<String, Object> executeGraphqlQuery(GraphqlClient client, String query) {
        if (client == null || query == null) {
            LOGGER.warn("Invalid parameters for executing GraphQL query");
            return Collections.emptyMap();
        }

        try {
            GraphqlRequest request = new GraphqlRequest(query);
            Type typeOfT = new TypeToken<Map<String, Object>>() {}.getType();
            Type typeOfU = new TypeToken<Map<String, Object>>() {}.getType();
            GraphqlResponse<Map<String, Object>, Map<String, Object>> response = client.execute(request, typeOfT, typeOfU);

            if (response == null) {
                LOGGER.warn("Null response received from GraphQL query");
                return Collections.emptyMap();
            }

            if (response.getErrors() != null && !response.getErrors().isEmpty()) {
                LOGGER.warn("GraphQL query returned errors: {}", response.getErrors());
                return Collections.emptyMap();
            }

            Map<String, Object> data = response.getData();
            if (data == null) {
                LOGGER.warn("No data received from GraphQL query");
                return Collections.emptyMap();
            }

            return data;
        } catch (Exception e) {
            LOGGER.error("Error executing GraphQL query", e);
            return Collections.emptyMap();
        }
    }

    /**
     * Builds the GraphQL query for fetching category information.
     *
     * @param categoryUids Array of category UIDs to query
     * @return The GraphQL query string
     */
    private String buildCategoryQuery(String[] categoryUids) {
        if (categoryUids == null || categoryUids.length == 0) {
            LOGGER.warn("Invalid category UIDs for building query");
            return null;
        }

        try {
            CategoryFilterInput filter = new CategoryFilterInput();
            FilterEqualTypeInput identifiersFilter = new FilterEqualTypeInput().setIn(Arrays.asList(categoryUids));
            filter.setCategoryUid(identifiersFilter);
            QueryQuery.CategoryListArgumentsDefinition searchArgs = s -> s.filters(filter);

            CategoryTreeQueryDefinition queryArgs = q -> q.uid().level();

            return Operations.query(query -> query
                    .categoryList(searchArgs, queryArgs)).toString();
        } catch (Exception e) {
            LOGGER.error("Error building category query", e);
            return null;
        }
    }

    /**
     * Extracts categories from the GraphQL response data.
     *
     * @param responseData The GraphQL response data
     * @return List of category data maps
     */
    private List<Map<String, Object>> extractCategoriesFromResponse(Map<String, Object> responseData) {
        if (responseData == null) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> items = Optional.ofNullable(responseData)
                .map(cd -> (List<Map<String, Object>>) cd.get(CATEGORY_LIST_KEY))
                .filter(list -> list != null && !list.isEmpty())
                .orElse(Collections.emptyList());
        if (items.isEmpty()) {
            LOGGER.debug("No categories found");
            return Collections.emptyList();
        }

        return items;
    }

    /**
     * Validates if the provided category UIDs array is valid for processing.
     *
     * @param categoryUids Array of category UIDs to validate
     * @return true if category UIDs are valid, false otherwise
     */
    private boolean isValidCategoryUids(String[] categoryUids) {
        if (categoryUids == null || categoryUids.length == 0) {
            LOGGER.warn("No category UIDs provided for cache invalidation");
            return false;
        }
        return true;
    }

    /**
     * Retrieves the navigation structure depth from the header configuration.
     *
     * @param resourceResolver The resource resolver
     * @return The navigation structure depth or null if not found
     */
    private Integer getNavigationStructureDepth(ResourceResolver resourceResolver) {
        if (resourceResolver == null) {
            LOGGER.warn("Resource resolver is null when getting navigation structure depth");
            return null;
        }

        Resource headerResource = resourceResolver.getResource(
            HEADER_FRAGMENT_PATH + "/" + NAVIGATION_NODE_PATH);
        if (headerResource == null) {
            LOGGER.warn("Header resource not found at path: {}", HEADER_FRAGMENT_PATH);
            return null;
        }

        return headerResource.getValueMap().get(STRUCTURE_DEPTH_PROPERTY, Integer.class);
    }

    /**
     * Determines if the full cache should be invalidated based on category levels.
     *
     * @param categories List of category data maps
     * @param navigationStructureDepth The navigation structure depth
     * @return true if cache should be invalidated, false otherwise
     */
    private boolean shouldInvalidateFullCache(List<Map<String, Object>> categories, int navigationStructureDepth) {
        if (categories == null || categories.isEmpty()) {
            return false;
        }

        return categories.stream()
            .map(category -> {
                Object levelObj = category.get(LEVEL_KEY);
                if (!(levelObj instanceof Number)) {
                    LOGGER.warn("Invalid level data type in category");
                    return null;
                }
                return ((Number) levelObj).intValue();
            })
            .filter(Objects::nonNull)
            .anyMatch(level -> level <= navigationStructureDepth + 1);
    }
}
