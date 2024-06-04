package com.venia.core.models.commerce.servlets;

import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractCategoryRetriever;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.PageManagerFactory;
import com.venia.core.models.commerce.services.CommerceComponentModelFinder;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component(
        service = {Filter.class},
        property = {
                "sling.filter.scope=REQUEST",
                "sling.filter.resourceTypes=cq:Page",
                "sling.filter.resourceTypes=core/cif/components/structure/page/v1/page",
                "sling.filter.resourceTypes=core/cif/components/structure/page/v2/page",
                "sling.filter.resourceTypes=core/cif/components/structure/page/v3/page",
                "sling.filter.extensions=html",
                "sling.filter.extensions=json",
                "sling.filter.resource.pattern=/content(/.+)?",
                "service.ranking:Integer=-4000"
        })
public class CatalogPageExceptionFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(com.venia.core.models.commerce.servlets.CatalogPageExceptionFilter.class);

    @Reference
    private PageManagerFactory pageManagerFactory;

    @Reference
    private CommerceComponentModelFinder commerceModelFinder;

    public CatalogPageExceptionFilter() {
    }

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (servletRequest instanceof SlingHttpServletRequest && servletResponse instanceof SlingHttpServletResponse) {
            SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) servletRequest;
            SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) servletResponse;
            PageManager pageManager = pageManagerFactory.getPageManager(slingRequest.getResourceResolver());
            Page currentPage = pageManager.getContainingPage(slingRequest.getResource());
            if (currentPage != null) {
                SiteStructure siteStructure = slingRequest.adaptTo(SiteStructure.class);
                if (siteStructure.isProductPage(currentPage) ) {
                    Product product = commerceModelFinder.findProductComponentModel(slingRequest, currentPage.getContentResource());
                    if (product != null && product.getFound() && product.getProductRetriever().hasErrors()) {
                        slingResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Commerce application not reachable");
                        return;
                    }
                } else if (siteStructure.isCategoryPage(currentPage)) {
                    ProductList productList = commerceModelFinder.findProductListComponentModel(slingRequest, currentPage.getContentResource());
                    if (productList != null) {
                        AbstractCategoryRetriever categoryRetriever = productList.getCategoryRetriever();
                        if (categoryRetriever == null && categoryRetriever.hasErrors()) {
                            slingResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Commerce application not reachable");
                            return;
                        }
                    }
                }
                // Need to add condition for search page
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    public void destroy() {
    }

}
