package com.venia.core.models.commerce.services.cacheinvalidation;

import com.adobe.cq.commerce.core.cacheinvalidation.spi.DispatcherCacheInvalidationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CustomAttributeForClearCacheForGraphqlAndDispatcherTest {

    private CustomAttributeForClearCacheForGraphqlAndDispatcher cacheInvalidationStrategy;

    @Mock
    private DispatcherCacheInvalidationContext context;

    @BeforeEach
    void setUp() {
        cacheInvalidationStrategy = new CustomAttributeForClearCacheForGraphqlAndDispatcher();
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
    void testGetPathsToInvalidateWithValidAttributeData() {
        List<String> paths = cacheInvalidationStrategy.getPathsToInvalidate(context);
        assertTrue(paths.isEmpty()); // Currently returns empty list as per implementation
    }
}
