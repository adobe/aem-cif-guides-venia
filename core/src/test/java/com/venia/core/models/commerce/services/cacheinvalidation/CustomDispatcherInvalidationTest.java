package com.venia.core.models.commerce.services.cacheinvalidation;

import com.adobe.cq.commerce.core.cacheinvalidation.spi.CacheInvalidationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CustomDispatcherInvalidationTest {

    private CustomDispatcherInvalidation cacheInvalidationStrategy;

    @Mock
    private CacheInvalidationContext context;

    @BeforeEach
    void setUp() {
        cacheInvalidationStrategy = new CustomDispatcherInvalidation();
    }

    @Test
    void testGetPattern() {
        assertEquals("\"sku\":\\s*\"(sku1)", cacheInvalidationStrategy.getPatterns(new String[]{"sku1"}).get(0));
    }

    @Test
    void testGetInvalidationRequestType() {
        assertEquals("customProductSkus", cacheInvalidationStrategy.getInvalidationType());
    }

    @Test
    void testGetPathsToInvalidateWithValidAttributeData() {
        List<String> paths = cacheInvalidationStrategy.getPathsToInvalidate(context);
        assertTrue(paths.isEmpty()); // Currently returns empty list as per implementation
    }
}
