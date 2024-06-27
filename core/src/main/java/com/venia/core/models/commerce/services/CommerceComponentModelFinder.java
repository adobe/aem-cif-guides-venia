/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.venia.core.models.commerce.services;

import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.drew.lang.annotations.Nullable;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.factory.ModelFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * This service allows to traverse a {@link Resource} tree looking for a {@link Resource} of a set of particular resource types and if
 * found adapting them to given adapter type. This helps for example finding the product component on the page and return the Product model
 * from it.
 */
@Component(
        service = com.venia.core.models.commerce.services.CommerceComponentModelFinder.class)
public class CommerceComponentModelFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(com.venia.core.models.commerce.services.CommerceComponentModelFinder.class);
    private static final Collection<String> PRODUCT_RTS = Collections.singleton("core/cif/components/commerce/product/v1/product");
    private static final Collection<String> PRODUCT_LIST_RTS = Arrays.asList(
            "core/cif/components/commerce/productlist/v2/productlist",
            "core/cif/components/commerce/productlist/v1/productlist");

    @Reference
    private ModelFactory modelFactory;

    @Nullable
    public Product findProductComponentModel(SlingHttpServletRequest request, Resource root) {
        return findComponentModel(request, root, PRODUCT_RTS, Product.class);
    }

    @Nullable
    public ProductList findProductListComponentModel(SlingHttpServletRequest request, Resource root) {
        return findComponentModel(request, root, PRODUCT_LIST_RTS, ProductList.class);
    }

    @Nullable
    public <T> T findComponentModel(SlingHttpServletRequest request, Resource root, Collection<String> resourceTypes,
                                    Class<T> adapterType) {
        Resource componentResource = findChildResourceWithType(root, resourceTypes);
        if (componentResource != null) {
            return modelFactory.getModelFromWrappedRequest(request, componentResource, adapterType);
        } else {
            return null;
        }
    }

    private Resource findChildResourceWithType(Resource fromResource, Collection<String> resourceTypes) {
        if (fromResource == null) {
            return null;
        }

        LOGGER.debug("Looking for child resource type '{}' from {}", resourceTypes, fromResource.getPath());

        for (Resource child : fromResource.getChildren()) {
            for (String resourceType : resourceTypes) {
                if (child.isResourceType(resourceType)) {
                    LOGGER.debug("Found child resource type '{}' at {}", resourceType, child.getPath());
                    return child;
                }
            }

            Resource resource = findChildResourceWithType(child, resourceTypes);
            if (resource != null) {
                return resource;
            }
        }

        return null;
    }
}

