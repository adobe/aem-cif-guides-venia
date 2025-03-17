package com.venia.core.models.commerce.services;

import com.adobe.cq.commerce.core.cacheinvalidation.spi.DispatcherCacheInvalidationContext;
import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.magento.graphql.*;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ClearFullDispatcherCacheBasedOnCategoryTest {

    private ClearFullDispatcherCacheBasedOnCategory cacheInvalidationStrategy;

    @Mock
    private DispatcherCacheInvalidationContext context;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Resource headerResource;

    @Mock
    private ValueMap valueMap;

    @Mock
    private MagentoGraphqlClient graphqlClient;

    @Mock
    private GraphqlResponse<Query, Error> graphqlResponse;

    @Mock
    private Query query;

    @Mock
    private List<CategoryTree> categoryList;

    @BeforeEach
    void setUp() {
        cacheInvalidationStrategy = new ClearFullDispatcherCacheBasedOnCategory();
    }

    @Test
    void testGetPattern() {
        assertNull(cacheInvalidationStrategy.getPattern());
    }

    @Test
    void testGetInvalidationRequestType() {
        assertEquals("categoryUids", cacheInvalidationStrategy.getInvalidationRequestType());
    }

    @Test
    void testGetPathsToInvalidateWithNullContext() {
        List<String> paths = cacheInvalidationStrategy.getPathsToInvalidate(null);
        assertTrue(paths.isEmpty());
    }

    @Test
    void testGetPathsToInvalidateWithNullResourceResolver() {
        when(context.getResourceResolver()).thenReturn(null);
        List<String> paths = cacheInvalidationStrategy.getPathsToInvalidate(context);
        assertTrue(paths.isEmpty());
    }

    @Test
    void testGetPathsToInvalidateWithNullHeaderResource() {
        when(context.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.getResource(any())).thenReturn(null);
        List<String> paths = cacheInvalidationStrategy.getPathsToInvalidate(context);
        assertTrue(paths.isEmpty());
    }

    @Test
    void testGetPathsToInvalidateWithNullStructureDepth() {
        when(context.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.getResource(any())).thenReturn(headerResource);
        when(headerResource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get(any(), eq(Integer.class))).thenReturn(null);
        List<String> paths = cacheInvalidationStrategy.getPathsToInvalidate(context);
        assertTrue(paths.isEmpty());
    }

    @Test
    void testGetPathsToInvalidateWithEmptyCategoryUids() {
        when(context.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.getResource(any())).thenReturn(headerResource);
        when(headerResource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get(any(), eq(Integer.class))).thenReturn(2);
        when(context.getAttributeData()).thenReturn(Collections.emptyList());
        List<String> paths = cacheInvalidationStrategy.getPathsToInvalidate(context);
        assertTrue(paths.isEmpty());
    }

    @Test
    void testGetPathsToInvalidateWithGraphqlError() {
        // Setup
        when(context.getResourceResolver()).thenReturn(resourceResolver);
        when(resourceResolver.getResource(any())).thenReturn(headerResource);
        when(headerResource.getValueMap()).thenReturn(valueMap);
        when(valueMap.get(any(), eq(Integer.class))).thenReturn(2);
        when(context.getAttributeData()).thenReturn(Arrays.asList("category1"));
        when(context.getGraphqlClient()).thenReturn(graphqlClient);
        when(graphqlClient.execute(any())).thenReturn(graphqlResponse);
        when(graphqlResponse.getData()).thenReturn(null);

        // Execute
        List<String> paths = cacheInvalidationStrategy.getPathsToInvalidate(context);

        // Verify
        assertTrue(paths.isEmpty());
    }
}
