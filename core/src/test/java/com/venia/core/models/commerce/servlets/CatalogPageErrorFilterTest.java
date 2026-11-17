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
package com.venia.core.models.commerce.servlets;

import com.adobe.cq.commerce.core.components.client.MagentoGraphqlClient;
import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.productlist.CategoryRetriever;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.PageManagerFactory;
import com.day.cq.wcm.api.WCMMode;
import com.venia.core.models.commerce.services.CommerceComponentModelFinder;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.BundleContext;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CatalogPageErrorFilterTest {

    @Mock
    private PageManagerFactory pageManagerFactory;

    @Mock
    private PageManager pageManager;

    @Mock
    private Page currentPage;

    @Mock
    private CommerceComponentModelFinder commerceModelFinder;

    @Mock
    private BundleContext bundleContext;

    @Mock
    private MockSlingHttpServletRequest slingRequest;

    @Mock
    private MockSlingHttpServletResponse slingResponse;

    @InjectMocks
    private CatalogPageErrorFilter catalogPageErrorFilter;

    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filterChain = mock(FilterChain.class);
        lenient().when(slingRequest.getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(WCMMode.DISABLED);
        lenient().when(pageManagerFactory.getPageManager(any())).thenReturn(pageManager);
        lenient().when(pageManager.getContainingPage(slingRequest.getResource())).thenReturn(currentPage);
    }

    @Test
    void doFilterWhenCurrentPageNull() throws IOException, ServletException {
        when(pageManager.getContainingPage(slingRequest.getResource())).thenReturn(null);

        catalogPageErrorFilter.doFilter(slingRequest, slingResponse, filterChain);
        verify(filterChain).doFilter(slingRequest, slingResponse);
    }

    @Test
    void doFilterWhenProductNull() throws IOException, ServletException {
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        SiteStructure siteStructure = mock(SiteStructure.class);

        when(slingRequest.getResourceResolver()).thenReturn(resourceResolver);
        when(slingRequest.adaptTo(SiteStructure.class)).thenReturn(siteStructure);
        when(siteStructure.isProductPage(any())).thenReturn(true);
        when(commerceModelFinder.findProductComponentModel(any(), any())).thenReturn(null);

        catalogPageErrorFilter.doFilter(slingRequest, slingResponse, filterChain);

        verify(filterChain).doFilter(slingRequest, slingResponse);
    }

    @Test
    void doFilterWhenProductRetrieverNull() throws IOException, ServletException {
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        SiteStructure siteStructure = mock(SiteStructure.class);
        Product product = mock(Product.class);

        when(slingRequest.getResourceResolver()).thenReturn(resourceResolver);
        when(slingRequest.adaptTo(SiteStructure.class)).thenReturn(siteStructure);
        when(siteStructure.isProductPage(any())).thenReturn(true);
        when(commerceModelFinder.findProductComponentModel(any(), any())).thenReturn(product);
        when(product.getProductRetriever()).thenReturn(null);
        catalogPageErrorFilter.doFilter(slingRequest, slingResponse, filterChain);
        verify(filterChain).doFilter(slingRequest, slingResponse);
    }

    @Test
    void doFilterWhenProductRetrieverHasErrors() throws IOException, ServletException {
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        SiteStructure siteStructure = mock(SiteStructure.class);
        Product product = mock(Product.class);
        AbstractProductRetriever productRetriever = mock(AbstractProductRetriever.class);

        when(slingRequest.getResourceResolver()).thenReturn(resourceResolver);
        when(slingRequest.adaptTo(SiteStructure.class)).thenReturn(siteStructure);
        when(siteStructure.isProductPage(any())).thenReturn(true);
        when(commerceModelFinder.findProductComponentModel(any(), any())).thenReturn(product);
        when(product.getProductRetriever()).thenReturn(productRetriever);
        when(productRetriever.getErrors()).thenReturn(Collections.singletonList(error(MagentoGraphqlClient.RUNTIME_ERROR_CATEGORY)));

        catalogPageErrorFilter.doFilter(slingRequest, slingResponse, filterChain);
        verify(slingResponse).sendError(
                HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                "Commerce application not reachable"
        );
        verify(filterChain, never()).doFilter(slingRequest, slingResponse);
    }

    @Test
    void doFilterWithCategoryPageWhenProductListNull() throws IOException, ServletException {
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        SiteStructure siteStructure = mock(SiteStructure.class);

        when(slingRequest.getResourceResolver()).thenReturn(resourceResolver);
        when(slingRequest.adaptTo(SiteStructure.class)).thenReturn(siteStructure);
        when(siteStructure.isCategoryPage(any())).thenReturn(true);
        when(commerceModelFinder.findProductListComponentModel(any(), any())).thenReturn(null);
        catalogPageErrorFilter.doFilter(slingRequest, slingResponse, filterChain);

        verify(filterChain).doFilter(slingRequest, slingResponse);
    }

    @Test
    void doFilterWithCategoryPageWhenCategoryRetrieverNullAndSearchResultErrorEmpty() throws IOException, ServletException {
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        SiteStructure siteStructure = mock(SiteStructure.class);
        ProductList productList = mock(ProductList.class);
        SearchResultsSet searchResultsSet = mock(SearchResultsSet.class);

        when(slingRequest.getResourceResolver()).thenReturn(resourceResolver);
        when(slingRequest.adaptTo(SiteStructure.class)).thenReturn(siteStructure);
        when(siteStructure.isCategoryPage(any())).thenReturn(true);
        when(commerceModelFinder.findProductListComponentModel(any(), any())).thenReturn(productList);
        when(productList.getCategoryRetriever()).thenReturn(null);
        when(productList.getSearchResultsSet()).thenReturn(searchResultsSet);
        when(searchResultsSet.getErrors()).thenReturn(Collections.emptyList());
        catalogPageErrorFilter.doFilter(slingRequest, slingResponse, filterChain);
        verify(filterChain).doFilter(slingRequest, slingResponse);
    }

    @Test
    void doFilterWithCategoryPageHasErrors() throws IOException, ServletException {
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        SiteStructure siteStructure = mock(SiteStructure.class);
        ProductList productList = mock(ProductList.class);
        CategoryRetriever categoryRetriever = mock(CategoryRetriever.class);

        when(slingRequest.getResourceResolver()).thenReturn(resourceResolver);
        when(slingRequest.adaptTo(SiteStructure.class)).thenReturn(siteStructure);
        when(siteStructure.isCategoryPage(any())).thenReturn(true);
        when(commerceModelFinder.findProductListComponentModel(any(), any())).thenReturn(productList);
        when(productList.getCategoryRetriever()).thenReturn(categoryRetriever);
        when(categoryRetriever.fetchCategory()).thenReturn(null);
        when(categoryRetriever.getErrors()).thenReturn(Collections.singletonList(error(MagentoGraphqlClient.RUNTIME_ERROR_CATEGORY)));

        catalogPageErrorFilter.doFilter(slingRequest, slingResponse, filterChain);

        verify(slingResponse).sendError(
                HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                "Commerce application not reachable"
        );
        verify(filterChain, never()).doFilter(slingRequest, slingResponse);
    }

    static Stream<Arguments> errorCases() {
        return Stream.of(
                Arguments.of(null, false),
                Arguments.of(Collections.emptyList(), false),
                Arguments.of(Collections.singletonList(null), false),
                Arguments.of(Collections.singletonList(error(null)), false),
                Arguments.of(Collections.singletonList(error("graphql-input")), false),
                Arguments.of(Collections.singletonList(error(MagentoGraphqlClient.RUNTIME_ERROR_CATEGORY + "-other")), false),
                Arguments.of(Collections.singletonList(error(MagentoGraphqlClient.RUNTIME_ERROR_CATEGORY)), true),
                Arguments.of(Arrays.asList(null, error("graphql-input"), error(MagentoGraphqlClient.RUNTIME_ERROR_CATEGORY)), true));
    }

    private static Error error(String category) {
        Error error = new Error();
        error.setCategory(category);
        return error;
    }

    private void catalogPage(boolean productPage) {
        SiteStructure siteStructure = mock(SiteStructure.class);
        when(slingRequest.adaptTo(SiteStructure.class)).thenReturn(siteStructure);
        if (productPage) {
            when(siteStructure.isProductPage(currentPage)).thenReturn(true);
        } else {
            when(siteStructure.isCategoryPage(currentPage)).thenReturn(true);
        }
    }

    private void assertResponse(boolean unavailable) throws IOException, ServletException {
        catalogPageErrorFilter.doFilter(slingRequest, slingResponse, filterChain);
        if (unavailable) {
            verify(slingResponse).sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Commerce application not reachable");
            verifyNoInteractions(filterChain);
        } else {
            verify(filterChain).doFilter(slingRequest, slingResponse);
            verifyNoInteractions(slingResponse);
        }
    }

    @ParameterizedTest
    @MethodSource("errorCases")
    void productErrorCategories(List<Error> errors, boolean unavailable) throws IOException, ServletException {
        catalogPage(true);
        Product product = mock(Product.class);
        AbstractProductRetriever retriever = mock(AbstractProductRetriever.class);
        when(commerceModelFinder.findProductComponentModel(any(), any())).thenReturn(product);
        when(product.getProductRetriever()).thenReturn(retriever);
        when(retriever.getErrors()).thenReturn(errors);

        assertResponse(unavailable);

        InOrder order = inOrder(retriever);
        order.verify(retriever).fetchProduct();
        order.verify(retriever).getErrors();
    }

    @ParameterizedTest
    @MethodSource("errorCases")
    void categoryErrorCategories(List<Error> errors, boolean unavailable) throws IOException, ServletException {
        catalogPage(false);
        ProductList productList = mock(ProductList.class);
        CategoryRetriever retriever = mock(CategoryRetriever.class);
        when(commerceModelFinder.findProductListComponentModel(any(), any())).thenReturn(productList);
        when(productList.getCategoryRetriever()).thenReturn(retriever);
        when(retriever.getErrors()).thenReturn(errors);

        assertResponse(unavailable);

        InOrder order = inOrder(retriever);
        order.verify(retriever).fetchCategory();
        order.verify(retriever).getErrors();
    }

    @ParameterizedTest
    @MethodSource("errorCases")
    void searchErrorCategories(List<Error> errors, boolean unavailable) throws IOException, ServletException {
        catalogPage(false);
        ProductList productList = mock(ProductList.class);
        CategoryRetriever retriever = mock(CategoryRetriever.class);
        SearchResultsSet results = mock(SearchResultsSet.class);
        when(commerceModelFinder.findProductListComponentModel(any(), any())).thenReturn(productList);
        when(productList.getCategoryRetriever()).thenReturn(retriever);
        when(retriever.fetchCategory()).thenReturn(mock(CategoryTree.class));
        when(productList.getSearchResultsSet()).thenReturn(results);
        when(results.getErrors()).thenReturn(errors);

        assertResponse(unavailable);

        InOrder order = inOrder(productList, results);
        order.verify(productList).getSearchResultsSet();
        order.verify(results).getErrors();
        verify(retriever, never()).getErrors();
    }

    @ParameterizedTest
    @ValueSource(strings = {"product", "category", "search"})
    void uninitializedErrorsPreserveUnderlyingHandling(String surface) throws IOException, ServletException {
        catalogPage("product".equals(surface));
        if ("product".equals(surface)) {
            Product product = mock(Product.class);
            AbstractProductRetriever retriever = mock(AbstractProductRetriever.class);
            when(commerceModelFinder.findProductComponentModel(any(), any())).thenReturn(product);
            when(product.getProductRetriever()).thenReturn(retriever);
            when(retriever.getErrors()).thenThrow(new IllegalStateException("Not populated"));
        } else {
            ProductList productList = mock(ProductList.class);
            when(commerceModelFinder.findProductListComponentModel(any(), any())).thenReturn(productList);
            if ("category".equals(surface)) {
                CategoryRetriever retriever = mock(CategoryRetriever.class);
                when(productList.getCategoryRetriever()).thenReturn(retriever);
                when(retriever.getErrors()).thenThrow(new IllegalStateException("Not populated"));
            } else {
                SearchResultsSet results = mock(SearchResultsSet.class);
                when(productList.getSearchResultsSet()).thenReturn(results);
                when(results.getErrors()).thenThrow(new IllegalStateException("Not populated"));
            }
        }

        assertResponse(false);
    }

    @ParameterizedTest
    @EnumSource(value = WCMMode.class, names = {"DISABLED"}, mode = EnumSource.Mode.EXCLUDE)
    void authorModesBypassCommerceHandling(WCMMode mode) throws IOException, ServletException {
        when(slingRequest.getAttribute(WCMMode.REQUEST_ATTRIBUTE_NAME)).thenReturn(mode);

        assertResponse(false);

        verifyNoInteractions(pageManagerFactory, commerceModelFinder);
    }

    @Test
    void nullCategoryRetrieverAndSearchResultsPreserveUnderlyingHandling() throws IOException, ServletException {
        catalogPage(false);
        when(commerceModelFinder.findProductListComponentModel(any(), any())).thenReturn(mock(ProductList.class));

        assertResponse(false);
    }

    @Test
    void foundProductPreservesUnderlyingHandling() throws IOException, ServletException {
        catalogPage(true);
        Product product = mock(Product.class);
        AbstractProductRetriever retriever = mock(AbstractProductRetriever.class);
        when(commerceModelFinder.findProductComponentModel(any(), any())).thenReturn(product);
        when(product.getProductRetriever()).thenReturn(retriever);
        when(product.getFound()).thenReturn(true);

        assertResponse(false);

        verify(retriever).fetchProduct();
        verify(retriever, never()).getErrors();
    }
}
