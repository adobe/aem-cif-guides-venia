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

package com.venia.core.models.commerce.services.cacheinvalidation;

import com.adobe.cq.commerce.core.cacheinvalidation.spi.CacheInvalidationContext;
import com.adobe.cq.commerce.core.cacheinvalidation.spi.DispatcherCacheInvalidationStrategy;
import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.*;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import java.util.*;

import org.apache.sling.api.resource.Resource;

/**
 * Extended implementation of the categoryUids cache invalidation strategy that builds upon the out-of-the-box functionality.
 * This service extends the standard category invalidation by:
 * - Adding support for navigation structure depth-based invalidation
 * - Apart from the existing categoryUids invalidation, this service will invalidates the entire store cache when category falls under the navigation structure depth.
 * 
 * The service invalidates the entire store cache when category falls under the navigation structure depth.
 */
@Component(
    service = DispatcherCacheInvalidationStrategy.class)
public class ExtendedCategoryUidInvalidation implements DispatcherCacheInvalidationStrategy {

    // Constants for navigation structure
    private static final String HEADER_FRAGMENT_PATH = "/content/experience-fragments/venia/us/en/site/header/master";
    private static final String NAVIGATION_NODE_PATH = "jcr:content/root/navigation";
    private static final String STRUCTURE_DEPTH_PROPERTY = "structureDepth";

    @Override
    public List<String> getPatterns(String[] invalidationParameters) {
        String pattern = "\"uids\"\\s*:\\s*\\{\"id\"\\s*:\\s*\"";
        String invalidationParametersString = String.join("|", invalidationParameters);
        return Collections.singletonList(pattern + "(" + invalidationParametersString + ")");
    }

    @Override
    public String getInvalidationRequestType() {
        return "categoryUids";
    }

    @Override
    public List<String> getPathsToInvalidate(CacheInvalidationContext context) {
        // Extract and validate category UIDs
        List<String> categoryUids = context.getInvalidationParameters();
        if (categoryUids == null || categoryUids.isEmpty()) {
            return Collections.emptyList();
        }

        // Get navigation structure depth from configuration
        Integer navigationStructureDepth = getNavigationStructureDepth(context.getResourceResolver());
        if (navigationStructureDepth == null) {
            return Collections.emptyList();
        }

        // Check if any category level requires cache invalidation
        if (shouldInvalidateFullCache(context, categoryUids.toArray(new String[0]), navigationStructureDepth)) {
            return Collections.singletonList(context.getStorePath());
        }

        return Collections.emptyList();
    }

    /**
     * Checks if the full cache should be invalidated based on category levels.
     * This method fetches category data and determines if any category's level
     * is within the navigation structure depth.
     *
     * @param context The cache invalidation context
     * @param categoryUids Array of category UIDs to query
     * @param navigationStructureDepth The navigation structure depth
     * @return true if cache should be invalidated, false otherwise
     */
    private boolean shouldInvalidateFullCache(CacheInvalidationContext context, String[] categoryUids, int navigationStructureDepth) {
        String query = buildCategoryQuery(categoryUids);
        Query data = getGraphqlResponseData(context.getGraphqlClient(), query);
        if (data == null || data.getCategoryList() == null) {
            return false;
        }
        List<CategoryTree> categories = data.getCategoryList();
        return categories.stream()
            .map(CategoryTree::getLevel)
            .filter(Objects::nonNull)
            .anyMatch(level -> level <= navigationStructureDepth + 1);
    }

    protected Query getGraphqlResponseData(MagentoGraphqlClient client, String query) {
        GraphqlResponse<com.adobe.cq.commerce.magento.graphql.Query, Error> response = client.execute(query);
        if (response == null || (response.getErrors() != null && !response.getErrors().isEmpty()) || response.getData() == null) {
            return null;
        }
        return response.getData();
    }

    /**
     * Builds the GraphQL query for fetching category information.
     *
     * @param categoryUids Array of category UIDs to query
     * @return The GraphQL query string
     */
    private String buildCategoryQuery(String[] categoryUids) {
            CategoryFilterInput filter = new CategoryFilterInput();
            FilterEqualTypeInput identifiersFilter = new FilterEqualTypeInput().setIn(Arrays.asList(categoryUids));
            filter.setCategoryUid(identifiersFilter);
            QueryQuery.CategoryListArgumentsDefinition searchArgs = s -> s.filters(filter);
            CategoryTreeQueryDefinition queryArgs = q -> q.uid().level();
            return Operations.query(query -> query
                    .categoryList(searchArgs, queryArgs)).toString();
    }

    /**
     * Retrieves the navigation structure depth from the header configuration.
     *
     * @param resourceResolver The resource resolver
     * @return The navigation structure depth or null if not found
     */
    private Integer getNavigationStructureDepth(ResourceResolver resourceResolver) {
        Resource headerResource = resourceResolver.getResource(
            HEADER_FRAGMENT_PATH + "/" + NAVIGATION_NODE_PATH);
        return headerResource != null ? headerResource.getValueMap().get(STRUCTURE_DEPTH_PROPERTY, Integer.class) : null;
    }
}
