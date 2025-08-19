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
import org.osgi.service.component.annotations.Component;
import java.util.*;

/**
 * Strategy implementation for clearing the dispatcher and graphql cache based on custom attribute.
 * 
 * <p>Example request JSON for cache invalidation:</p>
 * <pre>
 * {
 *   "storePath": "/content/venia/us/en",
 *   "customProductSkus": ["sku1", "sku2", "sku3"]
 * }
 * </pre>
 * 
 * <p>This will invalidate the cache for products with the specified SKUs.</p>
 */
@Component(
        service = DispatcherCacheInvalidationStrategy.class)
public class CustomDispatcherInvalidation implements DispatcherCacheInvalidationStrategy {

    // Note: If we are passing null value then it will not clear graphql cache
    @Override
    public List<String> getPatterns(String[] invalidationParameters) {
        String pattern = "\"sku\":\\s*\"";
        String invalidationParametersString = String.join("|", invalidationParameters);
        return Collections.singletonList(pattern + "(" + invalidationParametersString + ")");
    }

    @Override
    public String getInvalidationType() {
        return "customProductSkus";
    }

    @Override
    public List<String> getPathsToInvalidate(CacheInvalidationContext context) {

        // Add you custom logic to get the corresponding paths to be invalidated
        return Collections.emptyList();
    }

}
