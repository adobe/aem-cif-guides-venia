package com.venia.core.models.commerce.servlets;

import com.adobe.cq.commerce.core.components.models.common.SiteStructure;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractCategoryRetriever;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.PageManagerFactory;
import com.venia.core.models.commerce.services.CommerceComponentModelFinder;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.scripting.core.ScriptHelper;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
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

    private BundleContext bundleContext;

    @Activate
    protected void activate(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (servletRequest instanceof SlingHttpServletRequest && servletResponse instanceof SlingHttpServletResponse) {
            SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) servletRequest;
            SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) servletResponse;
            PageManager pageManager = pageManagerFactory.getPageManager(slingRequest.getResourceResolver());
            Page currentPage = pageManager.getContainingPage(slingRequest.getResource());
            boolean removeSlingScriptHelperFromBindings = false;
            if (currentPage != null) {
                // Get the SiteStructure model
                SiteStructure siteStructure = slingRequest.adaptTo(SiteStructure.class);
                if (siteStructure.isProductPage(currentPage)) {
                    // add the SlingScriptHelper to the bindings if it is not there yet
                    removeSlingScriptHelperFromBindings = addSlingScriptHelperIfNeeded(slingRequest, slingResponse);
                    Product product = commerceModelFinder.findProductComponentModel(slingRequest, currentPage.getContentResource());
                    if (product != null) {
                        AbstractProductRetriever productRetriever = product.getProductRetriever();
                        if (productRetriever.hasErrors()) {
                            slingResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Commerce application not reachable");
                            return;
                        }
                    }
                } else if (siteStructure.isCategoryPage(currentPage)) {
                    // add the SlingScriptHelper to the bindings if it is not there yet
                    removeSlingScriptHelperFromBindings = addSlingScriptHelperIfNeeded(slingRequest, slingResponse);
                    ProductList productList = commerceModelFinder.findProductListComponentModel(slingRequest, currentPage.getContentResource());
                    if (productList != null) {
                        // Get the AbstractCategoryRetriever model
                        AbstractCategoryRetriever categoryRetriever = productList.getCategoryRetriever();
                        if (categoryRetriever.hasErrors()) {
                            slingResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Commerce application not reachable");
                            return;
                        }
                    }
                }
                if (removeSlingScriptHelperFromBindings) {
                    // remove the ScriptHelper if we added it before
                    SlingBindings slingBindings = getSlingBindings(slingRequest);
                    if (slingBindings != null) {
                        slingBindings.remove("sling");
                    }
                }
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    public void destroy() {
    }

    /**
     * The {@link com.venia.core.models.commerce.services.CommerceComponentModelFinder} uses
     * {@link org.apache.sling.models.factory.ModelFactory#getModelFromWrappedRequest(SlingHttpServletRequest, Resource, Class)}
     * to obtain the model of either {@link Product} or {@link ProductList}. That method invokes all
     * {@link org.apache.sling.scripting.api.BindingsValuesProvider}
     * while creating the wrapped request. In AEM 6.5 they are not executed lazily and depend on some existing bindings on construction of
     * which one requires the SlingScriptHelper.
     *
     * @param slingRequest
     */
    private boolean addSlingScriptHelperIfNeeded(SlingHttpServletRequest slingRequest, SlingHttpServletResponse slingResponse) {
        SlingBindings slingBindings = getSlingBindings(slingRequest);
        if (slingBindings != null && slingBindings.getSling() == null) {
            slingBindings.put("sling", new ScriptHelper(bundleContext, null, slingRequest, slingResponse));
            return true;
        }
        return false;
    }

    private static SlingBindings getSlingBindings(SlingHttpServletRequest slingRequest) {
        Object attr = slingRequest.getAttribute(SlingBindings.class.getName());
        if (attr == null) {
            attr = new SlingBindings();
            slingRequest.setAttribute(SlingBindings.class.getName(), attr);
        }
        return attr instanceof SlingBindings ? (SlingBindings) attr : null;
    }
}
