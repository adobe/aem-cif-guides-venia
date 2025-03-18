package com.venia.core.models.commerce.services.cacheinvalidation;

import com.adobe.cq.commerce.core.cacheinvalidation.spi.DispatcherCacheInvalidationContext;
import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.*;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.day.cq.wcm.api.Page;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClearFullDispatcherCacheBasedOnExistingCategoryAttributeTest {

    private static final String TEST_STORE_PATH = "/content/venia/us/en";
    private static final String HEADER_FRAGMENT_PATH = "/content/experience-fragments/venia/us/en/site/header/master/jcr:content/root/navigation";

    @Mock private DispatcherCacheInvalidationContext context;
    @Mock private MagentoGraphqlClient graphqlClient;
    @Mock private Page page;

    @Mock private ResourceResolver resourceResolver;
    @Mock private Resource headerResource;
    @Mock private ValueMap valueMap;

    @Mock private GraphqlResponse<Query, Error> graphqlResponse;
    @Mock private Query query;
    @Mock private List<CategoryTree> categoryList;
    @Mock private CategoryTree categoryTree;
    @Mock private CategoryTree categoryTree2;

    private ClearFullDispatcherCacheBasedOnExistingCategoryAttribute strategy;

    @BeforeEach
    void setUp() {
        strategy = new ClearFullDispatcherCacheBasedOnExistingCategoryAttribute();
    }

    private void mockRequiredStuffs() {
        when(context.getResourceResolver()).thenReturn(resourceResolver);
        when(context.getGraphqlClient()).thenReturn(graphqlClient);
        when(context.getAttributeData()).thenReturn(Collections.singletonList("category1"));
        when(resourceResolver.getResource(HEADER_FRAGMENT_PATH)).thenReturn(headerResource);
        when(headerResource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get("structureDepth", Integer.class)).thenReturn(2);
    }

    @Test
    void shouldHandleInvalidScenarios() {
        when(context.getAttributeData()).thenReturn(Collections.emptyList());
        List<String> result = strategy.getPathsToInvalidate(context);
        assertEquals(0, result.size());
    }

    @Test
    void navigationStructureDepthBeenNotSet() {
        when(context.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.getResource(HEADER_FRAGMENT_PATH)).thenReturn(null);
        when(context.getAttributeData()).thenReturn(Collections.singletonList("category1"));
        List<String> result = strategy.getPathsToInvalidate(context);
        assertEquals(0, result.size());
    }

    @Test
    void shouldHandleInValidCategories() {
        mockRequiredStuffs();
        when(graphqlClient.execute(any())).thenReturn(null);
        List<String> result = strategy.getPathsToInvalidate(context);
        assertEquals(0, result.size());
    }

    @Test
    void shouldHandleForValidCategories() {
        when(context.getStorePath()).thenReturn(TEST_STORE_PATH);
        mockRequiredStuffs();
        when(graphqlClient.execute(any())).thenReturn(graphqlResponse);
        when(graphqlResponse.getData()).thenReturn(query);
        when(query.getCategoryList()).thenReturn(categoryList);
        when(categoryTree.getLevel()).thenReturn(1);
        when(categoryList.stream()).thenReturn(Stream.of(categoryTree));
        List<String> result = strategy.getPathsToInvalidate(context);
        assertEquals(1, result.size());
        assertEquals(TEST_STORE_PATH, result.get(0));
    }
}
