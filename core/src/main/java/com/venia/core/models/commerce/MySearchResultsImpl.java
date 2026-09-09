/*******************************************************************************
 *
 *    Copyright 2023 Adobe. All rights reserved.
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
package com.venia.core.models.commerce;

import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.searchresults.SearchResults;
import com.adobe.cq.commerce.core.components.storefrontcontext.SearchResultsStorefrontContext;
import com.adobe.cq.commerce.core.components.storefrontcontext.SearchStorefrontContext;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.core.search.models.SorterKey;
import com.adobe.cq.commerce.magento.graphql.ProductAttributeFilterInput;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQuery;
import com.adobe.cq.wcm.core.components.models.Component;
import com.adobe.cq.wcm.core.components.models.datalayer.ComponentData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.via.ResourceSuperType;

import javax.annotation.PostConstruct;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * This class shows how to customize the sort fields of the product search results.
 * As am example we remove the support for sorting by position.
 * See the initModel() method for details check.
 */
@Model(adaptables = SlingHttpServletRequest.class, adapters = SearchResults.class, resourceType = MySearchResultsImpl.RESOURCE_TYPE)
public class MySearchResultsImpl implements SearchResults , Component {
    protected static final String RESOURCE_TYPE = "venia/components/commerce/searchresults";
    @Self
    @Via(type = ResourceSuperType.class)
    SearchResults searchResults;

    public MySearchResultsImpl() {
        System.out.println("TEST");
    }

    @PostConstruct
    public void initModel() {
        // remove sort key with the name "position"
        List<SorterKey> keys = searchResults.getSearchResultsSet().getSorter().getKeys();
        if (keys != null) {
            keys.removeIf(sorterKey -> sorterKey.getName().equals("position"));
        }
    }

    @Override
    public SearchStorefrontContext getSearchStorefrontContext() {
        return searchResults.getSearchStorefrontContext();
    }

    @Override
    public SearchResultsStorefrontContext getSearchResultsStorefrontContext() {
        return searchResults.getSearchResultsStorefrontContext();
    }

    @Override
    public void extendProductQueryWith(Consumer<ProductInterfaceQuery> consumer) {
        searchResults.extendProductQueryWith(consumer);
    }

    @Override
    public void extendProductFilterWith(Function<ProductAttributeFilterInput, ProductAttributeFilterInput> function) {
        searchResults.extendProductFilterWith(function);
    }

    @Override
    public Collection<ProductListItem> getProducts() {
        return searchResults.getProducts();
    }

    @Override
    public SearchResultsSet getSearchResultsSet() {
        return searchResults.getSearchResultsSet();
    }

    @Override
    public boolean loadClientPrice() {
        return searchResults.loadClientPrice();
    }

    @Override
    public String getPaginationType() {
        return searchResults.getPaginationType();
    }

    @Override
    public boolean isAddToCartEnabled() {
        return searchResults.isAddToCartEnabled();
    }

    @Override
    public boolean isAddToWishListEnabled() {
        return searchResults.isAddToWishListEnabled();
    }

    @Override
    public String getId() {
        return ((Component)searchResults).getId();
    }

    @Override
    public ComponentData getData() {
        final ComponentData data = ((Component) searchResults).getData();
        if (data == null) {
            return null;
        }

        final AtomicReference<ComponentData> dataRef = new AtomicReference<>();
        ComponentData componentData = (ComponentData) Proxy.newProxyInstance(this.getClass().getClassLoader(),
                new Class[]{ComponentData.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getJson")) {
                        return String.format("{\"%s\":%s}", getId(), new ObjectMapper().writeValueAsString(dataRef.get()));
                    } else if (method.getName().equals("getType")) {
                        return getExportedType();
                    }
                    return method.invoke(data, args);
                });
        dataRef.set(componentData);
        return componentData;
    }

    @Override
    public String getAppliedCssClasses() {
        return ((Component)searchResults).getAppliedCssClasses();
    }

    @Override
    public String getExportedType() {
        return "venia/components/commerce/searchresults";
    }
}
