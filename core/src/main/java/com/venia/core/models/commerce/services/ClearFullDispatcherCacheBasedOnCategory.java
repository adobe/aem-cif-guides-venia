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

import com.adobe.cq.commerce.core.cacheinvalidation.spi.DispatcherCacheInvalidationStrategy;
import com.adobe.cq.commerce.magento.graphql.*;
import com.day.cq.wcm.api.Page;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import javax.jcr.Session;
import java.util.*;
import org.apache.sling.api.resource.Resource;

@Component(
    service = DispatcherCacheInvalidationStrategy.class,
    property = {"invalidateRequestParameter=categoryUids" })
public class ClearFullDispatcherCacheBasedOnCategory implements DispatcherCacheInvalidationStrategy {

    private static final String HEADER_FRAGMENT_PATH = "/content/experience-fragments/venia/us/en/site/header/master";
    private static final String NAVIGATION_NODE_PATH = "jcr:content/root/navigation";
    private static final String STRUCTURE_DEPTH_PROPERTY = "structureDepth";
    private static final String CATEGORY_LIST_KEY = "categoryList";
    private static final String LEVEL_KEY = "level";

    @Override
    public String getPattern() {
        return null;
    }

    @Override
    public String[] getCorrespondingPagePaths(Session session, String storePath, String dataList) {
        return new String[0];
    }

    @Override
    public String getGraphqlQuery(String[] data) {
        CategoryFilterInput filter = new CategoryFilterInput();
        FilterEqualTypeInput identifiersFilter = new FilterEqualTypeInput().setIn(Arrays.asList(data));
        filter.setCategoryUid(identifiersFilter);
        QueryQuery.CategoryListArgumentsDefinition searchArgs = s -> s.filters(filter);

        CategoryTreeQueryDefinition queryArgs = q -> q.uid().level();

        return Operations.query(query -> query
                .categoryList(searchArgs, queryArgs)).toString();
    }

    @Override
    public String[] getPathsToInvalidate(Page page, ResourceResolver resourceResolver, Map<String, Object> data, String storePath) {
        if (resourceResolver == null || data == null || !data.containsKey(CATEGORY_LIST_KEY)) {
            return new String[0];
        }

        Integer navigationStructureDepth = getNavigationStructureDepth(resourceResolver);
        if (navigationStructureDepth == null) {
            return new String[0];
        }

        List<Map<String, Object>> categories = (List<Map<String, Object>>) data.get(CATEGORY_LIST_KEY);
        
        if (shouldInvalidateFullCache(categories, navigationStructureDepth)) {
            return new String[] { storePath };
        }
        return new String[0];
    }

    private Integer getNavigationStructureDepth(ResourceResolver resourceResolver) {
        Resource headerResource = resourceResolver.getResource(
            HEADER_FRAGMENT_PATH + "/" + NAVIGATION_NODE_PATH);
        if (headerResource == null) {
            return null;
        }

        return headerResource.getValueMap().get(STRUCTURE_DEPTH_PROPERTY, Integer.class);
    }

    private boolean shouldInvalidateFullCache(List<Map<String, Object>> categories, int navigationStructureDepth) {
        return categories.stream()
            .map(category -> ((Number) category.get(LEVEL_KEY)).intValue())
            .filter(Objects::nonNull)
            .anyMatch(level -> level <= navigationStructureDepth + 1);
    }
}
