package com.venia.core.models.commerce;

import com.adobe.cq.commerce.core.components.models.common.Price;
import com.adobe.cq.commerce.core.components.models.product.Asset;
import com.adobe.cq.commerce.core.components.models.product.GroupItem;
import com.adobe.cq.commerce.core.components.models.product.Product;
import com.adobe.cq.commerce.core.components.models.product.Variant;
import com.adobe.cq.commerce.core.components.models.product.VariantAttribute;
import com.adobe.cq.commerce.core.components.models.retriever.AbstractProductRetriever;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.via.ResourceSuperType;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.Via;

import javax.annotation.PostConstruct;
import java.util.List;

@Model(adaptables = SlingHttpServletRequest.class, adapters = MyProduct.class, resourceType = MyProductImpl.RESOURCE_TYPE)
public class MyProductImpl implements MyProduct {
    protected static final String RESOURCE_TYPE = "venia/components/commerce/myproduct";

    @Self
    @Via(type = ResourceSuperType.class)
    private Product product;

    private AbstractProductRetriever productRetriever;

    @PostConstruct
    public void initModel() {
        productRetriever = getProductRetriever();

        if (productRetriever != null) {
            productRetriever.extendProductQueryWith(p -> p.countryOfManufacture());
        }
    }

    @Override public String getCountryOfManufacture() {
        return productRetriever.fetchProduct().getCountryOfManufacture();
    }

    // Sling Model Delegation Methods

    @Override public Boolean getFound() {
        return product.getFound();
    }

    @Override public String getName() {
        return product.getName();
    }

    @Override public String getDescription() {
        return product.getDescription();
    }

    @Override public String getSku() {
        return product.getSku();
    }

    @Override public String getCurrency() {
        return product.getCurrency();
    }

    @Override public Double getPrice() {
        return product.getPrice();
    }

    @Override public Price getPriceRange() {
        return product.getPriceRange();
    }

    @Override public String getFormattedPrice() {
        return product.getFormattedPrice();
    }

    @Override public Boolean getInStock() {
        return product.getInStock();
    }

    @Override public Boolean isConfigurable() {
        return product.isConfigurable();
    }

    @Override public Boolean isGroupedProduct() {
        return product.isGroupedProduct();
    }

    @Override public Boolean isVirtualProduct() {
        return product.isVirtualProduct();
    }

    @Override public Boolean isBundleProduct() {
        return product.isBundleProduct();
    }

    @Override public String getVariantsJson() {
        return product.getVariantsJson();
    }

    @Override public List<Variant> getVariants() {
        return product.getVariants();
    }

    @Override public List<GroupItem> getGroupedProductItems() {
        return product.getGroupedProductItems();
    }

    @Override public List<Asset> getAssets() {
        return product.getAssets();
    }

    @Override public String getAssetsJson() {
        return product.getAssetsJson();
    }

    @Override public List<VariantAttribute> getVariantAttributes() {
        return product.getVariantAttributes();
    }

    @Override public Boolean loadClientPrice() {
        return product.loadClientPrice();
    }

    @Override public AbstractProductRetriever getProductRetriever() {
        return product.getProductRetriever();
    }

    @Override public String getMetaDescription() {
        return product.getMetaDescription();
    }

    @Override public String getMetaKeywords() {
        return product.getMetaKeywords();
    }

    @Override public String getMetaTitle() {
        return product.getMetaTitle();
    }

    @Override public String getCanonicalUrl() {
        return product.getCanonicalUrl();
    }
}
