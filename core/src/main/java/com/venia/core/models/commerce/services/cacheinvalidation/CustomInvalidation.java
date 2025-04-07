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

import com.adobe.cq.commerce.core.cacheinvalidation.spi.CacheInvalidationStrategy;
import org.osgi.service.component.annotations.Component;

import java.util.Collections;
import java.util.List;

/**
 * Strategy implementation for clearing the graphql cache based on custom attribute.
 * 
 * <p>Example request JSON for cache invalidation:</p>
 * <pre>
 * {
 *   "storePath": "/content/venia/us/en",
 *   "customCategoryUids": ["categoryUid-1", "categoryUid-2", "categoryUid-3"]
 * }
 * </pre>
 * 
 * <p>This will invalidate cache entries containing the specified category UIDs.</p>
 */
@Component(
        service = CacheInvalidationStrategy.class)
public class CustomInvalidation implements CacheInvalidationStrategy {

    @Override
    public List<String> getPatterns(String[] invalidationParameters) {
        String pattern = "\"uids\"\\s*:\\s*\\{\"id\"\\s*:\\s*\"";
        String invalidationParametersString = String.join("|", invalidationParameters);
        return Collections.singletonList(pattern + "(" + invalidationParametersString + ")");
    }

    @Override
    public String getInvalidationRequestType() {
        return "customCategoryUids";
    }
} 