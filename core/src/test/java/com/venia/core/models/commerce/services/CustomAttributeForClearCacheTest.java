package com.venia.core.models.commerce.services;

import com.adobe.cq.commerce.core.cacheinvalidation.spi.DispatcherCacheInvalidationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class CustomAttributeForClearCacheTest {

    private CustomAttributeForClearCache cacheInvalidationStrategy;

    @Mock
    private DispatcherCacheInvalidationContext context;

    @BeforeEach
    void setUp() {
        cacheInvalidationStrategy = new CustomAttributeForClearCache();
        // Setup default mock behavior
        lenient().when(context.getAttributeData()).thenReturn(Collections.emptyList());
    }

    @Test
    void testGetPattern() {
        assertEquals("\"sku\":\\s*\"", cacheInvalidationStrategy.getPattern());
    }

    @Test
    void testGetInvalidationRequestType() {
        assertEquals("customProductSkus", cacheInvalidationStrategy.getInvalidationRequestType());
    }

    @Test
    void testGetPathsToInvalidateWithNullContext() {
        List<String> paths = cacheInvalidationStrategy.getPathsToInvalidate(null);
        assertTrue(paths.isEmpty());
    }

    @Test
    void testGetPathsToInvalidateWithEmptyAttributeData() {
        List<String> paths = cacheInvalidationStrategy.getPathsToInvalidate(context);
        assertTrue(paths.isEmpty());
    }

    @Test
    void testGetPathsToInvalidateWithNullAttributeData() {
        lenient().when(context.getAttributeData()).thenReturn(null);
        List<String> paths = cacheInvalidationStrategy.getPathsToInvalidate(context);
        assertTrue(paths.isEmpty());
    }

    @Test
    void testGetPathsToInvalidateWithValidAttributeData() {
        lenient().when(context.getAttributeData()).thenReturn(Arrays.asList("SKU123", "SKU456"));
        List<String> paths = cacheInvalidationStrategy.getPathsToInvalidate(context);
        assertTrue(paths.isEmpty()); // Currently returns empty list as per implementation
    }
}
