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

import com.adobe.cq.commerce.core.cacheinvalidation.spi.DispatcherCacheInvalidationContext;
import com.adobe.cq.commerce.core.cacheinvalidation.spi.DispatcherCacheInvalidationStrategy;
import org.osgi.service.component.annotations.Component;
import java.util.*;

/**
 * Strategy implementation for clearing the dispatcher & graphql cache based on custom attribute.
 */
@Component(
        service = DispatcherCacheInvalidationStrategy.class)
public class CustomAttributeForClearCacheForGraphqlAndDispatcher implements DispatcherCacheInvalidationStrategy {

    // Note: If we are passing null value then it will not clear graphql cache
    @Override
    public String getPattern() {
        return "\"sku\":\\s*\"";
    }

    @Override
    public String getInvalidationRequestType() {
        return "customProductSkus";
    }

    @Override
    public List<String> getPathsToInvalidate(DispatcherCacheInvalidationContext context) {

        // Add you custom logic to get the corresponding paths to be invalidated
        return Collections.emptyList();
    }

}
