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

import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.productlist.CategoryRetriever;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.PageManagerFactory;
import com.venia.core.models.commerce.services.CommerceComponentModelFinder;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.framework.BundleContext;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith({AemContextExtension.class, MockitoExtension.class})
public class CatalogPageErrorFilterTest {

    private final AemContext aemContext = new AemContext();

    @Mock
    private PageManagerFactory pageManagerFactory;

    @Mock
    private PageManager pageManager;

    @Mock
    private Page currentPage;

    @Mock
    private Resource resource;

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
        when(pageManagerFactory.getPageManager(any())).thenReturn(pageManager);
        when(pageManager.getContainingPage(slingRequest.getResource())).thenReturn(currentPage);
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
        when(productRetriever.hasErrors()).thenReturn(true);

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
        PageManager pageManager = mock(PageManager.class);
        Page currentPage = mock(Page.class);
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
        when(searchResultsSet.hasErrors()).thenReturn(false);
        catalogPageErrorFilter.doFilter(slingRequest, slingResponse, filterChain);
        verify(filterChain).doFilter(slingRequest, slingResponse);
    }

    @Test
    void doFilterWithCategoryPageHasErrors() throws IOException, ServletException {
        PageManager pageManager = mock(PageManager.class);
        Page currentPage = mock(Page.class);
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
        when(categoryRetriever.hasErrors()).thenReturn(true);

        catalogPageErrorFilter.doFilter(slingRequest, slingResponse, filterChain);

        verify(slingResponse).sendError(
                HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                "Commerce application not reachable"
        );
        verify(filterChain, never()).doFilter(slingRequest, slingResponse);
    }

}
