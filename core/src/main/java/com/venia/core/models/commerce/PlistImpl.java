package com.venia.core.models.commerce;

import java.util.Collection;

import javax.annotation.PostConstruct;

import com.adobe.cq.commerce.core.components.models.common.ProductListItem;
import com.adobe.cq.commerce.core.components.models.productlist.ProductList;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractCategoryRetriever;
import com.adobe.cq.commerce.core.components.storefrontcontext.CategoryStorefrontContext;
import com.adobe.cq.commerce.core.search.models.SearchResultsSet;
import com.shopify.graphql.support.SchemaViolationError;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Via;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.via.ResourceSuperType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Model(adaptables = SlingHttpServletRequest.class, adapters = { ProductList.class,
        Plist.class }, resourceType = PlistImpl.RESOURCE_TYPE)
public class PlistImpl implements Plist {

    /**
     *
     */
    private static final String ATTRIBUTE = "fashion_material";

    protected static final String RESOURCE_TYPE = "venia/components/commerce/productlist";

    @Self
    @Via(type = ResourceSuperType.class)
    private ProductList productList;

    private static final Logger LOGGER = LoggerFactory.getLogger(PlistImpl.class);

    

    AbstractCategoryRetriever categoryRetriever;

    @PostConstruct
    public void initModel() {
        categoryRetriever = productList.getCategoryRetriever();
        categoryRetriever.extendProductQueryWith(p -> p.addCustomSimpleField(ATTRIBUTE));

        // LOGGER.info("************" +
        // categoryRetriever.fetchCategory().getProducts());
        Collection<ProductListItem> productListItems = productList.getProducts();
        for (ProductListItem productListItem : productListItems) {
            LOGGER.debug("Product {}({})", productListItem.getName(), productListItem.getSKU());
            if (productListItem.getProduct().get(ATTRIBUTE) != null) {
                LOGGER.debug("Custom {})", productListItem.getProduct().get(ATTRIBUTE).toString());
            }
        }

    }

    @Override
    public boolean showTitle() {
        // TODO Auto-generated method stub
        return productList.showTitle();
    }

    @Override
    public String getTitle() {
        // TODO Auto-generated method stub
        return productList.getTitle();
    }

    @Override
    public String getImage() {
        // TODO Auto-generated method stub
        return productList.getImage();
    }

    @Override
    public boolean showImage() {
        // TODO Auto-generated method stub
        return productList.showImage();
    }

    @Override
    public AbstractCategoryRetriever getCategoryRetriever() {
        // TODO Auto-generated method stub
        return categoryRetriever;
    }

    @Override
    public Collection<ProductListItem> getProducts() {
        // TODO Auto-generated method stub
        return productList.getProducts();
    }

    @Override
    public SearchResultsSet getSearchResultsSet() {
        // TODO Auto-generated method stub
        return productList.getSearchResultsSet();
    }

    @Override
    public boolean loadClientPrice() {
        // TODO Auto-generated method stub
        return productList.loadClientPrice();
    }

    @Override
    public String getPaginationType() {
        // TODO Auto-generated method stub
        return productList.getPaginationType();
    }

    @Override
    public String getMetaDescription() {
        // TODO Auto-generated method stub
        return productList.getMetaDescription();
    }

    @Override
    public String getMetaKeywords() {
        // TODO Auto-generated method stub
        return productList.getMetaKeywords();
    }

    @Override
    public String getMetaTitle() {
        // TODO Auto-generated method stub
        return productList.getMetaTitle();
    }

    @Override
    public String getCanonicalUrl() {
        // TODO Auto-generated method stub
        return productList.getCanonicalUrl();
    }

    @Override
    public CategoryStorefrontContext getStorefrontContext() {
        // TODO Auto-generated method stub
        return productList.getStorefrontContext();
    }

}
